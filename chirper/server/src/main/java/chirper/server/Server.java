/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class Server implements AutoCloseable
{
    // the identifier of this server
    private final ServerId localServerId;

    // the identifiers of all remote servers, keyed by their addresses
    private final Map< Address, ServerId > remoteServerIds;

    // the messaging service
    private final ManagedMessagingService messaging;

    // the message encoder and decoder
    private final Serializer serializer;

    // the timestamp of the next chirp published by this server
    private long clock;

    // all chirps being published by this server, keyed by their timestamps
    private final Map< Long, PendingChirp > pendingChirps;

    // TODO: document
    private final State state;

    /**
     * TODO: document
     *
     * @param config TODO: document
     */
    public Server(ServerConfig config)
    {
        this(
            config.getLocalServerId(),
            config.getLocalServerPort(),
            config.getRemoteServerIds()
        );
    }

    /**
     * TODO: document
     *
     * @param localServerId the identifier of this server
     * @param localServerPort the port to be used by this server
     * @param remoteServerIds all remote server identifiers keyed by their
     *     addresses
     */
    public Server(
        ServerId localServerId,
        int localServerPort,
        Map< Address, ServerId > remoteServerIds
    )
    {
        this.localServerId = Objects.requireNonNull(localServerId);
        this.remoteServerIds = new HashMap<>(remoteServerIds);

        this.messaging = new NettyMessagingService(
            Config.NETTY_CLUSTER_NAME,
            Address.from(localServerPort),
            new MessagingConfig()
        );

        this.serializer =
            Serializer
                .builder()
                .withTypes(MsgChirp.class, MsgAck.class)
                .build();

        this.clock = Long.MIN_VALUE;

        this.pendingChirps = new HashMap<>();

        this.state = new State();

        // register message handlers

        final var e = Executors.newFixedThreadPool(1);

        this.messaging.registerHandler("get", this::handleClientGet, e);
        this.messaging.registerHandler("publish", this::handleClientPublish, e);

        this.messaging.registerHandler("chirp", this::handleServerChirp, e);
        this.messaging.registerHandler("ack", this::handleServerAck, e);
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

    private byte[] handleClientGet(Address from, byte[] payload)
    {
        final String[] topics = this.serializer.decode(payload);

        // TODO: validate topics

        final var chirps = this.state.getLatestChirps(topics);

        return this.serializer.encode(chirps.toArray(String[]::new));
    }

    private CompletableFuture< byte[] > handleClientPublish(
        Address from,
        byte[] payload
    )
    {
        final String chirp = this.serializer.decode(payload);

        // TODO: validate chirp

        return
            this.publishChirp(chirp)
                .thenApply(v -> null)
                .exceptionally(Throwable::getMessage)
                .thenApply(this.serializer::encode);
    }

    private void handleServerChirp(Address from, byte[] payload)
    {
        final var fromId = this.remoteServerIds.get(from);
        final var msg = this.serializer.< MsgChirp >decode(payload);

        // "synchronize" and tick clock

        this.clock = Math.max(this.clock, msg.timestamp) + 1;

        // add chirp to state

        this.state.addChirp(fromId, msg.timestamp, msg.text);

        // send acknowledgment

        this.messaging.sendAsync(
            from,
            "ack",
            this.serializer.encode(new MsgAck(msg.timestamp))
        );
    }

    private void handleServerAck(Address from, byte[] payload)
    {
        final var fromId = this.remoteServerIds.get(from);
        final var msg = this.serializer.< MsgAck >decode(payload);

        this.pendingChirps.get(msg.chirpTimestamp).ackServer(fromId);
    }

    /**
     * Publishes the given chirp, which should have been received from a
     * client.
     *
     * The returned future is completed when all servers have acknowledged the
     * chirp. This implies that, by the time the future is completed, all chirps
     * ordered before the chirp in question were received.
     *
     * @param chirp the chirp to be published
     *
     * @return a future that is completed when all servers acknowledge the chirp
     */
    private CompletableFuture< Void > publishChirp(String chirp)
    {
        var ackFuture = new CompletableFuture< Void >();

        // create pending chirp

        final var timestamp = this.clock++;

        this.pendingChirps.put(
            timestamp, new PendingChirp(this.remoteServerIds.values(), ackFuture)
        );

        // send chirp to servers

        final var payload = this.serializer.encode(
            new MsgChirp(timestamp, chirp)
        );

        final var sendFuture = CompletableFuture.allOf(
            this.remoteServerIds
                .keySet()
                .stream()
                .map(a -> this.messaging.sendAsync(a, "chirp", payload))
                .toArray(CompletableFuture[]::new)
        );

        // (when we sent all reqs and received all acks, ...)

        return sendFuture.thenAcceptBoth(ackFuture, (v1, v2) -> {
            this.pendingChirps.remove(timestamp);
            this.state.addChirp(this.localServerId, timestamp, chirp);
        });
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
    private final Set< ServerId > unackedServerIds;
    private final CompletableFuture< Void > onAllAcked;

    public PendingChirp(
        Collection< ServerId > serverIds,
        CompletableFuture< Void > onAllAcked
    )
    {
        this.unackedServerIds = new HashSet<>(serverIds);
        this.onAllAcked = onAllAcked;
    }

    public void ackServer(ServerId serverId)
    {
        this.unackedServerIds.remove(serverId);

        if (this.unackedServerIds.isEmpty())
            this.onAllAcked.complete(null);
    }
}

/* -------------------------------------------------------------------------- */
