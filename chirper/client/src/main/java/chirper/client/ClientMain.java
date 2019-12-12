/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Util;
import io.atomix.utils.net.Address;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/* -------------------------------------------------------------------------- */

public class ClientMain
{
    public static void main(String[] args) throws IOException
    {
        try (
            final var in = new BufferedReader(new InputStreamReader(System.in));
            final var out = new PrintWriter(new OutputStreamWriter(System.out));
            final var err = new PrintWriter(new OutputStreamWriter(System.err))
        )
        {
            try
            {
                // check usage and parse arguments

                final var serverAddress = parseArgs(args);

                if (serverAddress.isEmpty())
                {
                    err.println("Usage: chirper <host> <port>");
                    err.flush();
                    System.exit(2);
                }

                // run client and input loop

                try (final var client = new Client(serverAddress.get()))
                {
                    client.start();
                    new Prompt(client, in, out).inputLoop();
                }
            }
            catch (Exception e)
            {
                Util.printError(err, e.getMessage());
                System.exit(1);
            }
        }
    }

    private static Optional< Address > parseArgs(String[] args)
    {
        // check number of arguments

        if (args.length != 2)
            return Optional.empty();

        return new Address()

        // parse (and resolve) host

        final var address = InetAddress.getByName(args[0]);

        // parse port

        final int port;

        try
        {
            port = Integer.parseUnsignedInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid port number.");
        }

        // return socket address

        return Optional.of(new InetSocketAddress(address, port));
    }
}

/* -------------------------------------------------------------------------- */
