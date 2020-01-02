package chirper.server.replicators;

import java.util.concurrent.CompletableFuture;

public class Participant<T> {

    private Msg decision = null;

    private final Log log;

    private final T thing;

    private final CompletableFuture< Void > pendingDecision;

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

    public void prepare() {

        log.add(new Prepared());
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

    public void beginRollBack()
    {
        // TODO
    }

    public void commit()
    {
        // TODO
        this.log.add(new Commit());
    }

    private void checkDecision()
    {
        if (this.decision != null)
        {
            this.pendingDecision.complete(null);
        }
    }
}
