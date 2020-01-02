package chirper.server.replicators;

import chirper.server.network.ServerId;
import chirper.server.network.ServerNetwork;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UnorderedReplicator<T> extends Replicator<T>
{
    public UnorderedReplicator(
        ServerNetwork serverNetwork,
        Class<T> valueClass,
        Consumer<T> onValueCommitted
    )
    {
        super(serverNetwork, onValueCommitted);

        serverNetwork.registerPayloadType(valueClass);
        serverNetwork.registerHandler("value", this::handleValue);
    }

    @Override
    public CompletableFuture< Boolean > put(T value)
    {
        this.getOnValueCommitted().accept(value);

        // send value to all remote servers

        final var serverNetwork = this.getServerNetwork();

        final var futures =
            serverNetwork
            .getRemoteServerIds()
            .stream()
            .map(serverId -> serverNetwork.send(serverId, "value", value))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenApply(v -> true);
    }

    private void handleValue(ServerId serverId, T value)
    {
        this.getOnValueCommitted().accept(value);
    }
}
