/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgRollback extends Msg
{
    public int numRemove;

    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public MsgRollback(ServerId serverId, long chirpTimestamp, int numRemove)
    {
        super(serverId,chirpTimestamp);
        this.numRemove = numRemove;
    }
}

/* -------------------------------------------------------------------------- */
