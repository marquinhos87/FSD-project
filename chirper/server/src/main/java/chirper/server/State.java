/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import chirper.shared.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

/**
 * TODO: rename
 * TODO: document
 */
public class State
{
    // all chirps in reverse global total order
    private final SortedSet< Chirp > chirps;

    /**
     * TODO: document
     */
    public State()
    {
        this.chirps = new TreeSet<>(
            Comparator
                .comparing(Chirp::getPeerId)
                .thenComparingLong(Chirp::getTimestamp)
                .reversed()
        );
    }

    /**
     * TODO: document
     *
     * Thread-safety: Safe to call at any time from any context.
     *
     * @param publishingPeerId the identifier of the publishing peer
     * @param publisherChirpTimestamp the timestamp of the chirp, according
     *     to the publishing peer
     * @param chirpText the context of the chirp
     */
    public void addChirp(
        PeerId publishingPeerId,
        long publisherChirpTimestamp,
        String chirpText
    )
    {
        this.chirps.add(
            new Chirp(publishingPeerId, publisherChirpTimestamp, chirpText)
        );
    }

    /**
     * TODO: document
     *
     * @param topics TODO: document
     *
     * @return TODO: document
     */
    /**
     * Gets the most recent chirps with the given topics.
     *
     * Only up to {@param maxChirps} are returned in the list.
     *
     * The list is unmodifiable.
     *
     * Thread-safety: Safe to call at any time from any context.
     *
     * @param topics
     * @param maxChirps
     *
     * @return
     */
    public List< String > getLatestChirps(CharSequence[] topics)
    {
        return this.getLatestChirps(Arrays.asList(topics));
    }

    /**
     * TODO: document
     *
     * If topics is empty, then all chirps are considered.
     *
     * Thread-safety: Safe to call at any time from any context.
     *
     * @param topics TODO: document
     *
     * @return TODO: document
     */
    public List< String > getLatestChirps(
        Collection< ? extends CharSequence > topics
    )
    {
        // get matching chirps in reverse order

        var stream = this.chirps.stream().map(Chirp::getText);

        if (!topics.isEmpty())
        {
            stream = stream.filter(
                c -> !Collections.disjoint(Util.getChirpTopics(c), topics)
            );
        }

        final var latestChirps =
            stream
                .limit(Config.CHIRPS_PER_GET)
                .collect(Collectors.toCollection(ArrayList::new));

        // reverse chirps back to original order

        Collections.reverse(latestChirps);

        // return chirps

        return Collections.unmodifiableList(latestChirps);
    }

    private static class Chirp
    {
        private final PeerId peerId;
        private final long timestamp;
        private final String text;

        public Chirp(PeerId peerId, long timestamp, String text)
        {
            this.peerId = peerId;
            this.timestamp = timestamp;
            this.text = text;
        }

        public PeerId getPeerId()
        {
            return this.peerId;
        }

        public long getTimestamp()
        {
            return this.timestamp;
        }

        public String getText()
        {
            return this.text;
        }
    }
}

/* -------------------------------------------------------------------------- */
