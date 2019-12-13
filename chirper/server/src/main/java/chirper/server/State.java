/* -------------------------------------------------------------------------- */

package chirper.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/* -------------------------------------------------------------------------- */

public class State
{
    /**
     * TODO: document
     */
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

    // TODO: document
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

    }

    /**
     * TODO: document
     *
     * @param topics TODO: document
     *
     * @return TODO: document
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

    }
}

/* -------------------------------------------------------------------------- */
