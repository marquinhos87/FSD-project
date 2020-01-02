/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgCommit
{
    public ServerId serverId;
    public long chirpTimestamp;

    /**
     * TODO: document
     */
    public MsgCommit(ServerId serverId, long chirpTimestamp)
    {

        this.serverId = Objects.requireNonNull(serverId);
        this.chirpTimestamp = chirpTimestamp;
    }
}

/* -------------------------------------------------------------------------- */
