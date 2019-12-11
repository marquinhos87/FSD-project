/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Msg;
import chirper.shared.Util;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Client implements AutoCloseable
{
    private ManagedMessagingService ms;
    private Serializer s;
    private final Set< String > subscribedTopics;
    private Address address_server;

    public Client(InetSocketAddress socketAddress)
    {
        this.address_server = new Address(socketAddress.getHostName(), socketAddress.getPort());

        this.ms = new NettyMessagingService(
                "servidor", new Address(socketAddress.getHostName(), 12345),
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
            .map(Util::normalizeTopic)
            .collect(Collectors.toList());

        if (newTopics.isEmpty())
            throw new IllegalArgumentException("No topics specified.");

        this.subscribedTopics.clear();
        this.subscribedTopics.addAll(newTopics);

        StringBuilder sb = new StringBuilder();
        sb.append("!sub ");
        for(final var aux: this.subscribedTopics)
            sb.append(aux).append(" ");
        sendMsgAsync(sb.toString());

        // TODO: implement
    }

    public List< String > getLatestChirps() {
        final var chirps = sendMsgSync("!get");

        return List.of(chirps.split("\n")); // TODO: implement
    }

    public void publishChirp(CharSequence chirp)
    {
        if (!Util.chirpContainsTopics(chirp))
        {
            throw new IllegalArgumentException(
                "This chirp does not contain any topics."
            );
        }

        // TODO: implement
        sendMsgAsync(chirp);

    }

    public void sendMsgAsync(CharSequence chirp)
    {
        Msg m = new Msg(chirp.toString());
        ms.sendAsync(address_server, "cliente", s.encode(m));
    }

    public String sendMsgSync(CharSequence action) {
        Msg m = new Msg(action.toString());
        byte[] response = new byte[0];
        try {
            response = ms.sendAndReceive(address_server,"cliente",s.encode(m)).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new String(response);
    }

    @Override
    public void close() throws Exception
    {
        // TODO: implement
    }
}

/* -------------------------------------------------------------------------- */
