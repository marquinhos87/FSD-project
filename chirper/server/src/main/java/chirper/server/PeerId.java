/* -------------------------------------------------------------------------- */

package chirper.server;

/* -------------------------------------------------------------------------- */

/**
 * Just a wrapper around an int. Exists for type safety.
 *
 * Instances of this class are immutable.
 */
public class PeerId implements Comparable< PeerId >
{
    private final int value;

    public PeerId(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return this.value;
    }

    @Override
    public int compareTo(PeerId other)
    {
        return Integer.compare(this.getValue(), other.getValue());
    }
}

/* -------------------------------------------------------------------------- */
