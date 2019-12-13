/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

public class MsgChirp
{
    public final long timestamp;
    public final String text;

    public MsgChirp(long timestamp, String text)
    {
        this.timestamp = timestamp;
        this.text = text;
    }
}

/* -------------------------------------------------------------------------- */
