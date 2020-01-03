/* -------------------------------------------------------------------------- */

package chirper.shared;

/* -------------------------------------------------------------------------- */

public class Config
{
    /**
     * The maximum number of chirps that a "get" command retrieves.
     */
    public static final int CHIRPS_PER_GET = 10;

    /**
     * "Type" of messages representing a "get" command, sent from clients to
     * servers.
     */
    public static final String CLIENT_MSG_TYPE_GET = "c-get";

    /**
     * "Type" of messages representing a "publish" command, sent from clients to
     * servers.
     */
    public static final String CLIENT_MSG_TYPE_PUBLISH = "c-publish";

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
