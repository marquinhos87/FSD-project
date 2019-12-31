package chirper.server;

import java.util.Objects;

public class Msg {

    public ServerId serverId;
    public long timestamp;

    /**
     * TODO: document
     *
     * @param chirpTimestamp TODO: document
     */
    public Msg(ServerId serverId, long chirpTimestamp)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.timestamp = chirpTimestamp;
    }

}
