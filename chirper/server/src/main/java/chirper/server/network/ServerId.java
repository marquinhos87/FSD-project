/* -------------------------------------------------------------------------- */

package chirper.server.network;

import java.util.Objects;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 *
 * Just a wrapper around an int. Exists for type safety.
 *
 * Instances of this class are immutable.
 */
public class ServerId implements Comparable< ServerId >
{
    private final int value;

    /**
     * TODO: document
     *
     * @param value TODO: document
     */
    public ServerId(int value)
    {
        this.value = value;
    }

    /**
     * TODO: document
     *
     * @return TODO: document
     */
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
