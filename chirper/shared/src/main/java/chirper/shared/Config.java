/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

public class Config
{
    /**
     * TODO: document
     *
     * Just something necessary that must be the same in client and server. We
     * don't really use it for anything fancy.
     */
    public static final String NETTY_CLUSTER_NAME = "chirper";

    /**
     * TODO: document
     */
    public static final int MAX_CHIRPS_PER_TOPIC = 10;

    private Config()
    {
    }
}

/* -------------------------------------------------------------------------- */
