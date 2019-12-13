/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import chirper.shared.Util;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import org.apache.commons.math3.analysis.function.Add;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class Peer implements AutoCloseable
{
    // the identifier of this peer
    private final PeerId localPeerId;

    // the identifiers of all remote peers, keyed by their addresses
    private final Map< Address, PeerId > remotePeerIds;

    // the messaging service
    private final ManagedMessagingService messaging;

    // the message encoder and decoder
    private final Serializer serializer;

    // the timestamp of the next chirp published by this peer
    private long clock;

    // all chirps being published by this peer, keyed by their timestamps
    private final Map< Long, PendingChirp > pendingChirps;

    // all published chirps in reverse global total order
    private final SortedSet< PublishedChirp > publishedChirps;

    private final State state;

    /**
     * TODO: document
     *
     * @param config TODO: document
     */
    public Peer(PeerConfig config)
    {
        this(
            config.getLocalPeerId(),
            config.getLocalPeerPort(),
            config.getRemotePeerIds()
        );
    }

    /**
     * TODO: document
     *
     * @param localPeerId the identifier of this peer
     * @param localPeerPort the port to be used by this peer
     * @param remotePeerIds all remote peer identifiers keyed by their addresses
     */
    public Peer(
        PeerId localPeerId,
        int localPeerPort,
        Map< Address, PeerId > remotePeerIds
    )
    {
        this.localPeerId = Objects.requireNonNull(localPeerId);
        this.remotePeerIds = new HashMap<>(remotePeerIds);

        this.messaging = new NettyMessagingService(
            Config.NETTY_CLUSTER_NAME,
            Address.from(localPeerPort),
            new MessagingConfig()
        );

        this.serializer =
            Serializer
            .builder()
            .withTypes(MsgChirp.class, MsgAck.class)
            .build();

        this.clock = Long.MIN_VALUE;

        this.pendingChirps = new HashMap<>();

        this.publishedChirps = new TreeSet<>(
            Comparator
            .comparing(PublishedChirp::getPeerId)
            .thenComparingLong(PublishedChirp::getTimestamp)
            .reversed()
        );

        // register message handlers

        final var e = Executors.newFixedThreadPool(1);

        this.messaging.registerHandler("get", this::handleClientGet, e);
        this.messaging.registerHandler("publish", this::handleClientPublish, e);

        this.messaging.registerHandler("chirp", this::handlePeerChirp, e);
        this.messaging.registerHandler("ack", this::handlePeerAck, e);
    }

    /**
     * TODO: document
     */
    public void start()
    {
        this.messaging.start().join();
    }

    /**
     * TODO: document
     */
    @Override
    public void close()
    {
        this.messaging.stop().join();
    }

    /**
     * Gets the most recent chirps with the given topics.
     *
     * Only up to {@param maxChirps} are returned in the list.
     *
     * The list is unmodifiable.
     *
     * Thread-safety: Safe to call at any time from any context.
     *
     * @param topics
     * @param maxChirps
     * @return
     */
    public List< PublishedChirp > getChirps(
        Collection< ? extends CharSequence > topics,
        long maxChirps
    )
    {
        // get matching chirps in reverse order

        final List< PublishedChirp > chirps;

        synchronized (this.publishedChirps)
        {
            chirps =
                this
                .publishedChirps
                .stream()
                .filter(c -> !Collections.disjoint(
                    Util.getChirpTopics(c.getText()),
                    topics
                ))
                .limit(maxChirps)
                .collect(Collectors.toList());
        }

        // reverse chirps back to original order

        Collections.reverse(chirps);

        // return chirps

        return Collections.unmodifiableList(chirps);
    }

    /**
     * Publishes the given chirp, which should have been received from a client.
     *
     * The returned future is completed when all peers have acknowledged the
     * chirp. This implies that, by the time the future is completed, all chirps
     * ordered before the chirp in question were received.
     *
     * @param chirp the chirp to be published
     * @return a future that is completed when all peers acknowledge the chirp
     */
    public CompletableFuture< Void > publishChirp(String chirp)
    {
        var future = new CompletableFuture< Void >();

        // create pending chirp

        final var timestamp = this.clock++;

        this.pendingChirps.put(
            timestamp, new PendingChirp(this.remotePeerIds.values(), future)
        );

        // send chirp to peers

        final var payload = this.serializer.encode(
            new MsgChirp(timestamp, chirp)
        );

        for (final var peerAddress : this.remotePeerIds.keySet())
        {
            future = CompletableFuture.allOf(
                future,
                this.messaging.sendAsync(peerAddress, "chirp", payload)
            );
        }

        return future.thenRun(() -> {
            this.pendingChirps.remove(timestamp);

            this.publishedChirps.add(
                new PublishedChirp(this.localPeerId, timestamp, chirp)
            );
        });
    }

    private CompletableFuture< byte[] > handleClientGet(
        Address from,
        byte[] payload
    )
    {
        final String[] topics = this.serializer.decode(payload);

        // TODO: validate topics

        final var chirps = this.state.getLatestChirps(topics);

        this.serializer.encode(chirps.t);
    }
    }

    private CompletableFuture< byte[] > handleClientPublish(
        Address from,
        byte[] payload
    )
    {

    }

    private void handlePeerChirp(Address from, byte[] payload)
    {
        final var fromId = this.remotePeerIds.get(from);
        final var msg = this.serializer.< MsgChirp >decode(payload);

        this.clock = Math.max(this.clock, msg.timestamp) + 1;

        this.publishedChirps.add(new PublishedChirp(
            fromId, msg.timestamp, msg.text
        ));
    }

    private void handlePeerAck(Address from, byte[] payload)
    {
        final var fromId = this.remotePeerIds.get(from);
        final var msg = this.serializer.< MsgAck >decode(payload);

        this.pendingChirps.get(msg.chirpTimestamp).ackPeer(fromId);
    }
}

/* -------------------------------------------------------------------------- */

class MsgChirp
{
    public final long timestamp;
    public final String text;

    public MsgChirp(long timestamp, String text)
    {
        this.timestamp = timestamp;
        this.text = text;
    }
}

class MsgAck
{
    public final long chirpTimestamp;

    public MsgAck(long chirpTimestamp)
    {
        this.chirpTimestamp = chirpTimestamp;
    }
}

class PendingChirp
{
    private final Set< PeerId > unackedPeerIds;
    private final CompletableFuture< Void > onAllAcked;

    public PendingChirp(
        Collection< PeerId > peerIds,
        CompletableFuture< Void > onAllAcked
    )
    {
        this.unackedPeerIds = new HashSet<>(peerIds);
        this.onAllAcked = onAllAcked;
    }

    public void ackPeer(PeerId peerId)
    {
        this.unackedPeerIds.remove(peerId);

        if (this.unackedPeerIds.isEmpty())
            this.onAllAcked.complete(null);
    }
}

//class PublishedChirp
//{
//    private final PeerId peerId;
//    private final long timestamp;
//    private final String text;
//
//    public PublishedChirp(PeerId peerId, long timestamp, String text)
//    {
//        this.peerId = peerId;
//        this.timestamp = timestamp;
//        this.text = text;
//    }
//
//    public PeerId getPeerId()
//    {
//        return this.peerId;
//    }
//
//    public long getTimestamp()
//    {
//        return this.timestamp;
//    }
//
//    public String getText()
//    {
//        return this.text;
//    }
//}

/* -------------------------------------------------------------------------- */
