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
    public static final int CHIRPS_PER_GET = 3;

    /**
     * TODO: document
     *
     * Sent from client to server and vice-versa.
     */
    public static final String CLIENT_GET_MSG_NAME = "c-get";

    /**
     * TODO: document
     *
     * Sent from client to server and vice-versa.
     */
    public static final String CLIENT_PUBLISH_MSG_NAME = "c-publish";

    /**
     * TODO: document
     *
     * Sent from server to server.
     */
    public static final String SERVER_PUBLISH_MSG_NAME = "s-publish";

    /**
     * TODO: document
     *
     * Sent from server to server.
     */
    public static final String SERVER_ACK_PUBLICATION_MSG_NAME = "s-ack";

    /**
     * TODO: document
     *
     * Sent from server to server.
     */
    public static final String SERVER_PREPARE_PUBLICATION_MSG_NAME = "s-prepare";

    /**
     * TODO: document
     *
     * Sent from server to server.
     */
    public static final String SERVER_COMMIT_PUBLICATION_MSG_NAME = "s-commit";

    /**
     * TODO: document
     *
     * Sent from server to server.
     */
    public static final String SERVER_ROLLBACK_PUBLICATION_MSG_NAME = "s-abort";

    /**
     * TODO: document
     *
     * Sent from server to server.
     */
    public static final String SERVER_VOTE_OK_MSG_NAME = "s-ok";

    private Config()
    {
    }
}

/* -------------------------------------------------------------------------- */
