package chirper.server;

import java.util.concurrent.CompletableFuture;

public class Participant extends Log {
    public Participant() {
        super(new CompletableFuture<>(), "nome");
    }

    public void add(Object o) {
         super.add(o);
    }

    public Object get() {
        return super.get();
    }
}
