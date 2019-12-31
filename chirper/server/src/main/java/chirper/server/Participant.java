package chirper.server;

import java.util.concurrent.CompletableFuture;

public class Participant {

    private Msg decision = null;

    private final Log log;

    private final MsgChirp chirp;

    private final CompletableFuture< Void > pendingDecision;

    public Participant(
        CompletableFuture< Void > pendingDecision,
        Log log,
        MsgChirp chirp
    )
    {
        this.pendingDecision = pendingDecision;
        this.log = log;
        this.chirp = chirp;

        log.add(this.chirp);

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

    public MsgChirp getMsgChirp()
    {
        return this.chirp;
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
