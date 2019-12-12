/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Util;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;

/* -------------------------------------------------------------------------- */

public class Prompt
{
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "^\\s*!\\s*(?<command>\\w*)(?:\\s+(?<args>.*))?"
    );

    private final Client client;

    private final BufferedReader in;
    private final PrintWriter out;

    public Prompt(Client client, BufferedReader in, PrintWriter out)
    {
        this.client = client;

        this.in = in;
        this.out = out;
    }

    public void inputLoop() throws Exception
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

                    if (args != null)
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

                    if (args == null)
                    {
                        Util.printError(
                            this.out,
                            "Must specify one or more topics."
                        );
                    }
                    else
                    {
                        this.handleSubscribe(args);
                    }

                    break;

                case "":

                    Util.printError(
                        this.out,
                        "Empty command, must be 'get', 'sub', or 'subscribe'."
                    );

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
        if (this.client.getSubscribedTopics().isEmpty())
        {
            Util.printError(out, "You are not subscribed to any topics.");
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