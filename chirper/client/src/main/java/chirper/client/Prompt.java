/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class Prompt
{
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "^\\s*!\\s*(?<command>\\w*)(?<args>\\s*.*)$"
    );

    // TODO: document
    private final Client client;

    // TODO: document
    private final BufferedReader in;

    // TODO: document
    private final PrintWriter out;

    /**
     * TODO: document
     *
     * @param client TODO: document
     * @param in TODO: document
     * @param out TODO: document
     */
    public Prompt(Client client, BufferedReader in, PrintWriter out)
    {
        this.client = client;

        this.in = in;
        this.out = out;
    }

    /**
     * TODO: document
     *
     * @throws IOException TODO: document
     */
    public void inputLoop() throws IOException
    {
        while (true)
        {
            // print prompt

            this.out.print("> ");
            this.out.flush();

            // read line

            final var line = this.in.readLine();

            if (line == null)
            {
                // no more input, exit input loop
                Util.printWarning(this.out, "Exiting...");
                break;
            }

            // handle line

            if (!line.isBlank())
                this.handleLine(line);
        }
    }

    private void handleLine(String line)
    {
        final var commandMatcher = COMMAND_PATTERN.matcher(line);

        if (commandMatcher.matches())
        {
            // treat line as command

            final var command = commandMatcher.group("command");
            final var args = commandMatcher.group("args");

            switch (command)
            {
                case "get":

                    if (!args.isBlank())
                    {
                        Util.printError(
                            this.out,
                            "Command 'get' does not accept arguments."
                        );
                    }
                    else
                    {
                        this.handleGet();
                    }

                    break;

                case "sub":
                case "subscribe":

                    this.handleSubscribe(args);

                    break;

                default:

                    Util.printError(
                        this.out,
                        "Unknown command, must be 'get', 'sub', or 'subscribe'."
                    );

                    break;
            }
        }
        else
        {
            // treat line as chirp

            this.handlePublish(line.strip());
        }
    }

    private void handleGet()
    {
        // get chirps

        final List< String > chirps;

        try
        {
            chirps = this.client.getLatestChirps();
        }
        catch (Exception e)
        {
            Util.printError(this.out, e.getMessage());
            return;
        }

        // print chirps

        if (chirps.isEmpty())
        {
            Util.printWarning(
                this.out,
                "No chirps exist for any of your subscribed topics."
            );
        }
        else
        {
            for (final var chirp : chirps)
                this.out.println(chirp);

            this.out.flush();
        }
    }

    private void handleSubscribe(String topics)
    {
        try
        {
            this.client.setSubscribedTopics(
                Pattern
                    .compile("\\s+")
                    .splitAsStream(topics)
                    .filter(t -> !t.isBlank())
                    .collect(Collectors.toList())
            );
        }
        catch (IllegalArgumentException e)
        {
            Util.printError(this.out, e.getMessage());
        }
    }

    private void handlePublish(String chirp)
    {
        try
        {
            this.client.publishChirp(chirp);
        }
        catch (Exception e)
        {
            Util.printError(this.out, e.getMessage());
        }
    }

//    private void handleLogin()
//    {
//        // TODO: implement
//    }
//
//    private void handleRegister()
//    {
//        // TODO: implement
//    }
}

/* -------------------------------------------------------------------------- */
