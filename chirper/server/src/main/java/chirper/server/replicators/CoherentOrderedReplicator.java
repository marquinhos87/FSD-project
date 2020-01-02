package chirper.server.replicators;

import chirper.server.network.ServerId;
import chirper.shared.Config;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

/**
 * Implements 2PC for committing arbitrary objects, and thus handles all
 * inter-server communication.
 *
 * Call put() to run an instance of 2PC; the returned future completes
 * successfully with true if 2PC commits, successfully with false if 2PC aborts,
 * and exceptionally if the onValueCommitted callback throws an exception.
 *
 * Note that the future returned by put() only completes after the
 * onValueCommitted callback is run locally for the value passed to put().
 *
 * The onValueCommitted callback is called for any 2PC that commits, be it
 * coordinated by the local server or by another server. Also, the callback is
 * called for each committed value in the same order in all servers.
 *
 * @param <T> the type of things to be committed
 */

public class CoherentOrderedReplicator<T> extends Replicator<T>
{
    private final ServerId localServerId;

    private final List < ServerIdAddress > remoteServerAddressesAndIds;

    private final ManagedMessagingService messaging;

    private final Serializer serializer;

    private long n_twopc = 0;

    private Log coordinatorLog;
    private Log participantLog;

    // participating
    private Map<pair, Participant > participating;

    // Coordinating
    private Map<Long,pendingTransaction> coordinating;

    private class pendingTransaction
    {
        private final int numRemoteServers;
        private long id;
        private final Set< ServerId > ackedServerIds;
        private final Set< ServerId > votedServerIds;
        private final CompletableFuture< Void > onAllAcked;
        private final CompletableFuture< Void > onAllVoted;

        /**
         * TODO: document
         *
         * @param numRemoteServers TODO: document
         * @param onAllAcked TODO: document
         */
        public pendingTransaction
        (
            int numRemoteServers,
            CompletableFuture< Void > onAllAcked,
            CompletableFuture< Void > onAllVoted
        )
        {
            this.numRemoteServers = numRemoteServers;
            this.ackedServerIds = new HashSet<>();
            this.votedServerIds = new HashSet<>();
            this.onAllAcked = Objects.requireNonNull(onAllAcked);
            this.onAllVoted = Objects.requireNonNull(onAllVoted);

            checkAllVoted();
            checkAllAcked();
        }

        public void serverVote(ServerId serverId)
        {
            if(!this.votedServerIds.contains(serverId))
            {
                this.votedServerIds.add(serverId);
                checkAllVoted();
            }
        }

        private void checkAllVoted()
        {
            if (this.votedServerIds.size() == this.numRemoteServers)
            {
                this.onAllVoted.complete(null);
            }
        }

        public void ackServer(ServerId serverId)
        {
            this.ackedServerIds.add(serverId);

            checkAllAcked();
        }

        private void checkAllAcked()
        {
            if (this.ackedServerIds.size() == this.numRemoteServers)
            {
                this.onAllAcked.complete(null);
            }
        }
    }

    private class pair
    {
        public ServerId serverid;
        public long twopc_id;

        public pair(ServerId serverId, long twopc_id)
        {
            this.serverid = serverId;
            this.twopc_id = twopc_id;
        }
    }

    public CoherentOrderedReplicator(
        int localServerId,
        Collection<chirper.server.ServerIdAddress> remoteServerAddressesAndIds,
        ManagedMessagingService messaging,
        /*Consumer<T> onValueCommitted,*/
        Class<chirper.server.MsgChirp> type
    )
    {
        this.localServerId = new ServerId(localServerId);

        this.remoteServerAddressesAndIds = remoteServerAddressesAndIds;

        this.serializer = Serializer.builder()
                .withTypes(type,
                           MsgAck.class, ServerId.class, MsgCommit.class, MsgRollback.class,
                           MsgPrepare.class)
                .build();

        this.messaging = messaging;

        this.participating = new HashMap<>();

        this.coordinating = new HashMap<>();

        this.coordinatorLog = new Log("coordinator",this.localServerId.getValue(),type);

        this.participantLog = new Log("participant",this.localServerId.getValue(),type);

        final var exec = Executors.newFixedThreadPool(1);

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

    private void handleServerCommit(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgCommit >decode(payload);

        this.participating.get(msg.twopc_id).setDecision(msg);
    }

    private void handleServerRollback(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgRollback >decode(payload);

        this.participating.get(msg.twopc_id).setDecision(msg);
    }

    private void handleServerAck(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgAck >decode(payload);

        this.coordinating.get(msg.twopc_id).ackServer(msg.serverId);
    }

    private void handleServerOk(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgAck >decode(payload);

        this.coordinating.get(msg.twopc_id).serverVote(msg.serverId);
    }

    private void handleServerPublish(Address from, byte[] payload)
    {

        final var msg = this.serializer.<MsgPrepare>decode(payload);
        // "synchronize" and tick clock

        //this.clock = Math.max(this.clock, msg.timestamp) + 1;

        System.out.println("First phase of the 2PC");

        // prepare

        var decision = prepareCommit(msg);

        // Vote and wait for outcome, Coordinator decision

        waitOutcome(decision,from,msg.twopc_id);

    }

    /**
     * Prepare the log for the new chirp.
     * @param msg
     * @return
     */
    private CompletableFuture< Void > prepareCommit(MsgPrepare msg)
    {
        var decision = new CompletableFuture< Void >();

        var participant = new Participant(decision,participantLog,msg);

        this.participating.put(new pair(msg.serverId,msg.twopc_id),participant);

        participant.prepare();

        return decision;
    }

    /**
     * Wait for the outcome of the the two phase commit.
     * @param decision
     * @param from
     * @param twopc_id
     */
    private void waitOutcome(
        CompletableFuture< Void > decision,
        Address from,
        long twopc_id
    )
    {

        // send acknowledgment, Vote OK - First phase answer

        this.messaging.sendAsync(
            from,
            Config.SERVER_VOTE_OK_MSG_NAME,
            this.serializer.encode(new MsgAck(this.localServerId, twopc_id))
        );

        decision.thenAccept(v ->
            {

                // Act according to decision made by the Coordinator
                var participant = this.participating.get(twopc_id);

                var action = participant.getDecision();

                System.out.println("Second phase of the 2PC");

                if (action instanceof MsgCommit)
                {
                    var msg = participant.getMsgChirp();
                    System.out.println("Decision: Commit Chirp -> ");
                    //this.state.addChirp(msg.serverId, msg.timestamp, msg.text);

                    participant.commit();

                }
                else {
                    participant.beginRollBack();
                }

                // send acknowledgment - Two Phase Commit Over
                this.messaging.sendAsync(
                    from,
                    Config.SERVER_ACK_PUBLICATION_MSG_NAME,
                    this.serializer.encode(new MsgAck(this.localServerId, twopc_id))
                );

                System.out.println("Terminou 2PC.");
            }
        );

    }

    public CompletableFuture< String > askToVote(CompletableFuture< Void > voteFuture, T value, long id)
    {
        // Insert participants into Coordinator Log

        coordinatorLog.add(this.remoteServerAddressesAndIds);

        // send chirp to servers

        //var msgchirp = new MsgChirp(this.localServerId, id, chirp);

        final var payload = this.serializer.encode(
            value
        );

        System.out.println("Starting First phase of the 2PC");

        final var sendFuture = CompletableFuture.allOf(
            this.remoteServerAddressesAndIds
                .stream()
                .map(
                    serverIdAddress -> this.messaging.sendAsync(
                        serverIdAddress.ad, Config.SERVER_PUBLISH_MSG_NAME, payload
                    )
                )
                .toArray(CompletableFuture[]::new)
        );



        // (when we sent all reqs and received all acks, ...)

        return sendFuture.thenAcceptBoth(voteFuture, (v1, v2) -> {
            System.out.println("Recebeu todos os Votos.");
            this.participantLog.add(value);
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
            this.remoteServerAddressesAndIds
                .stream()
                .map(
                    serverIdAddress -> this.messaging.sendAsync(
                        serverIdAddress.ad, Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, commit
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

    private CompletionStage< Void > askToRollback(CompletableFuture<Void> ackFuture, long timestamp, T value)
    {
        this.coordinatorLog.add(new Abort());

        final var abort = this.serializer.encode(new MsgRollback(this.localServerId,timestamp,2));

        final var sendCommit = CompletableFuture.allOf(
            this.remoteServerAddressesAndIds
                .stream()
                .map(
                    serverIdAddress -> this.messaging.sendAsync(
                        serverIdAddress.ad, Config.SERVER_ROLLBACK_PUBLICATION_MSG_NAME, abort
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

    public CompletableFuture< Boolean > put(T value)
    {
        final var id = this.n_twopc++;

        var voteFuture = new CompletableFuture< Void >();

        var ackFuture = new CompletableFuture< Void >();

        // create pending chirp

        this.coordinating.put(
            id,
            new pendingTransaction(this.remoteServerAddressesAndIds.size(), ackFuture, voteFuture)
        );

        var firstPhase = askToVote(voteFuture,value,id);

        return firstPhase.thenAccept(v ->
            {
                askToCommit(ackFuture,id).thenRun(()->
                {
                    this.coordinating.remove(id);
                    //this.state.addChirp(this.localServerId, id, value);
                    System.out.println("Terminou o 2PC.");
                });
            }).thenApply(v -> true);
    }
}
