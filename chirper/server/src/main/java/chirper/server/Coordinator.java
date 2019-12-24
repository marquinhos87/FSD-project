package chirper.server;

import chirper.shared.Config;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.utils.net.Address;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class Coordinator {

    private final Log log;
    private final Set< ServerId > ackedServerIds;
    private final CompletableFuture< Void > onAllAcked;

    public Coordinator(int id) {
        this.ackedServerIds = new TreeSet<>();
        this.onAllAcked = new CompletableFuture<>();
        this.log = new Log(id);
    }

    public void add(Object o) {
        log.add(o);
    }

    public Object get() {
        return log.get();
    }

    public void remove(int i) {
        log.remove(i);
    }

    public CompletableFuture<Void> prepared(byte[] payload, Set<Address> remoteServerAddresses, ManagedMessagingService messaging) {
        return CompletableFuture.allOf(
            remoteServerAddresses
                .stream()
                .map(
                    address -> messaging.sendAndReceive(
                                address, Config.SERVER_PREPARE_PUBLICATION_MSG_NAME, payload
                            )
                )
                .toArray(CompletableFuture[]::new)
        );
    }

    public CompletableFuture<Void> commited(byte[] payload, Set<Address> remoteServerAddresses, ManagedMessagingService messaging) {
        return CompletableFuture.allOf(
            remoteServerAddresses
                .stream()
                .map(
                    address -> messaging.sendAndReceive(
                                address, Config.SERVER_COMMIT_PUBLICATION_MSG_NAME, payload
                            )
                )
                .toArray(CompletableFuture[]::new)
        );
    }

    public CompletableFuture<Void> rollback(byte[] payload, Set<Address> remoteServerAddresses, ManagedMessagingService messaging) {
        return CompletableFuture.allOf(
            remoteServerAddresses
                .stream()
                .map(
                    address -> messaging.sendAndReceive(
                        address, Config.SERVER_ROLLBACK_PUBLICATION_MSG_NAME, payload
                    )
                )
                .toArray(CompletableFuture[]::new)
        );
    }
}
