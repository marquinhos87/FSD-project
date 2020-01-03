/* -------------------------------------------------------------------------- */

package chirper.shared;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/* -------------------------------------------------------------------------- */

public class Network implements AutoCloseable
{
    private final ManagedMessagingService messagingService;
    private final Executor executor;

    private final SerializerBuilder serializerBuilder;
    private Serializer serializer;

    public Network(int localPort)
    {
        this.messagingService = new NettyMessagingService(
            "chirper",
            Address.from(localPort),
            new MessagingConfig()
        );

        this.executor = Executors.newFixedThreadPool(1);

        this.serializerBuilder = Serializer.builder();
    }

    public void registerPayloadType(Class<?> payloadType)
    {
        this.serializerBuilder.addType(payloadType);
    }

    public <T> void registerHandler(
        String msgType,
        BiConsumer< Address, T > handler
    )
    {
        this.messagingService.registerHandler(
            msgType,
            (address, bytes) -> {
                handler.accept(address, this.serializer.decode(bytes));
                },
            this.executor
            );
    }

    public < T, U > void registerHandler(
        String msgType,
        BiFunction< Address, T, CompletableFuture<U> > handler
    )
    {
        this.messagingService.registerHandler(
            msgType,
            (address, bytes) ->
                handler
                    .apply(address, this.serializer.decode(bytes))
                    .thenApply(this.serializer::encode)
        );
    }

    public <T> CompletableFuture< Void > send(
        Address address,
        String msgType,
        T payload
    )
    {
        return this.messagingService.sendAsync(
            address,
            msgType,
            this.serializer.encode(payload)
        );
    }

    public < T, U > CompletableFuture<U> sendAndReceive(
        Address address,
        String msgType,
        T payload
    )
    {
        final var future = this.messagingService.sendAndReceive(
            address,
            msgType,
            this.serializer.encode(payload)
        );

        return future.thenApply(this.serializer::decode);
    }

    public void start() throws ExecutionException, InterruptedException
    {
        this.serializer = this.serializerBuilder.build();

        this.messagingService.start().get();
    }

    @Override
    public void close() throws ExecutionException, InterruptedException
    {
        this.messagingService.stop().get();
    }
}

/* -------------------------------------------------------------------------- */
