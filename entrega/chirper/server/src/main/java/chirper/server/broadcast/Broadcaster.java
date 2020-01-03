package chirper.server.broadcast;

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
 * and exceptionally if the onMessageReceived callback throws an exception.
 *
 * Note that the future returned by put() only completes after the
 * onMessageReceived callback is run locally for the value passed to put().
 *
 * The onMessageReceived callback is called for any 2PC that commits, be it
 * coordinated by the local server or by another server. Also, the callback is
 * called for each committed value in the same order in all servers.
 *
 * @param <T> the type of things to be committed
 */
public abstract class Broadcaster<T>
{
    private final ServerNetwork serverNetwork;
    private final Consumer<T> onMessageTransmitted;

    protected Broadcaster(
        ServerNetwork serverNetwork,
        Consumer<T> onMessageTransmitted
    )
    {
        this.serverNetwork = Objects.requireNonNull(serverNetwork);
        this.onMessageTransmitted = Objects.requireNonNull(onMessageTransmitted);
    }

    public abstract void repeatTransactions();

    public abstract CompletableFuture< Boolean > broadcast(T value);

    protected ServerNetwork getServerNetwork()
    {
        return this.serverNetwork;
    }

    protected Consumer<T> getOnMessageTransmitted()
    {
        return this.onMessageTransmitted;
    }
}
