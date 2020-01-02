/* -------------------------------------------------------------------------- */

package chirper.server.replicators;

/* -------------------------------------------------------------------------- */

import chirper.server.network.ServerId;

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
