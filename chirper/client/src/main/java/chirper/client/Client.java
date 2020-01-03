/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Config;
import chirper.shared.Network;
import chirper.shared.Util;
import io.atomix.utils.net.Address;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Client implements AutoCloseable
{
    // the address of the server that this client is connected to
    private final Address serverAddress;

    // the network connecting client and server
    private final Network network;

    // the set of currently subscribed topics
    private final Set< String > subscribedTopics;

    private < T, U > U sendAndReceive(String type, T request)
        throws ExecutionException, InterruptedException
    {
        final CompletableFuture<U> future = this.network.sendAndReceive(
            this.serverAddress,
            type,
            request
        );

        return future.get();
    }

    public Client(Address serverAddress)
    {
        this.serverAddress = Objects.requireNonNull(serverAddress);
        this.network = new Network(0);

        this.subscribedTopics = new HashSet<>();
    }

    public void start() throws ExecutionException, InterruptedException
    {
        this.network.start();
    }

    @Override
    public void close() throws ExecutionException, InterruptedException
    {
        this.network.close();
    }

    public Set< String > getSubscribedTopics()
    {
        return Collections.unmodifiableSet(this.subscribedTopics);
    }

    public void setSubscribedTopics(Collection< ? extends CharSequence > topics)
    {
        // validate topics

        final var newTopics =
            topics
                .stream()
                .map(Util::normalizeTopic)
                .collect(Collectors.toSet());

        // modify set of subscribed topics

        this.subscribedTopics.clear();
        this.subscribedTopics.addAll(newTopics);
    }

    public List< String > getLatestChirps()
        throws ExecutionException, InterruptedException
    {
        // send get request and await reply

        final String[] chirps = this.sendAndReceive(
            Config.CLIENT_MSG_TYPE_GET,
            this.subscribedTopics.toArray(String[]::new)
        );

        // check if the server replied with an error

        if (chirps == null)
            throw new IllegalArgumentException();

        // return the latest chirps

        return Arrays.asList(chirps);
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

        final String error = this.sendAndReceive(
            Config.CLIENT_MSG_TYPE_PUBLISH,
            chirp.toString()
        );

        // check if the server replied with an error

        if (error != null)
            throw new IllegalArgumentException(error);
    }
}

/* -------------------------------------------------------------------------- */
