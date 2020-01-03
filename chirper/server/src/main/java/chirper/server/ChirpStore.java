/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.server.network.ServerId;
import chirper.shared.Config;
import chirper.shared.Util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final Path stateFilePath;

    // maps topics to all chirps with that topic
    // (the same chirp can exist under more than one topic)
    private Map< String, SortedSet< Chirp > > chirpsByTopic;

    private long latestTimestamp;

    public ChirpStore(ServerId localServerId)
        throws IOException, ClassNotFoundException
    {
        this.stateFilePath = Path.of("state-" + localServerId.getValue() + ".dat");
        this.readState();
    }

    public void addChirp(Chirp chirp)
    {
        Util.getChirpTopicsStream(chirp.getText())
            .forEachOrdered(topic -> this.addChirpUnderTopic(topic, chirp));

        this.latestTimestamp = chirp.getTimestamp();
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
            set = new TreeSet<>();
            this.chirpsByTopic.put(topic, set);
        }

        // remove oldest chirp if at maximum capacity

        if (set.size() == Config.CHIRPS_PER_GET)
            set.remove(set.first());

        // add new chirp

        set.add(chirp);

        // persist state

        this.writeState();
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

    public long getLatestTimestamp()
    {
        return this.latestTimestamp;
    }

    private void readState() throws IOException, ClassNotFoundException
    {
        if (!Files.exists(stateFilePath))
        {
            this.chirpsByTopic = new HashMap<>();
            this.latestTimestamp = Long.MIN_VALUE;
        }
        else
        {
            try (
                final var f = new FileInputStream(stateFilePath.toFile());
                final var o = new ObjectInputStream(f)
            )
            {
                this.chirpsByTopic = (Map< String, SortedSet< Chirp > >)o.readObject();
                this.latestTimestamp = o.readLong();
            }
        }
    }

    private void writeState()
    {
        try
        {
            try (
                final var f = new FileOutputStream(this.stateFilePath.toFile());
                final var o = new ObjectOutputStream(f)
            )
            {
                o.writeObject(this.chirpsByTopic);
                o.writeLong(this.latestTimestamp);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

/* -------------------------------------------------------------------------- */
