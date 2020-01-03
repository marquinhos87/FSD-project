/* -------------------------------------------------------------------------- */

package chirper.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Prompt
{
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "^\\s*!\\s*(?<command>\\w*)(?<args>\\s*.*)$"
    );

    private final Client client;

    private final BufferedReader in;
    private final PrintWriter out;

    public Prompt(Client client, BufferedReader in, PrintWriter out)
    {
        this.client = Objects.requireNonNull(client);

        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
    }

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
                this.printWarning("Exiting...");
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
                        this.printError("Command 'get' does not accept arguments.");
                    else
                        this.handleGet();

                    break;

                case "sub":
                case "subscribe":

                    this.handleSubscribe(args);

                    break;

                default:

                    this.printError(
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
            this.printError(e.getMessage());
            return;
        }

        // print chirps

        if (chirps.isEmpty())
        {
            this.printWarning(
                this.client.getSubscribedTopics().isEmpty()
                    ? "No chirps exist."
                    : "No chirps exist for any of your subscribed topics."
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
            this.printError(e.getMessage());
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
            this.printError(e.getMessage());
        }
    }

    private void printWarning(String text)
    {
        this.out.format("\033[33m%s\033[0m\n", text);
        this.out.flush();
    }

    private void printError(String text)
    {
        this.out.format("\033[31m%s\033[0m\n", text);
        this.out.flush();
    }
}

/* -------------------------------------------------------------------------- */
