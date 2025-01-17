/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Network;
import chirper.server.network.ServerId;
import chirper.server.network.ServerNetwork;
import chirper.server.broadcast.AllOrNothingBroadcaster;
import chirper.server.broadcast.Broadcaster;
import chirper.shared.Config;
import io.atomix.utils.net.Address;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/* -------------------------------------------------------------------------- */

public class Server implements AutoCloseable
{
    private final ServerId localServerId;
    private final Network network;
    private final Broadcaster< Chirp > broadcaster;

    private long nextChirpTimestamp;
    private final ChirpStore chirpStore;

    public Server(ServerConfig config)
        throws IOException, ClassNotFoundException
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
    ) throws IOException, ClassNotFoundException
    {
        this.localServerId = localServerId;
        this.network = new Network(localServerPort);

        final var serverNetwork = new ServerNetwork(
            this.network,
            localServerId,
            remoteServerAddressesById
        );

        this.broadcaster = new AllOrNothingBroadcaster<>(
            serverNetwork,
            this::onChirpPublished,
            Chirp.class
        );

        this.chirpStore = new ChirpStore(localServerId);
        this.nextChirpTimestamp = this.chirpStore.getLatestTimestamp() + 1;

        // register message handlers

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
        String chirpText
    )
    {
        final var chirp = new Chirp(
            this.localServerId,
            this.nextChirpTimestamp++,
            chirpText
        );

        return
            this.broadcaster
                .broadcast(chirp)
                .thenApply(success -> success ? null : "Error");
    }

    private void onChirpPublished(Chirp chirp)
    {
        this.nextChirpTimestamp = 1 + Math.max(
            this.nextChirpTimestamp,
            chirp.getTimestamp()
        );

        this.chirpStore.addChirp(chirp);
    }
}

/* -------------------------------------------------------------------------- */
