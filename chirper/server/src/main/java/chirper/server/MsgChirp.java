/* -------------------------------------------------------------------------- */

package chirper.server;

import java.util.Objects;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class MsgChirp
{
    public ServerId serverId;
    public long timestamp;
    public String text;

    /**
     * TODO: document
     *
     * @param timestamp TODO: document
     * @param text TODO: document
     */
    public MsgChirp(ServerId serverId, long timestamp, String text)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.timestamp = timestamp;
        this.text = Objects.requireNonNull(text);
    }
}

/* -------------------------------------------------------------------------- */