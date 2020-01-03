/* -------------------------------------------------------------------------- */

package chirper.shared;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* -------------------------------------------------------------------------- */

public class Util
{
    // chirps

    private static final Pattern TOPIC_PATTERN = Pattern.compile(
        "#?(\\p{IsAlphabetic}+)"
    );

    private static final Pattern INLINE_TOPIC_PATTERN = Pattern.compile(
        "(?:^|\\P{IsAlphabetic})#(\\p{IsAlphabetic}+)"
    );

    public static String normalizeTopic(CharSequence topic)
    {
        final var matcher = TOPIC_PATTERN.matcher(topic);

        if (!matcher.matches())
        {
            throw new IllegalArgumentException(
                String.format("Invalid topic '%s'.", topic)
            );
        }

        return matcher.group(1);
    }

    public static boolean chirpContainsTopics(CharSequence chirp)
    {
        return INLINE_TOPIC_PATTERN.matcher(chirp).find();
    }

    public static Stream< String > getChirpTopicsStream(CharSequence chirp)
    {
        return
            INLINE_TOPIC_PATTERN
                .matcher(chirp)
                .results()
                .map(mr -> mr.group(1));
    }

    public static Set< String > getChirpTopics(CharSequence chirp)
    {
        return
            getChirpTopicsStream(chirp)
                .collect(Collectors.toUnmodifiableSet());
    }
}

/* -------------------------------------------------------------------------- */
