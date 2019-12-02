/* -------------------------------------------------------------------------- */

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Client implements AutoCloseable
{
    private final InetSocketAddress socketAddress;

    private final Set< String > subscribedTopics;

    public Client(InetSocketAddress socketAddress)
    {
        this.socketAddress = socketAddress;

        this.subscribedTopics = new HashSet<>();
    }

    public void start()
    {
        // TODO: implement
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

        this.subscribedTopics.clear();
        this.subscribedTopics.addAll(newTopics);
    }

    public List< String > getLatestChirps()
    {
        return List.of(); // TODO: implement
    }

    public void publishChirp(CharSequence chirp)
    {
        // TODO: implement
    }

    @Override
    public void close() throws Exception
    {
        // TODO: implement
    }
}

/* -------------------------------------------------------------------------- */
