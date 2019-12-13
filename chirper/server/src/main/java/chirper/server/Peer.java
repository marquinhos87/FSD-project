/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import chirper.shared.Util;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Peer
{
    // the identifier of this peer
    private final PeerId localPeerId;

    // the identifiers of all remote peers keyed by their addresses
    private final Map< Address, PeerId > peerIds;

    // the messaging service
    private final MessagingService messaging;

    // the message encoder and decoder
    private final Serializer serializer;

    // the timestamp of the next chirp published by this peer
    private long clock;

    // all chirps being published by this peer keyed by their timestamps
    private final Map< Long, PendingChirp > pendingChirps;

    // all published chirps in their global total order
    private final SortedSet< PublishedChirp > publishedChirps;

    /**
     *
     * @param localPeerId the identifier of this peer
     * @param localPeerPort the port to be used by this peer
     * @param peerIds all remote peer identifiers keyed by their addresses
     */
    public Peer(
        PeerId localPeerId,
        int localPeerPort,
        Map< Address, PeerId > peerIds
    )
    {
        this.localPeerId = localPeerId;

        this.peerIds = new HashMap<>(peerIds);

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
    }

    /**
     *
     */
    public void run() throws InterruptedException
    {
        final var executor = Executors.newFixedThreadPool(1);

        this.messaging.registerHandler("chirp", this::handleChirp, executor);
        this.messaging.registerHandler("ack", this::handleAck, executor);

        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
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
            timestamp, new PendingChirp(this.peerIds.values(), future)
        );

        // send chirp to peers

        final var payload = this.serializer.encode(
            new MsgChirp(timestamp, chirp)
        );

        for (final var peerAddress : this.peerIds.keySet())
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

    private void handleChirp(Address from, byte[] payload)
    {
        final var fromId = this.peerIds.get(from);
        final var msg = this.serializer.< MsgChirp >decode(payload);

        this.clock = Math.max(this.clock, msg.timestamp) + 1;

        this.publishedChirps.add(new PublishedChirp(
            fromId, msg.timestamp, msg.text
        ));
    }

    private void handleAck(Address from, byte[] payload)
    {
        final var fromId = this.peerIds.get(from);
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

class PublishedChirp
{
    private final PeerId peerId;
    private final long timestamp;
    private final String text;

    public PublishedChirp(PeerId peerId, long timestamp, String text)
    {
        this.peerId = peerId;
        this.timestamp = timestamp;
        this.text = text;
    }

    public PeerId getPeerId()
    {
        return this.peerId;
    }

    public long getTimestamp()
    {
        return this.timestamp;
    }

    public String getText()
    {
        return this.text;
    }
}

/* -------------------------------------------------------------------------- */
