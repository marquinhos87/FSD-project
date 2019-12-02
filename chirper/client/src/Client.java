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
    private final Set< String > subscribedTopics;

    public Client(InetSocketAddress socketAddress)
    {
        this.subscribedTopics = new HashSet<>();
    }

    public void start()
    {

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
    }

    @Override
    public void close() throws Exception
    {
        // TODO: implement
    }
}

/* -------------------------------------------------------------------------- */
