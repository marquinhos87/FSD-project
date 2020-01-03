package chirper.server.broadcast;

import chirper.server.network.ServerId;
import chirper.server.network.ServerNetwork;
import chirper.shared.Config;
import io.atomix.storage.journal.SegmentedJournalReader;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

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
        SegmentedJournalReader<Object> sjr = this.coordinatorLog.getReader();
        String s = "";
        Object aux;
        while(sjr.hasNext()) {
            aux = sjr.next().entry();
            if(aux instanceof String)
                s = (String) aux;
            else if(aux instanceof Prepared) {
                this.coordinating.put(
                    ((Prepared) aux).twopc_id,
                    new PendingTransaction(
                        serverNetwork.getRemoteServerIds().size(),
                        new CompletableFuture<>(),
                        new CompletableFuture<>(),
                        ((Prepared) aux).twopc_id,
                        s)
                );
                n_twopc = Math.max(n_twopc,((Prepared) aux).twopc_id);
            }
            else if(aux instanceof Commit) {
                if(this.coordinating.containsKey(((Commit) aux).twopc_id))
                    this.coordinating.remove(((Commit) aux).twopc_id);
                n_twopc = Math.max(n_twopc,((Commit) aux).twopc_id);
            }
            else if(aux instanceof Abort) {
                if(this.coordinating.containsKey(((Abort) aux).twopc_id))
                    this.coordinating.remove(((Abort) aux).twopc_id);
                n_twopc = Math.max(n_twopc, ((Abort) aux).twopc_id);
            }
        }

        this.participantLog = new Log("participant",serverNetwork.getLocalServerId().getValue(),type);
        sjr = this.participantLog.getReader();
        while(sjr.hasNext()) {
            aux = sjr.next().entry();
            if(aux instanceof String)
                s = (String) aux;
            else if(aux instanceof Prepared)
                this.participating.put(
                    new pair(((Prepared) aux).serverId,((Prepared) aux).twopc_id),
                    new Participant(new CompletableFuture<>(),this.participantLog,s));
            else if(aux instanceof Commit)
                if(this.participating.containsKey(new pair(((Commit) aux).serverId,((Commit) aux).twopc_id)))
                    this.participating.remove(new pair(((Commit) aux).serverId,((Commit) aux).twopc_id));
            else if(aux instanceof Abort)
                if(this.participating.containsKey(new pair(((Commit) aux).serverId,((Commit) aux).twopc_id)))
                    this.participating.remove(new pair(((Commit) aux).serverId,((Commit) aux).twopc_id));
        }

        serverNetwork.registerPayloadType(type);
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
        this.participating.get(new pair(serverId,value.twopc_id)).setDecision(value);
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

        waitOutcome(decision,serverId,value.twopc_id, (T) value.content);
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

        participant.prepare(msg.serverId,msg.twopc_id);

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
        long twopc_id,
        T value
    )
    {
        if(serverNetwork.getLocalServerId().getValue()==2) System.exit(-1);
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
                    getOnMessageReceived().accept((T) value);
                    participant.commit(serverId,twopc_id);
                }
                else {
                    participant.beginRollBack(serverId,twopc_id);
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

    public CompletableFuture<String> askToVote(CompletableFuture< Boolean > voteFuture, T value, long twopc_id)
    {
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
        return sendFuture.thenAcceptAsync(v -> voteFuture.thenRun(()->{})).thenApply(v->"Commit").exceptionally(v->"Abort");

        /*return sendFuture.thenAcceptBothAsync(voteFuture, (v1, v2) -> {
            System.out.println("Recebeu todos os Votos.");
        })
            .thenApply(v -> "Commit");*/
    }

    public CompletableFuture< Void > askToCommit(CompletableFuture< Boolean > ackFuture, long twopc_id)
    {
        final var commit = new MsgCommit(this.serverNetwork.getLocalServerId(),twopc_id);

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

        return sendCommit.thenAcceptBothAsync(ackFuture, (v3, v4) -> {
            System.out.println("Recebeu todos os ACK.");
        });
    }

    private CompletableFuture< Boolean > askToRollback(CompletableFuture<Boolean> ackFuture, long twopc_id, T value)
    {
        System.out.println("Chega aqui?");
        final var abort = new MsgRollback(this.serverNetwork.getLocalServerId(),twopc_id,2);

        final var sendRollback = CompletableFuture.allOf(
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

        /*return sendRollback.thenAcceptAsync(v -> {
            System.out.println("Recebeu todos os Rollbacks.");
        }).thenApply(v -> true).exceptionally(v -> true);*/

        return sendRollback.thenAccept(v -> {
            ackFuture.thenApply(v2 -> true).exceptionally(v3 -> false);
        }).thenApply(v -> true).exceptionally(v -> false);
    }

    public void repeatTransactions() {
        for(Map.Entry<Long,PendingTransaction> aux: this.coordinating.entrySet()) {
            PendingTransaction p = aux.getValue();
            var firstPhase = askToVote(p.onAllVoted, (T) p.value,p.id);
            firstPhase.thenAccept(v ->
            {
                askToCommit(p.onAllAcked,p.id).thenRun(()->
                {
                    this.coordinating.remove(p.id);
                    getOnMessageReceived().accept((T) p.value);
                    System.out.println("Terminou o 2PC.");
                });
            });
        }

        for(Map.Entry<pair,Participant> aux: this.participating.entrySet()) {
            pair pp = aux.getKey();
            Participant p = aux.getValue();
            System.out.println(p.thing);
            waitOutcome(p.pendingDecision,pp.serverid,pp.twopc_id, (T) p.thing);
        }
    }

    @Override
    public CompletableFuture< Boolean > broadcast(T value)
    {
        final var id = this.n_twopc++;

        var voteFuture = new CompletableFuture< Boolean >();

        var ackFuture = new CompletableFuture< Boolean >();

        // create pending chirp

        this.coordinating.put(
            id,
            new PendingTransaction(this.serverNetwork.getRemoteServerIds().size(), ackFuture, voteFuture,id,value)
        );

        this.coordinatorLog.appendEntry(value);
        this.coordinatorLog.appendEntry(new Prepared(serverNetwork.getLocalServerId(),id));

        var firstPhase = askToVote(voteFuture,value,id);

        /*return firstPhase.thenAccept(v ->
            {
                askToCommit(ackFuture,id).thenRun(()->
                {
                    getOnMessageReceived().accept(value);
                    System.out.println("Terminou o 2PC.");
                });
            }).thenApply(v -> true);*/

        return firstPhase.thenAccept(v ->
        {
            if (v.equals("Commit")){
                this.coordinatorLog.appendEntry(new Commit(serverNetwork.getLocalServerId(), id));
                askToCommit(ackFuture, id).thenRun(() ->
                {
                    getOnMessageReceived().accept(value);
                    System.out.println("Terminou o 2PC.");
                });
            }
            else {
                System.out.println(v);
                this.coordinatorLog.appendEntry(new Commit(serverNetwork.getLocalServerId(), id));
                askToRollback(ackFuture,id,value).thenRun(() ->
                {
                    System.out.println("Abortou o 2PC.");
                });
            }
        }).thenApply(v -> true);


        /*try {
            String string = firstPhase.get(2, TimeUnit.SECONDS);
            System.out.println(string);
            if (string.equals("Commit")){
                this.coordinatorLog.add(new Commit(serverNetwork.getLocalServerId(),id));
                askToCommit(ackFuture, id).thenRun(() ->
                {
                    getOnMessageReceived().accept(value);
                    System.out.println("Terminou o 2PC.");
                });
                return firstPhase.thenApply(v -> true);
            }
            else {
                System.out.println(string);
                this.coordinatorLog.add(new Commit(serverNetwork.getLocalServerId(),id));
                askToRollback(ackFuture,id,value).thenRun(() ->
                {
                    System.out.println("Abortou o 2PC.");
                });
                return firstPhase.thenApply(v -> false);
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            this.coordinatorLog.add(new Commit(serverNetwork.getLocalServerId(),id));
            askToRollback(ackFuture,id,value).thenRun(() ->
            {
                System.out.println("Abortou o 2PC.");
            });
            return firstPhase.thenApply(v -> false);*
        }*/
    }
}
