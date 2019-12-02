/* -------------------------------------------------------------------------- */

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixConfig;
import io.atomix.storage.journal.Journal;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Client implements AutoCloseable
{
    private ManagedMessagingService ms;
    private Serializer s;
    private final Set< String > subscribedTopics;

    public Client(InetSocketAddress socketAddress)
    {
        this.ms = new NettyMessagingService(
                "servidor", new Address(socketAddress.getHostName(), socketAddress.getPort()),
                new MessagingConfig());

        this.s = new SerializerBuilder().addType(Msg.class).build();

        this.subscribedTopics = new HashSet<>();
    }

    public void start()
    {
        this.ms.start();
    }

    public Set< String > getSubscribedTopics()
    {
        return Collections.unmodifiableSet(this.subscribedTopics);
    }

    public void setSubscribedTopics(CharSequence[] topics)
    {
        this.setSubscribedTopics(List.of(topics));
    }

    public void setSubscribedTopics(Collection< ? extends CharSequence > topics)
    {
        // note: must validate new topics before modifying subscribed topic set

        final var newTopics =
            topics
            .stream()
            .map(Config::normalizeTopic)
            .collect(Collectors.toList());

        if (newTopics.isEmpty())
            throw new IllegalArgumentException("No topics specified.");

        this.subscribedTopics.clear();
        this.subscribedTopics.addAll(newTopics);

        // TODO: implement
    }

    public List< String > getLatestChirps()
    {
        return List.of(); // TODO: implement
    }

    public void publishChirp(CharSequence chirp)
    {
        if (!Config.chirpContainsTopics(chirp))
        {
            throw new IllegalArgumentException(
                "This chirp does not contain any topics."
            );
        }

        // TODO: implement

        Msg m = new Msg(1,1,chirp.toString(),new ArrayList<>());
        ms.sendAsync(Address.from("localhost",12345), "cliente", s.encode(m));
    }

    @Override
    public void close() throws Exception
    {
        // TODO: implement
    }
}

/* -------------------------------------------------------------------------- */
