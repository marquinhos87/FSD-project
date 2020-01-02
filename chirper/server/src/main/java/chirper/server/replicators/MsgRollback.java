/* -------------------------------------------------------------------------- */

package chirper.server.replicators;

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
     * @param twopc_id TODO: document
     */
    public MsgRollback(ServerId serverId, long twopc_id, int numRemove)
    {
        super(serverId,twopc_id);
        this.numRemove = numRemove;
    }
}

/* -------------------------------------------------------------------------- */
