/* -------------------------------------------------------------------------- */

package chirper.server.state;

import chirper.server.network.ServerId;

import java.io.Serializable;
import java.util.Objects;

/* -------------------------------------------------------------------------- */

public class Chirp implements Serializable
{
    private final ServerId serverId;
    private final long timestamp;
    private final String text;

    public Chirp(ServerId serverId, long timestamp, String text)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.timestamp = timestamp;
        this.text = Objects.requireNonNull(text);
    }

    public ServerId getServerId()
    {
        return this.serverId;
    }

    public long getTimestamp()
    {
        return this.timestamp;
    }

    public String getText()
    {
        return this.text;
    }
}

/* -------------------------------------------------------------------------- */
