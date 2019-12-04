/* -------------------------------------------------------------------------- */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/* -------------------------------------------------------------------------- */

public class Main
{
    public static void main(String[] args) throws IOException
    {
        try (
            final var in = new BufferedReader(new InputStreamReader(System.in));
            final var out = new PrintWriter(new OutputStreamWriter(System.out))
        )
        {
            try
            {
                // check usage and parse arguments

                final var socketAddress = parseArgs(args);

                if (socketAddress.isEmpty())
                {
                    out.println("Usage: chirper <host> [<port>]");
                    out.flush();
                    System.exit(2);
                }

                // run client

                runClient(in, out, socketAddress.get());
            }
            catch (Exception e)
            {
                printError(out, e.getMessage());
                System.exit(1);
            }
        }
    }

    private static Optional< InetSocketAddress > parseArgs(String[] args)
    {
        int port;

        if (args.length == 1)
        {
            port = Config.DEFAULT_CLIENT_PORT;
        }
        else if (args.length == 2)
        {
            try
            {
                port = Integer.parseUnsignedInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Invalid port number.");
            }
        }
        else
        {
            return Optional.empty();
        }

        return Optional.of(new InetSocketAddress(args[0], port));
    }

    private static void runClient(
        BufferedReader in,
        PrintWriter out,
        InetSocketAddress socketAddress
    ) throws Exception
    {
        try (final var client = new Client(socketAddress))
        {
            client.start();

            // input loop

            while (true)
            {
                out.print("> ");
                out.flush();

                final var line = in.readLine();

                if (line == null)
                    break; // no more input, terminate

                handleLine(client, line, out);
            }
        }
    }

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "^\\s*!\\s*(?<command>\\w+)(?:\\s+(?<args>.*))?"
    );

    private static void handleLine(Client client, String line, PrintWriter out) throws ExecutionException, InterruptedException {
        if (line.isBlank())
            return;

        final var matcher = COMMAND_PATTERN.matcher(line);

        if (matcher.matches())
        {
            // treat line as command

            switch (matcher.group("command"))
            {
                case "get":

                    if (matcher.group("args") != null)
                    {
                        printError(
                            out,
                            "Command 'get' does not accept arguments."
                        );
                    }
                    else
                    {
                        handleGet(client, out);
                    }

                    break;

                case "sub":
                case "subscribe":

                    handleSubscribe(client, matcher.group("args"), out);

                    break;

                default:

                    printError(
                        out,
                        "Unknown command, must be 'get', 'sub', or 'subscribe'."
                    );

                    break;
            }
        }
        else
        {
            // treat line as chirp

            handlePublish(client, line, out);
        }
    }

    private static void handleLogin()
    {
        //TODO : implement
    }

    private static void handleRegister()
    {
        //TODO : implement
    }

    private static void handleGet(Client client, PrintWriter out) throws ExecutionException, InterruptedException {
        if (client.getSubscribedTopics().isEmpty())
        {
            printError(
                out,
                "You are not subscribed to any topics."
            );
        }
        else
        {
            final var chirps = client.getLatestChirps();

            if (chirps.isEmpty())
            {
                printWarning(
                    out,
                    "No chirps exist for any of your subscribed"
                        + " topics."
                );
            }

            for (final var chirp : chirps)
                out.println(chirp);

            out.flush();
        }
    }

    private static void handleSubscribe(Client client, String topics, PrintWriter out)
    {
        if (topics == null)
            topics = "";

        try
        {
            client.setSubscribedTopics(topics.split("\\s+"));
        }
        catch (IllegalArgumentException e)
        {
            printError(out, e.getMessage());
        }
    }

    private static void handlePublish(Client client, String chirp, PrintWriter out)
    {
        try
        {
            client.publishChirp(chirp);
        }
        catch (IllegalArgumentException e)
        {
            printError(out, e.getMessage());
        }
    }

    private static void printWarning(PrintWriter out, String msg)
    {
        out.format("\033[33m%s\033[0m\n", msg);
        out.flush();
    }

    private static void printError(PrintWriter out, String msg)
    {
        out.format("\033[31m%s\033[0m\n", msg);
        out.flush();
    }
}

/* -------------------------------------------------------------------------- */
