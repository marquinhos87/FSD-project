package chirper.shared;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Implements 2PC for committing arbitrary objects, and thus handles all
 * inter-server communication.
 *
 * Call put() to run an instance of 2PC; the returned future completes
 * successfully if 2PC commits and exceptionally if 2PC aborts. But do not take
 * the value as committed when this future completes successfully!
 *
 * Wait for the onValueCommitted callback instead. This is called for any 2PC
 * that commits, be it coordinated by the local server or by another server.
 * Also, the callback is called for each committed value in the same order in
 * all servers.
 *
 * @param <T> the type of things to be committed
 */
public class TwoPhaseCommit<T>
{
    public TwoPhaseCommit(
        List< ServerIdAddress > peers,
        Consumer<T> onValueCommitted
    )
    {

    }

    public CompletableFuture< Void > put(T value)
    {

    }
}
