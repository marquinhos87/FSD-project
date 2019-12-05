/* -------------------------------------------------------------------------- */

import java.nio.charset.Charset;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Config
{
    private Config()
    {
    }

    // network

    public static final int DEFAULT_CLIENT_PORT = 7777;

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

    public static Set< String > getChirpTopics(CharSequence chirp)
    {
        return
            INLINE_TOPIC_PATTERN
                .matcher(chirp)
                .results()
                .map(mr -> mr.group(1))
                .collect(Collectors.toUnmodifiableSet());
    }

}

/* -------------------------------------------------------------------------- */
