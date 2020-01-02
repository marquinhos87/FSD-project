/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgAck extends Msg
{
    /**
     * TODO: document
     *
     * @param twopc_id TODO: document
     */
    public MsgAck(ServerId serverId, long twopc_id)
    {
        super(serverId,twopc_id);
    }
}

/* -------------------------------------------------------------------------- */
