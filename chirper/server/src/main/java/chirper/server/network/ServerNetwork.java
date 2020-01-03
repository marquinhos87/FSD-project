package chirper.server.network;

import chirper.shared.Network;
import io.atomix.utils.net.Address;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ServerNetwork
{
    private final Network underlyingNetwork;
    private final ServerId localServerId;
    private final Map< ServerId, Address > remoteServerAddressesById;

    public ServerNetwork(
        Network underlyingNetwork,
        ServerId localServerId,
        Map< ServerId, Address > remoteServerAddressesById
    )
    {
        this.underlyingNetwork = underlyingNetwork;
        this.localServerId = localServerId;
        this.remoteServerAddressesById = new HashMap<>(remoteServerAddressesById);

        this.underlyingNetwork.registerPayloadType(TaggedPayload.class);
    }

    public ServerId getLocalServerId()
    {
        return this.localServerId;
    }

    public Set< ServerId > getRemoteServerIds()
    {
        return Collections.unmodifiableSet(
            this.remoteServerAddressesById.keySet()
        );
    }

    public void registerPayloadType(Class<?> payloadType)
    {
        this.underlyingNetwork.registerPayloadType(payloadType);
    }

    public <T> void registerHandler(
        String msgType,
        BiConsumer< ServerId, T > handler
    )
    {
        this.underlyingNetwork.< TaggedPayload<T> >registerHandler(
            msgType,
            (address, taggedPayload) -> handler.accept(
                taggedPayload.sourceServerId,
                taggedPayload.payload
            )
        );
    }

    public <T> CompletableFuture< Void > send(
        ServerId serverId,
        String msgType,
        T payload
    )
    {
        return this.underlyingNetwork.send(
            this.remoteServerAddressesById.get(serverId),
            msgType,
            new TaggedPayload<>(this.localServerId, payload)
        );
    }

    private static class TaggedPayload<T>
    {
        public final ServerId sourceServerId;
        public final T payload;

        public TaggedPayload(ServerId sourceServerId, T payload)
        {
            this.sourceServerId = sourceServerId;
            this.payload = payload;
        }
    }
}
