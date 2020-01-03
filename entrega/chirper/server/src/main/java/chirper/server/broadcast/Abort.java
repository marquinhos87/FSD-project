package chirper.server.broadcast;

import chirper.server.network.ServerId;

public class Abort {
    public long twopc_id;
    public ServerId serverId;
    // Commit Marker
    public Abort(ServerId serverId, long twopc_id) {
        this.serverId = serverId;
        this.twopc_id = twopc_id;
    }
}
