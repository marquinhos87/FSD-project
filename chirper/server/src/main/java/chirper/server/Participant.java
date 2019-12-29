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

        log.add(this.chirp); // Insert chirp to commit into the participant log
        log.add(new Prepared()); // Insert prepared marker into the participant log

        checkDecision();
    }

    public void setDecision(Msg decision)
    {
        this.decision = decision;

        checkDecision();
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
        System.out.println("Decision: Commit");
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
