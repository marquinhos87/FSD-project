/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgCom
{
    public ServerId serverId;
    public long chirpTimestamp;

    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public MsgCom(ServerId serverId, long chirpTimestamp)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.chirpTimestamp = chirpTimestamp;
    }
}

/* -------------------------------------------------------------------------- */
