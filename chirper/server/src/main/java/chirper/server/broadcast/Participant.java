package chirper.server.broadcast;

import chirper.server.network.ServerId;

import java.util.concurrent.CompletableFuture;

public class Participant<T> {

    private Msg decision = null;

    private final Log log;

    public final T thing;

    public final CompletableFuture< Void > pendingDecision;

    public Participant(
        CompletableFuture< Void > pendingDecision,
        Log log,
        T chirp
    )
    {
        this.pendingDecision = pendingDecision;
        this.log = log;
        this.thing = chirp;

        log.add(this.thing);

        checkDecision();
    }

    public void prepare(ServerId serverId, long twopc_id) {

        log.add(new Prepared(serverId,twopc_id));
    }

    public void setDecision(Msg decision)
    {
        this.decision = decision;

        checkDecision();
    }

    public T getMsgChirp()
    {
        return this.thing;
    }

    public Msg getDecision()
    {
        return this.decision;
    }

    public void beginRollBack(ServerId serverId, long twopc_id)
    {
        // TODO
        this.log.add(new Abort(serverId,twopc_id));
    }

    public void commit(ServerId serverId, long twopc_id)
    {
        // TODO
        this.log.add(new Commit(serverId,twopc_id));
    }

    private void checkDecision()
    {
        if (this.decision != null)
        {
            this.pendingDecision.complete(null);
        }
    }
}
