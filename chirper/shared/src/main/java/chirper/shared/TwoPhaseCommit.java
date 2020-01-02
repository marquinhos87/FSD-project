package chirper.shared;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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

public class TwoPhaseCommit<T>
{
    private final ServerId localServerId;

    private final List < ServerIdAddress > remoteServerAdressesAndIds;

    private final ManagedMessagingService messaging;

    private final Serializer serializer;

    private int n_twopc = 0;

    // participating
    private Map<Pair<ServerId,Integer>,Participant> twopc;

    // Coordinating
    private class pendingTransaction
    {
        private final int numRemoteServers;
        private int id;
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

    public TwoPhaseCommit(
        ServerId localServerId,
        List< ServerIdAddress > remoteServerAddressesAndIds,
        ManagedMessagingService messaging,
        Consumer<T> onValueCommitted,
        Class<T> type
    )
    {
        this.localServerId = localServerId;

        this.remoteServerAdressesAndIds = remoteServerAddressesAndIds;

        this.serializer = Serializer.builder()
                .withTypes(type,MsgAck.class, ServerId.class, MsgCommit.class, MsgRollback.class)
                .build();

        this.messaging = messaging;

        this.twopc = new HashMap<>();

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

        this.twopc.get(msg.timestamp).setDecision(msg);
    }

    private void handleServerRollback(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgRollback >decode(payload);

        //this.participantRole.get(msg.timestamp).setDecision(msg);
    }

    private void handleServerAck(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgAck >decode(payload);

        //this.pendingChirps.get(msg.timestamp).ackServer(msg.serverId);
    }

    private void handleServerOk(Address from, byte[] payload)
    {
        final var msg = this.serializer.< MsgAck >decode(payload);

        //this.pendingChirps.get(msg.timestamp).serverVote(msg.serverId);
    }

    private void handleServerPublish(Address from, byte[] payload)
    {

        final var msg = this.serializer.decode(payload);

        // "synchronize" and tick clock

        //this.clock = Math.max(this.clock, msg.timestamp) + 1;

        System.out.println("First phase of the 2PC");

        // prepare

        //var decision = prepareCommit(msg);

        // Vote and wait for outcome, Coordinator decision

        //waitOutcome(decision,from,msg.timestamp);

    }

    public CompletableFuture< Boolean > put(T value)
    {
        this.n_twopc++;

        return CompletableFuture.completedFuture(null);
    }
}
