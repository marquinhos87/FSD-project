package chirper.server;

import chirper.shared.Config;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.utils.net.Address;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class Coordinator extends Log {

    private final int numRemoteServers;
    private final Set< ServerId > ackedServerIds;
    private final CompletableFuture< Void > onAllAcked;

    public Coordinator() {
        super(new CompletableFuture<>(), "nome");
        numRemoteServers = 1;
        ackedServerIds = new TreeSet<>();
        onAllAcked = new CompletableFuture<>();
    }

    public CompletableFuture<Void> prepared(byte[] payload, Set<Address> remoteServerAddresses, ManagedMessagingService messaging) {
        return CompletableFuture.allOf(
            remoteServerAddresses
                .stream()
                .map(
                    address -> messaging.sendAsync(
                        address, Config.SERVER_PREPARE_PUBLICATION_MSG_NAME, payload
                    )
                )
                .toArray(CompletableFuture[]::new)
        );
    }

    public CompletableFuture<Void> commited(Set<Address> remoteServerAddresses, ManagedMessagingService messaging) {
        return CompletableFuture.allOf(
            remoteServerAddresses
                .stream()
                .map(
                    address -> messaging.sendAsync(
                        address, Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, "".getBytes()
                    )
                )
                .toArray(CompletableFuture[]::new)
        );
    }
}
