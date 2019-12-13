/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgChirp
{
    public final long timestamp;
    public final String text;

    /**
     * TODO: document
     *
     * @param timestamp TODO: document
     * @param text TODO: document
     */
    public MsgChirp(long timestamp, String text)
    {
        this.timestamp = timestamp;
        this.text = text;
    }
}

/* -------------------------------------------------------------------------- */
