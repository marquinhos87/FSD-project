/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.server.state.Chirp;
import chirper.shared.Config;
import chirper.shared.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* -------------------------------------------------------------------------- */

public class ChirpStore
{
    // orders chirps by (server-id, timestamp) pairs
    private static final Comparator< Chirp > CHIRP_AGE_COMPARATOR =
        Comparator
            .comparingLong(Chirp::getTimestamp)
            .thenComparing(Chirp::getServerId);

    // maps topics to all chirps with that topic
    // (the same chirp can exist under more than one topic)
    private final Map< String, SortedSet< Chirp > > chirpsByTopic;

    public ChirpStore()
    {
        this.chirpsByTopic = new HashMap<>();
    }

    public void addChirp(Chirp chirp)
    {
        Util.getChirpTopicsStream(chirp.getText())
            .forEachOrdered(topic -> this.addChirpUnderTopic(topic, chirp));
    }

    private void addChirpUnderTopic(String topic, Chirp chirp)
    {
        // get chirp set for topic

        final SortedSet< Chirp > set;

        if (this.chirpsByTopic.containsKey(topic))
        {
            set = this.chirpsByTopic.get(topic);
        }
        else
        {
            set = new TreeSet<>(CHIRP_AGE_COMPARATOR);
            this.chirpsByTopic.put(topic, set);
        }

        // remove oldest chirp if at maximum capacity

        if (set.size() == Config.CHIRPS_PER_GET)
            set.remove(set.first());

        // add new chirp

        set.add(chirp);
    }

    public List< String > getLatestChirps(String[] topics)
    {
        return this.getLatestChirps(Arrays.asList(topics));
    }

    public List< String > getLatestChirps(Collection< String > topics)
    {
        // get stream of all chirp sets for requested topics
        // (no requested topics == requested all topics)

        final Stream< SortedSet< Chirp > > chirpSetStream;

        if (topics.isEmpty())
        {
            chirpSetStream = this.chirpsByTopic.values().stream();
        }
        else
        {
            chirpSetStream =
                topics
                    .stream()
                    .map(this.chirpsByTopic::get)
                    .filter(Objects::nonNull);
        }

        // get most recent chirps for requested topics (in reverse order)

        final var chirps =
            chirpSetStream
                .flatMap(SortedSet::stream)
                .sorted(CHIRP_AGE_COMPARATOR.reversed())
                .limit(Config.CHIRPS_PER_GET)
                .map(Chirp::getText)
                .collect(Collectors.toCollection(ArrayList::new)); // mutable

        // re-reverse chirps to get chronological order

        Collections.reverse(chirps);

        return chirps;
    }
}

/* -------------------------------------------------------------------------- */
