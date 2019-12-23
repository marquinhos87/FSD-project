package chirper.server;

import java.util.concurrent.CompletableFuture;

public class Participant extends Log {
    public Participant() {
        super(new CompletableFuture<>(), "nome");
    }
}
