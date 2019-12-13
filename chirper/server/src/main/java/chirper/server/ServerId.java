/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

/**
 * Just a wrapper around an int. Exists for type safety.
 *
 * Instances of this class are immutable.
 */
public class ServerId implements Comparable< ServerId >
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
    public int compareTo(ServerId other)
    {
        return Integer.compare(this.getValue(), other.getValue());
    }
}

/* -------------------------------------------------------------------------- */
