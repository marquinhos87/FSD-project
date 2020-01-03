/* -------------------------------------------------------------------------- */

package chirper.server.state;

import chirper.server.network.ServerId;

import java.io.Serializable;
import java.util.Objects;

/* -------------------------------------------------------------------------- */

public class Chirp implements Serializable, Comparable< Chirp >
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

    @Override
    public boolean equals(Object obj)
    {
        return
            obj != null &&
                this.getClass() == obj.getClass() &&
                this.getTimestamp() == ((Chirp)obj).getTimestamp() &&
                this.getServerId() == ((Chirp)obj).getServerId();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.getTimestamp(), this.getServerId());
    }

    @Override
    public int compareTo(Chirp other)
    {
        final var i = Long.compare(this.getTimestamp(), other.getTimestamp());

        if (i != 0)
            return i;
        else
            return this.getServerId().compareTo(other.getServerId());
    }
}

/* -------------------------------------------------------------------------- */
