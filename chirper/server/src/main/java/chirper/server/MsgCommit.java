/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgCommit extends Msg
{
    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public MsgCommit(ServerId serverId, long chirpTimestamp)
    {

        super(serverId,chirpTimestamp);
        /*this.serverId = Objects.requireNonNull(serverId);
        this.chirpTimestamp = chirpTimestamp;*/
    }
}

/* -------------------------------------------------------------------------- */
