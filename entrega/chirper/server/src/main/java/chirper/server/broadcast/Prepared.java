package chirper.server.broadcast;

import chirper.server.network.ServerId;

public class Prepared {
    public final long twopc_id;
    public final ServerId serverId;
    // Log Marker
    public Prepared(ServerId serverId, long twopc_id) {
        this.serverId = serverId;
        this.twopc_id = twopc_id;
    }
}
