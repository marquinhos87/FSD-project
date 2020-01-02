package chirper.shared;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Just a simple persistent chirp store.
 *
 * Add chirps with appendChirp(). The chirps end up having the same order as the
 * order in which appendChirp() is invoked.
 */
public class ChirpStore
{
    public ChirpStore()
    {

    }

    public List< String > getLatestChirps(CharSequence[] topics)
    {
        return this.getLatestChirps(Arrays.asList(topics));
    }

    public List< String > getLatestChirps(
        Collection< ? extends CharSequence > topics
    )
    {

    }

    public void appendChirp(String chirp)
    {

    }
}
