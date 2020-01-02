package chirper.shared;

import java.util.Objects;

public class Msg {

    public ServerId serverId;
    public long id;

    /**
     * TODO: document
     *
     * @param timestamp TODO: document
     */
    public Msg(ServerId serverId, long twopc_id)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.id = twopc_id;
    }

}
