/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

import java.util.Objects;

/**
 * TODO: document
 */
public class MsgAck extends Msg
{
    //public ServerId serverId;
    //public long chirpTimestamp;

    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public MsgAck(ServerId serverId, long chirpTimestamp)
    {
        super(serverId,chirpTimestamp);
        //this.serverId = Objects.requireNonNull(serverId);
        //this.chirpTimestamp = chirpTimestamp;
    }
}

/* -------------------------------------------------------------------------- */
