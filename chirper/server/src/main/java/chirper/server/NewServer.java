/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Peer;
import chirper.shared.Util;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class NewServer
{
    private static class PendingChirp
    {
        private final long timestamp;
        private final String text;

        private final Set< Integer > unackedPeerIds;

        public PendingChirp(
            long timestamp,
            String text,
            Collection< Integer > peerIds
        )
        {
            this.timestamp = timestamp;
            this.text = text;
            this.unackedPeerIds = new HashSet<>(peerIds);
        }

        public long getTimestamp()
        {
            return this.timestamp;
        }

        public String getText()
        {
            return this.text;
        }

        public Set< Integer > getUnackedPeerIds()
        {
            return Collections.unmodifiableSet(this.unackedPeerIds);
        }

        public void ackPeer(int peerId)
        {
            this.unackedPeerIds.remove(peerId);
        }
    }

    private static class PublishedChirp
    {
        private final int peerId;
        private final long timestamp;
        private final String text;

        public PublishedChirp(int peerId, long timestamp, String text)
        {
            this.peerId = peerId;
            this.timestamp = timestamp;
            this.text = text;
        }

        public int getPeerId()
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

    private final int localPeerId;
    private final Map< Address, Peer > peers;

    private final MessagingService messaging;
    private final Serializer serializer;

    private long clock;
    private final List< PendingChirp > pendingChirps;
    private final SortedSet< PublishedChirp > publishedChirps;

    public NewServer(
        int localPeerId,
        int localPeerPort,
        Collection< Peer > peers
    )
    {
        this.localPeerId = localPeerId;

        this.peers =
            peers
            .stream()
            .collect(Collectors.toUnmodifiableMap(Peer::getAddress, p -> p));

        this.messaging = new NettyMessagingService(
            "chirper", Address.from(localPeerPort), new MessagingConfig()
        );

        this.serializer =
            Serializer
            .builder()
            .withTypes(MsgChirp.class, MsgAck.class)
            .build();

        this.clock = Long.MIN_VALUE;

        this.pendingChirps = new ArrayList<>();

        this.publishedChirps = new TreeSet<>(
            Comparator
            .comparing(PublishedChirp::getPeerId)
            .thenComparingLong(PublishedChirp::getTimestamp)
            .reversed()
        );

        final var executor = Executors.newFixedThreadPool(1);

        this.messaging.registerHandler("chirp", this::handleChirp, executor);
        this.messaging.registerHandler("ack", this::handleAck, executor);
    }

    /**
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
        final var payload = this.serializer.encode(
            new MsgChirp(this.clock++, chirp)
        );

        for (final var peer : this.peers)
        {
            this.messaging.sendAsync(peer, "msg", payload)
        }

        CompletableFuture.allOf()
    }

    private CompletableFuture< Void > send(Address to, byte[] payload)
    {
        return this.messaging.sendAsync(to, "msg", payload);
    }

    private void handleChirp(Address from, byte[] payload)
    {
        final var fromId = this.peers.indexOf(from);
        final var msg = this.serializer.< MsgChirp >decode(payload);

        this.clock = Math.max(this.clock, msg.timestamp) + 1;
    }

    private void handleAck(Address from, byte[] payload)
    {

    }
}

/* -------------------------------------------------------------------------- */
