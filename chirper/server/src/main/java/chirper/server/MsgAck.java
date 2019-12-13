/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

public class MsgAck
{
    public final long chirpTimestamp;

    public MsgAck(long chirpTimestamp)
    {
        this.chirpTimestamp = chirpTimestamp;
    }
}

/* -------------------------------------------------------------------------- */
