/* -------------------------------------------------------------------------- */

package chirper.client;

import io.atomix.utils.net.Address;
import io.atomix.utils.net.MalformedAddressException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

/* -------------------------------------------------------------------------- */

public class Main
{
    public static void main(String[] args)
        throws ExecutionException, InterruptedException, IOException
    {
        try (
            final var in = new BufferedReader(new InputStreamReader(System.in));
            final var out = new PrintWriter(new OutputStreamWriter(System.out))
        )
        {
            // check usage and parse arguments

            final var serverAddress = parseArgs(args);

            if (serverAddress == null)
            {
                out.println("Usage: chirper <server_endpoint>");
                out.flush();
                System.exit(2);
            }

            // run client and input loop

            try (final var client = new Client(serverAddress))
            {
                client.start();
                new Prompt(client, in, out).inputLoop();
            }
        }
    }

    private static Address parseArgs(String[] args)
    {
        // check number of arguments

        if (args.length != 1)
            return null;

        // parse argument as an endpoint

        try
        {
            return Address.from(args[0]);
        }
        catch (MalformedAddressException e)
        {
            return null;
        }
    }
}

/* -------------------------------------------------------------------------- */
