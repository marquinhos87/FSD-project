/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgRollback
{
    public ServerId serverId;
    public long chirpTimestamp;
    public int numRemove;

    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public MsgRollback(ServerId serverId, long chirpTimestamp, int numRemove)
    {
        this.numRemove = numRemove;
        this.serverId = serverId;
        this.chirpTimestamp = chirpTimestamp;
    }
}

/* -------------------------------------------------------------------------- */
