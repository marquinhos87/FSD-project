/* -------------------------------------------------------------------------- */

package chirper.server.broadcast;

/* -------------------------------------------------------------------------- */

import chirper.server.network.ServerId;

/**
 * TODO: document
 */
public class MsgCommit extends Msg
{
    //public ServerId serverId;
    //public long id;

    /**
     * TODO: document
     */
    public MsgCommit(ServerId serverId, long id)
    {
        super(serverId,id);
        //this.serverId = Objects.requireNonNull(serverId);
        //this.id = id;
    }
}

/* -------------------------------------------------------------------------- */
