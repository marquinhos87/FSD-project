/* -------------------------------------------------------------------------- */

package chirper.server;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class PendingChirp
{
    private final int numRemoteServers;
    private final Set< ServerId > ackedServerIds;

    private final CompletableFuture< Void > onAllAcked;

    /**
     * TODO: document
     *
     * @param numRemoteServers TODO: document
     * @param onAllAcked TODO: document
     */
    public PendingChirp(
        int numRemoteServers,
        CompletableFuture< Void > onAllAcked
    )
    {
        this.numRemoteServers = numRemoteServers;
        this.ackedServerIds = new HashSet<>();

        this.onAllAcked = Objects.requireNonNull(onAllAcked);

        checkAllAcked();
    }

    /**
     * TODO: document
     *
     * @param serverId TODO: document
     */
    public void ackServer(ServerId serverId)
    {
        this.ackedServerIds.add(serverId);

        checkAllAcked();
    }

    /**
     * TODO: document
     */
    private void checkAllAcked()
    {
        if (this.ackedServerIds.size() == this.numRemoteServers)
        {
            this.onAllAcked.complete(null);
        }
    }
}

/* -------------------------------------------------------------------------- */
