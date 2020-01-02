/* -------------------------------------------------------------------------- */

package chirper.server.prev;

import chirper.server.MsgChirp;
import chirper.server.PendingChirp;
import chirper.server.ServerConfig;
import chirper.server.network.ServerId;
import chirper.server.replicators.MsgAck;
import chirper.server.replicators.MsgCommit;
import chirper.server.replicators.MsgRollback;
import chirper.server.replicators.Participant;
import chirper.shared.Config;
import chirper.server.replicators.CoherentOrderedReplicator;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.*;
import java.util.concurrent.*;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class Server implements AutoCloseable
{
    // the identifier of this server
    private final ServerId localServerId;

    // the addresses of all remote servers
    private final Set< Address > remoteServerAddresses;

    private final List< ServerIdAddress > remoteServerIdsAddresses;

    // the messaging service
    private final ManagedMessagingService messaging;

    // the message encoder and decoder
    private final Serializer serializer;

    // the timestamp of the next chirp published by this server
    private long clock;

    // all chirps being published by this server, keyed by their timestamps
    private final Map< Long, PendingChirp > pendingChirps;

    // Participant instance for a Two Phase Commit iteration
    private final Map< Long, Participant > participantRole;

    // all current instances of Two Phase Commits
    private final CoherentOrderedReplicator twopc;

    // TODO: document
    private final State state;

    // TODO: document
    //private final Coordinator coordinator;
    private final Log coordinatorLog;
    // TODO: document
    private final Log participantLog;

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
            config.getRemoteServerAddresses(),
            config.getRemoteServerIdsAddresses()
        );
    }

    /**
     * TODO: document
     *
     * @param localServerId the identifier of this server
     * @param localServerPort the port to be used by this server
     * @param remoteServerAddresses all remote server identifiers keyed by their
     *     addresses
     */
    public Server(
        ServerId localServerId,
        int localServerPort,
        Collection< Address > remoteServerAddresses,
        Collection< ServerIdAddress> remoteServerIdsAddresses
    )
    {
        this.localServerId = Objects.requireNonNull(localServerId);

        this.remoteServerAddresses = new HashSet<>(remoteServerAddresses);

        this.remoteServerIdsAddresses = new ArrayList<>(remoteServerIdsAddresses);

        this.messaging = new NettyMessagingService(
            Config.NETTY_CLUSTER_NAME,
            Address.from(localServerPort),
            new MessagingConfig()
        );

        this.serializer =
            Serializer
                .builder()
                .withTypes(MsgChirp.class, MsgAck.class, ServerId.class, MsgCommit.class, MsgRollback.class)
                .build();

        this.clock = Long.MIN_VALUE;

        this.pendingChirps = new HashMap<>();

        this.participantRole = new HashMap<>();

        this.twopc = new CoherentOrderedReplicator<MsgChirp>(localServerId.getValue(), remoteServerIdsAddresses, this.messaging, MsgChirp.class);

        this.coordinatorLog = new Log("coordinator",this.localServerId.getValue());

        this.participantLog = new Log("participant",this.localServerId.getValue());

        this.state = new State();

        // register message handlers

        final var exec = Executors.newFixedThreadPool(1);

        this.messaging.registerHandler(
            Config.CLIENT_GET_MSG_NAME, this::handleClientGet, exec
        );

        this.messaging.registerHandler(
            Config.CLIENT_PUBLISH_MSG_NAME, this::handleClientPublish
        );

        this.messaging.registerHandler(
            Config.SERVER_PUBLISH_MSG_NAME, this::handleServerPublish, exec
        );

        this.messaging.registerHandler(
            Config.SERVER_ACK_PUBLICATION_MSG_NAME, this::handleServerAck, exec
        );

        this.messaging.registerHandler(
            Config.SERVER_VOTE_OK_MSG_NAME, this::handleServerOk, exec
        );

        this.messaging.registerHandler(
            Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, this::handleServerCommit, exec
        );

        this.messaging.registerHandler(
            Config.SERVER_ROLLBACK_PUBLICATION_MSG_NAME, this::handleServerRollback, exec
        );
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

    /**
     * Participants get to know of a new Chirp and are asked to prepare by the Coordinator.
     * Start of the Two Phase Commit.
     */
    private void handleServerPublish(Address from, byte[] payload)
    {

        final var msg = this.serializer.< MsgChirp >decode(payload);

        // "synchronize" and tick clock

        this.clock = Math.max(this.clock, msg.timestamp) + 1;

        System.out.println("First phase of the 2PC");

        // prepare

        var decision = prepareCommit(msg);

        // Vote and wait for outcome, Coordinator decision

        waitOutcome(decision,from,msg.timestamp);

    }

    /**
     * Wait for the outcome of the the two phase commit.
     * @param decision
     * @param from
     * @param timestamp
     */
    private void waitOutcome(
        CompletableFuture< Void > decision,
        Address from,
        long timestamp
        )
    {

        // send acknowledgment, Vote OK - First phase answer

        this.messaging.sendAsync(
            from,
            Config.SERVER_VOTE_OK_MSG_NAME,
            this.serializer.encode(new MsgAck(this.localServerId, timestamp))
        );

        decision.thenAccept(v ->
        {

            // Act according to decision made by the Coordinator
            var participant = this.participantRole.get(timestamp);

            var action = participant.getDecision();

            System.out.println("Second phase of the 2PC");

            if (action instanceof MsgCommit)
            {
                var msg = participant.getMsgChirp();
                System.out.println("Decision: Commit Chirp -> "+msg.text);
                this.state.addChirp(msg.serverId, msg.timestamp, msg.text);

                participant.commit();

            }
            else {
                participant.beginRollBack();
            }

            // send acknowledgment - Two Phase Commit Over
            this.messaging.sendAsync(
                from,
                Config.SERVER_ACK_PUBLICATION_MSG_NAME,
                this.serializer.encode(new MsgAck(this.localServerId, timestamp))
            );

            System.out.println("Terminou 2PC.");
        }
        );

    }

    /**
     * Prepare the log for the new chirp.
     * @param msg
     * @return
     */
    private CompletableFuture< Void > prepareCommit(MsgChirp msg)
    {
        var decision = new CompletableFuture< Void >();

        var participant = new Participant(decision,participantLog,msg);

        this.participantRole.put(msg.timestamp,participant);

        participant.prepare();

        return decision;
    }

    private void handleServerCommit(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgCommit >decode(payload);

        this.participantRole.get(msg.timestamp).setDecision(msg);
    }

    private void handleServerRollback(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgRollback >decode(payload);

        this.participantRole.get(msg.timestamp).setDecision(msg);
    }

    private void handleServerAck(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgAck >decode(payload);

        this.pendingChirps.get(msg.timestamp).ackServer(msg.serverId);
    }

    private void handleServerOk(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgAck >decode(payload);

        this.pendingChirps.get(msg.timestamp).serverVote(msg.serverId);
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
        System.out.println("About to Start publishing Chirp...");

        final var timestamp = this.clock++;

        var msgchirp = new MsgChirp(localServerId,timestamp,chirp);

        return twopc.put(msgchirp);

        /*
        // 2 Completables, 1 for the Server Votes (Phase 1) and 1 for the Acks (Phase 2)

        var voteFuture = new CompletableFuture< Void >();
        var ackFuture = new CompletableFuture< Void >();

        // create pending chirp

        this.pendingChirps.put(
             timestamp,
            new PendingChirp(this.remoteServerAddresses.size(), ackFuture, voteFuture)
        );

        var firstPhase = askToVote(voteFuture,chirp,timestamp);

        return firstPhase.thenAccept(v ->
        {

            if (v.equals("Commit"))
                askToCommit(ackFuture,timestamp).thenRun(()->
                {
                    this.pendingChirps.remove(timestamp);
                    this.state.addChirp(this.localServerId, timestamp, chirp);
                    System.out.println("Terminou o 2PC.");
                });
            else
                {
                    askToRollback(ackFuture,timestamp,chirp).thenRun(() ->
                    {

                    });
                }

        });*/

        /*
        return sendFuture.thenAcceptBoth(ackFuture, (v1, v2) -> {
            this.pendingChirps.remove(timestamp);
            this.state.addChirp(this.localServerId, timestamp, chirp);
        }); */
    }

    public CompletableFuture< String > askToVote(CompletableFuture< Void > voteFuture, String chirp, long timestamp)
    {
        // Insert participants into Coordinator Log

        coordinatorLog.add(this.remoteServerAddresses);

        // send chirp to servers

        var msgchirp = new MsgChirp(this.localServerId, timestamp, chirp);

        final var payload = this.serializer.encode(
            msgchirp
        );

        System.out.println("Starting First phase of the 2PC");

        final var sendFuture = CompletableFuture.allOf(
            this.remoteServerAddresses
                .stream()
                .map(
                    address -> this.messaging.sendAsync(
                        address, Config.SERVER_PUBLISH_MSG_NAME, payload
                    )
                )
                .toArray(CompletableFuture[]::new)
        );



        // (when we sent all reqs and received all acks, ...)

        return sendFuture.thenAcceptBoth(voteFuture, (v1, v2) -> {
            System.out.println("Recebeu todos os Votos.");
            this.participantLog.add(msgchirp);
            this.participantLog.add(new Prepared());
        })
            .thenApply(v -> "Commit")
            .exceptionally(v ->
        {
            System.out.println("Nao recebeu todos os Votos.");
            return "Abort";
        });
    }

    public CompletableFuture< Void > askToCommit(CompletableFuture< Void > ackFuture, long chirpTimestamp)
    {
        this.coordinatorLog.add(new Commit());

        final var commit = this.serializer.encode(new MsgCommit(this.localServerId,chirpTimestamp));

        final var sendCommit = CompletableFuture.allOf(
            this.remoteServerAddresses
                .stream()
                .map(
                    address -> this.messaging.sendAsync(
                        address, Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, commit
                    )
                )
                .toArray(CompletableFuture[]::new)
        );

        // Gather all Acks of the second phase

        return sendCommit.thenAcceptBoth(ackFuture, (v3, v4) -> {
            System.out.println("Recebeu todos os ACK.");
            this.participantLog.add(new Commit());
        });
    }

    private CompletionStage< Void > askToRollback(CompletableFuture<Void> ackFuture, long timestamp, String chirp)
    {
        this.coordinatorLog.add(new Abort());

        final var abort = this.serializer.encode(new MsgRollback(this.localServerId,timestamp,2));

        final var sendCommit = CompletableFuture.allOf(
            this.remoteServerAddresses
                .stream()
                .map(
                    address -> this.messaging.sendAsync(
                        address, Config.SERVER_ROLLBACK_PUBLICATION_MSG_NAME, abort
                    )
                )
                .toArray(CompletableFuture[]::new)
        );

        // Gather all Acks of the rollback

        return sendCommit.thenAcceptBoth(ackFuture, (v3, v4) -> {
            System.out.println("Recebeu todos os Rollbacks.");
            this.participantLog.add(new Abort());
        });
    }
}

/* -------------------------------------------------------------------------- */
