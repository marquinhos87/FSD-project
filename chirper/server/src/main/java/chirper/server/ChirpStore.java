/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import chirper.shared.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class ChirpStore
{
    // all chirps in reverse global total order
    private final LinkedList< String > chirps;

    public ChirpStore()
    {
        this.chirps = new LinkedList<>();
    }

    public void addChirp(String chirpText)
    {
        this.chirps.addFirst(chirpText);
    }

    public List< String > getLatestChirps(CharSequence[] topics)
    {
        return this.getLatestChirps(Arrays.asList(topics));
    }

    public List< String > getLatestChirps(
        Collection< ? extends CharSequence > topics
    )
    {
        // get matching chirps in reverse order

        var stream = this.chirps.stream();

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
}

/* -------------------------------------------------------------------------- */
