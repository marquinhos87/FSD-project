package chirper.server.replicators;

import chirper.server.network.ServerNetwork;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Implements 2PC for committing arbitrary objects, and thus handles all
 * inter-server communication.
 *
 * Call put() to run an instance of 2PC; the returned future completes
 * successfully with true if 2PC commits, successfully with false if 2PC aborts,
 * and exceptionally if the onValueCommitted callback throws an exception.
 *
 * Note that the future returned by put() only completes after the
 * onValueCommitted callback is run locally for the value passed to put().
 *
 * The onValueCommitted callback is called for any 2PC that commits, be it
 * coordinated by the local server or by another server. Also, the callback is
 * called for each committed value in the same order in all servers.
 *
 * @param <T> the type of things to be committed
 */
public abstract class Replicator<T>
{
    private final ServerNetwork serverNetwork;
    private final Consumer<T> onValueCommitted;

    protected Replicator(
        ServerNetwork serverNetwork,
        Consumer<T> onValueCommitted
    )
    {
        this.serverNetwork = Objects.requireNonNull(serverNetwork);
        this.onValueCommitted = Objects.requireNonNull(onValueCommitted);
    }

    public abstract CompletableFuture< Boolean > put(T value);

    protected ServerNetwork getServerNetwork()
    {
        return this.serverNetwork;
    }

    protected Consumer<T> getOnValueCommitted()
    {
        return this.onValueCommitted;
    }
}
