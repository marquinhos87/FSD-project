package chirper.server.broadcast;

import chirper.server.network.ServerId;
import chirper.server.network.ServerNetwork;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UnorderedBroadcaster<T> extends Broadcaster<T>
{
    public UnorderedBroadcaster(
        ServerNetwork serverNetwork,
        Class<T> messagePayloadClass,
        Consumer<T> onMessageReceived
    )
    {
        super(serverNetwork, onMessageReceived);

        serverNetwork.registerPayloadType(messagePayloadClass);
        serverNetwork.registerHandler("value", this::handleValue);
    }

    @Override
    public void repeatTransactions() {}

    @Override
    public CompletableFuture< Boolean > broadcast(T value)
    {
        this.getOnMessageReceived().accept(value);

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
        this.getOnMessageReceived().accept(value);
    }
}
