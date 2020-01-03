/* -------------------------------------------------------------------------- */

package chirper.server.network;

import java.io.Serializable;
import java.util.Objects;

/* -------------------------------------------------------------------------- */

/**
 * An integer value that uniquely identifies a server.
 *
 * This class is a simple wrapper around an {@code int}, and exists for extra
 * type safety.
 *
 * Instances of this class are immutable and comparable.
 */
public class ServerId implements Comparable< ServerId >, Serializable
{
    private final int value;

    public ServerId(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return this.value;
    }

    @Override
    public boolean equals(Object obj)
    {
        return
            obj != null &&
            this.getClass() == obj.getClass() &&
            this.getValue() == ((ServerId)obj).getValue();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.getValue());
    }

    @Override
    public int compareTo(ServerId other)
    {
        return Integer.compare(this.getValue(), other.getValue());
    }
}

/* -------------------------------------------------------------------------- */
