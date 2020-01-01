package chirper.server;

import java.util.Objects;

public class Msg {

    public ServerId serverId;
    public long timestamp;

    /**
     * TODO: document
     *
     * @param timestamp TODO: document
     */
    public Msg(ServerId serverId, long timestamp)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.timestamp = timestamp;
    }

}
