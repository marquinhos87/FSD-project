/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgAck
{
    public ServerId serverId;
    public long timestamp;

    /**
     * TODO: document
     *
     * @param timestamp TODO: document
     */
    public MsgAck(ServerId serverId, long timestamp)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.timestamp = timestamp;
    }
}

/* -------------------------------------------------------------------------- */
