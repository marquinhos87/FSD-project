/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgRollback
{
    public ServerId serverId;
    public long timestamp;
    public int numRemove;

    /**
     * TODO: document
     *
     * @param timestamp TODO: document
     */
    public MsgRollback(ServerId serverId, long timestamp, int numRemove)
    {
        this.numRemove = numRemove;
        this.serverId = serverId;
        this.timestamp = timestamp;
    }
}

/* -------------------------------------------------------------------------- */
