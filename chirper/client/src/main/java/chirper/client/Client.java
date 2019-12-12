/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Util;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/* -------------------------------------------------------------------------- */

public class Client implements AutoCloseable
{
    // the address of the server this client is connected to
    private final Address serverAddress;

    // the messaging service
    private final ManagedMessagingService messaging;

    // the message encoder and decoder
    private final Serializer serializer;

    public Client(Address serverAddress)
    {
        this.serverAddress = serverAddress;

        this.messaging = new NettyMessagingService(
            "chirper", Address.local(), new MessagingConfig()
        );

        this.serializer = Serializer.builder().build();

//        final var executor = Executors.newFixedThreadPool(1);
//
//        this.messaging.registerHandler("publish-ack", this::handlePublishAck, executor);
//
//        this.address_server = new Address(socketAddress.getHostName(), socketAddress.getPort());
//
//        this.ms = new NettyMessagingService(
//                "servidor", new Address(socketAddress.getHostName(), 12345),
//                new MessagingConfig());
//
//        this.s = new SerializerBuilder().addType(Msg.class).build();
    }

    public void start()
    {
        this.messaging.start().join();
    }

    @Override
    public void close()
    {
        this.messaging.stop().join();
    }

    public void setSubscribedTopics(CharSequence[] topics)
        throws ExecutionException, InterruptedException
    {
        this.setSubscribedTopics(Arrays.asList(topics));
    }

    public void setSubscribedTopics(Collection< ? extends CharSequence > topics)
        throws ExecutionException, InterruptedException
    {
        // validate topics

        final var newTopics =
            topics
            .stream()
            .map(Util::normalizeTopic)
            .toArray(String[]::new);

        if (newTopics.length == 0)
            throw new IllegalArgumentException("must have at least one topic");

        // send subscribe request and await reply

        final var reqPayload = this.serializer.encode(newTopics);
        final var replyPayload = this.sendAndReceive("subscribe", reqPayload);

        // check if the server replied with an error

        final var errorMessage = this.serializer.< String >decode(replyPayload);

        if (!errorMessage.isEmpty())
            throw new IllegalArgumentException(errorMessage);

//        this.subscribedTopics.clear();
//        this.subscribedTopics.addAll(newTopics);
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("!sub ");
//        for(final var aux: this.subscribedTopics)
//            sb.append(aux).append(" ");
//        sendMsgAsync(sb.toString());
    }

    public List< String > getLatestChirps()
        throws ExecutionException, InterruptedException
    {
        // send get request and await reply

        final var replyPayload = this.sendAndReceive("get", new byte[0]);

        // decode and return the latest chirps

        return List.of(this.serializer.< String[] >decode(replyPayload));
    }

    public void publishChirp(CharSequence chirp)
        throws ExecutionException, InterruptedException
    {
        // validate chirp

        if (!Util.chirpContainsTopics(chirp))
        {
            throw new IllegalArgumentException(
                "This chirp does not contain any topics."
            );
        }

        // send publish request and await reply

        final var reqPayload = this.serializer.encode(chirp.toString());
        final var replyPayload = this.sendAndReceive("publish", reqPayload);

        // check if the server replied with an error

        final var errorMessage = this.serializer.< String >decode(replyPayload);

        if (!errorMessage.isEmpty())
            throw new IllegalArgumentException(errorMessage);
    }

    private byte[] sendAndReceive(String type, byte[] payload)
        throws ExecutionException, InterruptedException
    {
        return
            this
            .messaging
            .sendAndReceive(this.serverAddress, type, payload)
            .get();
    }

//    public void sendMsgAsync(CharSequence chirp)
//    {
//        Msg m = new Msg(chirp.toString());
//        ms.sendAsync(address_server, "cliente", s.encode(m));
//    }

//    public String sendMsgSync(CharSequence action) {
//        Msg m = new Msg(action.toString());
//        byte[] response = new byte[0];
//        try {
//            response = ms.sendAndReceive(address_server,"cliente",s.encode(m)).get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
//        return new String(response);
//    }
}

/* -------------------------------------------------------------------------- */
