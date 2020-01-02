package chirper.server.replicators;

import chirper.server.network.ServerId;

public class MsgPrepare<T> extends Msg
{
    public T content;

    public MsgPrepare(ServerId serverId, long twopc_id, T content)
    {
        super(serverId,twopc_id);
        this.content = content;
    }
}
