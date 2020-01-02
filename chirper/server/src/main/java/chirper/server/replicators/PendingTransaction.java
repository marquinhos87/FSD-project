package chirper.server.replicators;

import chirper.server.network.ServerId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PendingTransaction {
    private final int numRemoteServers;
    private long id;
    private final Set<ServerId> ackedServerIds;
    private final Set< ServerId > votedServerIds;
    private final CompletableFuture< Void > onAllAcked;
    private final CompletableFuture< Void > onAllVoted;

    /**
     * TODO: document
     *
     * @param numRemoteServers TODO: document
     * @param onAllAcked TODO: document
     */
    public PendingTransaction
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
