/* -------------------------------------------------------------------------- */

package chirper.server;

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

    /**
     * TODO: document
     *
     * @param other TODO: document
     * @return TODO: document
     */
    @Override
    public int compareTo(ServerId other)
    {
        return Integer.compare(this.getValue(), other.getValue());
    }
}

/* -------------------------------------------------------------------------- */
