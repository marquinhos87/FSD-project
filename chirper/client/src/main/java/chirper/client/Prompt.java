/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class Prompt
{
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "^\\s*!\\s*(?<command>\\w*)\\s+(?<args>.*)"
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
     * @throws ExecutionException TODO: document
     * @throws IOException TODO: document
     * @throws InterruptedException TODO: document
     */
    public void inputLoop()
        throws ExecutionException, IOException, InterruptedException
    {
        while (true)
        {
            // print prompt

            this.out.print("> ");
            this.out.flush();

            // read line

            final var line = this.in.readLine();

            if (line == null)
                break; // no more input, exit input loop

            // handle line

            if (!line.isBlank())
                this.handleLine(line);
        }
    }

    private void handleLine(String line)
        throws ExecutionException, InterruptedException
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

    private void handleGet() throws ExecutionException, InterruptedException
    {
        if (this.client.getSubscribedTopics().isEmpty())
        {
            Util.printError(this.out, "You are not subscribed to any topics.");
        }
        else
        {
            final var chirps = this.client.getLatestChirps();

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
    }

    private void handleSubscribe(String topics)
    {
        try
        {
            this.client.setSubscribedTopics(topics.split("\\s+"));
        }
        catch (IllegalArgumentException e)
        {
            Util.printError(this.out, e.getMessage());
        }
    }

    private void handlePublish(String chirp)
        throws ExecutionException, InterruptedException
    {
        try
        {
            this.client.publishChirp(chirp);
        }
        catch (IllegalArgumentException e)
        {
            Util.printError(this.out, e.getMessage());
        }
    }

    private void handleLogin()
    {
        // TODO: implement
    }

    private void handleRegister()
    {
        // TODO: implement
    }
}

/* -------------------------------------------------------------------------- */
