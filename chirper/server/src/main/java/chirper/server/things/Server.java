package chirper.server.things;

import chirper.server.ServerId;
import io.atomix.utils.net.Address;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Server implements AutoCloseable
{
    private final Network network;
    private final ServerNetwork serverNetwork;
    private final Replicator< String > replicator;

    private final ChirpStore chirpStore;

    public Server(
        ServerId localServerId,
        int localServerPort,
        Map< ServerId, Address > remoteServerAddressesById
    )
    {
        this.network = new Network(localServerPort);

        this.serverNetwork = new ServerNetwork(
            this.network,
            localServerId,
            remoteServerAddressesById
        );

        this.replicator = new BasicReplicator<>(
            this.serverNetwork,
            this::onChirpCommitted
        );

        this.chirpStore = new ChirpStore();
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

    private void onChirpCommitted(String chirp)
    {
        this.chirpStore.appendChirp(chirp);
    }
}
