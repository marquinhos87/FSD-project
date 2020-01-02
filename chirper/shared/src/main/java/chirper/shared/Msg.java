package chirper.shared;

import java.util.Objects;

public class Msg {

    public ServerId serverId;
    public long twopc_id;

    /**
     * TODO: document
     *
     * @param twopc_id TODO: document
     */
    public Msg(ServerId serverId, long twopc_id)
    {
        this.serverId = Objects.requireNonNull(serverId);
        this.twopc_id = twopc_id;
    }

}
