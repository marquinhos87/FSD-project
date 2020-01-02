package chirper.server.broadcast;

import chirper.server.network.ServerId;
import chirper.server.network.ServerNetwork;
import chirper.shared.Config;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Implements 2PC for committing arbitrary objects, and thus handles all
 * inter-server communication.
 *
 * Call put() to run an instance of 2PC; the returned future completes
 * successfully with true if 2PC commits, successfully with false if 2PC aborts,
 * and exceptionally if the onMessageReceived callback throws an exception.
 *
 * Note that the future returned by put() only completes after the
 * onMessageReceived callback is run locally for the value passed to put().
 *
 * The onMessageReceived callback is called for any 2PC that commits, be it
 * coordinated by the local server or by another server. Also, the callback is
 * called for each committed value in the same order in all servers.
 *
 * @param <T> the type of things to be committed
 */

public class AllOrNothingOrderedBroadcaster<T> extends Broadcaster<T>
{
    private final ServerNetwork serverNetwork;

    private long n_twopc = 0;

    private Log coordinatorLog;
    private Log participantLog;

    // participating
    private Map<pair, Participant > participating;

    // Coordinating
    private Map<Long,PendingTransaction> coordinating;


    private class pair
    {
        public ServerId serverid;
        public long twopc_id;

        public pair(ServerId serverId, long twopc_id)
        {
            this.serverid = serverId;
            this.twopc_id = twopc_id;
        }

        public ServerId getServerid()
        {
            return this.serverid;
        }

        public long getTwopc_id()
        {
            return this.twopc_id;
        }
        @Override
        public boolean equals(Object obj)
        {
            return
                obj != null &&
                    this.getClass() == obj.getClass() &&
                    this.serverid.equals(((pair)obj).getServerid()) &&
                    this.twopc_id == ((pair)obj).getTwopc_id();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.serverid.getValue(),twopc_id);
        }
    }

    public AllOrNothingOrderedBroadcaster(
        ServerNetwork serverNetwork,
        Consumer<T> onMessageReceived,
        Class<T> type
    )
    {
        super(serverNetwork,onMessageReceived);

        this.serverNetwork = serverNetwork;

        this.participating = new HashMap<>();

        this.coordinating = new HashMap<>();

        this.coordinatorLog = new Log("coordinator",serverNetwork.getLocalServerId().getValue(),type);

        this.participantLog = new Log("participant",serverNetwork.getLocalServerId().getValue(),type);

        serverNetwork.registerPayloadType(MsgAck.class);

        serverNetwork.registerPayloadType(MsgCommit.class);

        serverNetwork.registerPayloadType(MsgPrepare.class);

        serverNetwork.registerPayloadType(MsgRollback.class);

        serverNetwork.registerPayloadType(ServerId.class);

        serverNetwork.registerHandler(Config.SERVER_PUBLISH_MSG_NAME,this::handleServerPublish);

        serverNetwork.registerHandler(Config.SERVER_ACK_PUBLICATION_MSG_NAME, this::handleServerAck);

        serverNetwork.registerHandler(Config.SERVER_VOTE_OK_MSG_NAME, this::handleServerOk);

        serverNetwork.registerHandler(Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, this::handleServerCommit);

        serverNetwork.registerHandler(Config.SERVER_ROLLBACK_PUBLICATION_MSG_NAME, this::handleServerRollback);
    }

    private void handleServerCommit(ServerId serverId, MsgCommit value)
    {
        this.participating.get(new pair(value.serverId,value.twopc_id)).setDecision(value);
    }

    private void handleServerRollback(ServerId serverId, MsgRollback value)
    {
        this.participating.get(new pair(serverId,value.twopc_id)).setDecision(value);
    }

    private void handleServerAck(ServerId serverId, MsgAck value)
    {
        this.coordinating.get(value.twopc_id).ackServer(value.serverId);
    }

    private void handleServerOk(ServerId serverId, MsgAck value)
    {
        this.coordinating.get(value.twopc_id).serverVote(value.serverId);
    }

    private void handleServerPublish(ServerId serverId, MsgPrepare value)
    {
        // "synchronize" and tick clock

        //this.clock = Math.max(this.clock, msg.timestamp) + 1;

        System.out.println("First phase of the 2PC");

        // prepare

        var decision = prepareCommit(value);

        // Vote and wait for outcome, Coordinator decision

        waitOutcome(decision,serverId,value.twopc_id);

        getOnMessageReceived().accept((T) value.content);

        this.participating.remove(new pair(value.serverId,value.twopc_id));
    }

    /**
     * Prepare the log for the new chirp.
     * @param msg
     * @return
     */
    private CompletableFuture< Void > prepareCommit(MsgPrepare msg)
    {
        var decision = new CompletableFuture< Void >();

        var participant = new Participant(decision,participantLog,msg.content);

        this.participating.put(new pair(msg.serverId,msg.twopc_id),participant);

        participant.prepare();

        return decision;
    }

    /**
     * Wait for the outcome of the the two phase commit.
     * @param decision
     * @param serverId
     * @param twopc_id
     */
    private void waitOutcome(
        CompletableFuture< Void > decision,
        ServerId serverId,
        long twopc_id
    )
    {
         System.out.println("Waiting for Outcome");
        // send acknowledgment, Vote OK - First phase answer

        this.serverNetwork.send(
            serverId,
            Config.SERVER_VOTE_OK_MSG_NAME,
            new MsgAck(this.serverNetwork.getLocalServerId(), twopc_id)
        );

        decision.thenAccept(v ->
            {
                // Act according to decision made by the Coordinator
                var participant = this.participating.get(new pair(serverId,twopc_id));

                var action = participant.getDecision();

                System.out.println("Second phase of the 2PC");

                if (action instanceof MsgCommit)
                {
                    System.out.println("Decision: Commit Chirp -> ");

                    participant.commit();
                }
                else {
                    participant.beginRollBack();
                }

                // send acknowledgment - Two Phase Commit Over
                this.serverNetwork.send(
                    serverId,
                    Config.SERVER_ACK_PUBLICATION_MSG_NAME,
                    new MsgAck(this.serverNetwork.getLocalServerId(), twopc_id)
                );

                System.out.println("Terminou 2PC.");
            }
        );

    }

    public CompletableFuture< String > askToVote(CompletableFuture< Void > voteFuture, T value, long twopc_id)
    {
        // Insert participants into Coordinator Log

        //coordinatorLog.add(serverNetwork.); // Nao funciona a leitura do log depois

        // send chirp to servers

        var msg = new MsgPrepare<T>(this.serverNetwork.getLocalServerId(), twopc_id, value);

        System.out.println("Starting First phase of the 2PC");

        final var sendFuture = CompletableFuture.allOf(
            this.serverNetwork.getRemoteServerIds()
                .stream()
                .map(
                    serverId -> this.getServerNetwork().send(
                        serverId, Config.SERVER_PUBLISH_MSG_NAME, msg) // MsgPrepare
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

    public CompletableFuture< Void > askToCommit(CompletableFuture< Void > ackFuture, long twopc_id)
    {
        this.coordinatorLog.add(new Commit());

        final var commit =new MsgCommit(this.serverNetwork.getLocalServerId(),twopc_id);

        final var sendCommit = CompletableFuture.allOf(
            this.serverNetwork.getRemoteServerIds()
                .stream()
                .map(
                    serverId -> this.getServerNetwork().send(
                        serverId, Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, commit
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

    private CompletionStage< Void > askToRollback(CompletableFuture<Void> ackFuture, long twopc_id, T value)
    {
        this.coordinatorLog.add(new Abort());

        final var abort = new MsgRollback(this.serverNetwork.getLocalServerId(),twopc_id,2);

        final var sendCommit = CompletableFuture.allOf(
            this.serverNetwork.getRemoteServerIds()
                .stream()
                .map(
                    serverId -> this.getServerNetwork().send(
                        serverId, Config.SERVER_ROLLBACK_PUBLICATION_MSG_NAME, abort
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

    @Override
    public CompletableFuture< Boolean > broadcast(T value)
    {
        final var id = this.n_twopc++;

        var voteFuture = new CompletableFuture< Void >();

        var ackFuture = new CompletableFuture< Void >();

        // create pending chirp

        this.coordinating.put(
            id,
            new PendingTransaction(this.serverNetwork.getRemoteServerIds().size(), ackFuture, voteFuture)
        );

        var firstPhase = askToVote(voteFuture,value,id);

        return firstPhase.thenAccept(v ->
            {
                askToCommit(ackFuture,id).thenRun(()->
                {
                    this.coordinating.remove(id);
                    getOnMessageReceived().accept(value);
                    System.out.println("Terminou o 2PC.");
                });
            }).thenApply(v -> true);
    }
}
