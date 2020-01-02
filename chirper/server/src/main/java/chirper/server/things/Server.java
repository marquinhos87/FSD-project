package chirper.server.things;

import chirper.server.ServerConfig;
import chirper.server.ServerId;
import chirper.server.network.Network;
import chirper.server.network.ServerNetwork;
import chirper.server.replicators.Replicator;
import chirper.server.replicators.UnorderedReplicator;
import chirper.shared.Config;
import io.atomix.utils.net.Address;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Server implements AutoCloseable
{
    private final Network network;
    private final Replicator< String > replicator;

    private final ChirpStore chirpStore;

    /**
     * TODO: document
     *
     * @param config TODO: document
     */
    public Server(ServerConfig config)
    {
        this(
            config.getLocalServerId(),
            config.getLocalServerPort(),
            config.getRemoteServerAddressesById()
        );
    }

    public Server(
        ServerId localServerId,
        int localServerPort,
        Map< ServerId, Address > remoteServerAddressesById
    )
    {
        this.network = new Network(localServerPort);

        final var serverNetwork = new ServerNetwork(
            this.network,
            localServerId,
            remoteServerAddressesById
        );

        this.replicator = new UnorderedReplicator<>(
            serverNetwork,
            String.class,
            this::onChirpCommitted
        );

        this.chirpStore = new ChirpStore();

        this.network.registerHandler(
            Config.CLIENT_GET_MSG_NAME,
            this::clientGetHandler
        );

        this.network.registerHandler(
            Config.CLIENT_PUBLISH_MSG_NAME,
            this::clientPublishHandler
        );
    }

    public void start() throws ExecutionException, InterruptedException
    {
        this.network.start();
    }

    @Override
    public void close() throws Exception
    {
        this.network.close();
    }

    private void clientGetHandler(Address clientAddress, String[] topics)
    {
        this.network.send(
            clientAddress,
            Config.CLIENT_GET_MSG_NAME,
            this.chirpStore.getLatestChirps(topics).toArray(String[]::new)
        );
    }

    private void clientPublishHandler(Address clientAddress, String chirp)
    {
        this.replicator
            .put(chirp)
            .thenApply(success -> success ? null : "Error")
            .thenAccept(
                error -> this.network.send(
                    clientAddress,
                    Config.CLIENT_PUBLISH_MSG_NAME,
                    error
                    )
            );
    }

    private void onChirpCommitted(String chirp)
    {
        this.chirpStore.addChirp(chirp);
    }
}
