/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgRol
{
    public ServerId serverId;
    public long chirpTimestamp;
    public int numRemove;

    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public MsgRol(ServerId serverId, long chirpTimestamp, int numRemove)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.chirpTimestamp = chirpTimestamp;
        this.numRemove = numRemove;
    }
}

/* -------------------------------------------------------------------------- */
