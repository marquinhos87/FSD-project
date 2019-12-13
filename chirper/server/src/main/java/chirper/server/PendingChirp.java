/* -------------------------------------------------------------------------- */

package chirper.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class PendingChirp
{
    private final Set< ServerId > unackedServerIds;
    private final CompletableFuture< Void > onAllAcked;

    /**
     * TODO: document
     *
     * @param serverIds TODO: document
     * @param onAllAcked TODO: document
     */
    public PendingChirp(
        Collection< ServerId > serverIds,
        CompletableFuture< Void > onAllAcked
    )
    {
        this.unackedServerIds = new HashSet<>(serverIds);
        this.onAllAcked = onAllAcked;

        checkEmpty();
    }

    /**
     * TODO: document
     *
     * @param serverId TODO: document
     */
    public void ackServer(ServerId serverId)
    {
        this.unackedServerIds.remove(serverId);

        checkEmpty();
    }

    /**
     * TODO: document
     */
    private void checkEmpty()
    {
        if (this.unackedServerIds.isEmpty())
            this.onAllAcked.complete(null);
    }
}

/* -------------------------------------------------------------------------- */
