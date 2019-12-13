/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgChirp
{
    public long timestamp;
    public String text;

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
