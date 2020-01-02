package chirper.server.things;

import chirper.server.ServerId;
import io.atomix.utils.net.Address;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Handler;

public class BasicReplicator<T> extends Replicator<T>
{
    protected BasicReplicator(
        ServerNetwork serverNetwork,
        Consumer<T> onValueCommitted
    )
    {
        super(serverNetwork, onValueCommitted);

        serverNetwork.registerHandler("value", this::handleValue);
        serverNetwork.registerHandler("ack", this::handleAck);
    }

    @Override
    public abstract CompletableFuture< Boolean > put(T value)
    {

    }

    private void handleValue(ServerId serverId, T value)
    {
        final var value = seriazlier.decode<T>(payload);

        communicator.send("ack", serverAddress,)
    }

    private void handleAck(ServerId serverId, Void payload)
    {
        ???
    }
}
