package chirper.shared;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TwoPhaseCommit<T> extends Replicator<T>
{
    public TwoPhaseCommit(
        ServerId localServerId,
        List< ServerIdAddress > remoteServerAddressesAndIds,
        Consumer<T> onValueCommitted
    )
    {
        super(localServerId, remoteServerAddressesAndIds, onValueCommitted);
    }

    @Override
    public CompletableFuture< Boolean > put(T value)
    {

    }
}
