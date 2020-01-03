package chirper.server.broadcast;

import chirper.server.network.ServerId;

public class Commit {
    public final long twopc_id;
    public final ServerId serverId;
    // Commit Marker
    public Commit(ServerId serverId, long twopc_id) {
        this.serverId = serverId;
        this.twopc_id = twopc_id;
    }
}
