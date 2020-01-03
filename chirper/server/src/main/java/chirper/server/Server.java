/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.server.state.Chirp;
import chirper.shared.Network;
import chirper.server.network.ServerId;
import chirper.server.network.ServerNetwork;
import chirper.server.broadcast.AllOrNothingOrderedBroadcaster;
import chirper.server.broadcast.Broadcaster;
import chirper.shared.Config;
import io.atomix.utils.net.Address;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/* -------------------------------------------------------------------------- */

public class Server implements AutoCloseable
{
    private final Network network;
    private final Broadcaster< Chirp > broadcaster;

    private final ChirpStore chirpStore;

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

        this.broadcaster = new AllOrNothingOrderedBroadcaster<>(
            serverNetwork,
            this::onChirpPublished,
            String.class
        );

        this.chirpStore = new ChirpStore();

        this.network.registerHandler(
            Config.CLIENT_MSG_TYPE_GET,
            this::clientGetHandler
        );

        this.network.registerHandler(
            Config.CLIENT_MSG_TYPE_PUBLISH,
            this::clientPublishHandler
        );
    }

    public void start() throws ExecutionException, InterruptedException
    {
        this.network.start();
        this.broadcaster.repeatTransactions();
    }

    @Override
    public void close() throws ExecutionException, InterruptedException
    {
        this.network.close();
    }

    private CompletableFuture< String[] > clientGetHandler(
        Address clientAddress,
        String[] topics
    )
    {
        final var result =
            this.chirpStore
                .getLatestChirps(topics)
                .toArray(String[]::new);

        return CompletableFuture.completedFuture(result);
    }

    private CompletableFuture< String > clientPublishHandler(
        Address clientAddress,
        String chirp
    )
    {
        return
            this.broadcaster
                .broadcast(chirp)
                .thenApply(success -> success ? null : "Error");
    }

    private void onChirpPublished(String chirp)
    {
        this.chirpStore.addChirp(chirp);
    }
}

/* -------------------------------------------------------------------------- */
