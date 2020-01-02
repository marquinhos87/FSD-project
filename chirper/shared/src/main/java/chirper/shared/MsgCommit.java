/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgCommit extends Msg
{
    public ServerId serverId;
    public long id;

    /**
     * TODO: document
     */
    public MsgCommit(ServerId serverId, long id)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.id = id;
    }
}

/* -------------------------------------------------------------------------- */
