/* -------------------------------------------------------------------------- */

package chirper.client;

import chirper.shared.Util;
import io.atomix.utils.net.Address;
import io.atomix.utils.net.MalformedAddressException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

/* -------------------------------------------------------------------------- */

/**
 * TODO: document
 */
public class Main
{
    /**
     * TODO: document
     *
     * @param args TODO: document
     * @throws IOException TODO: document
     */
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException
    {
        try (
            final var in = new BufferedReader(new InputStreamReader(System.in));
            final var out = new PrintWriter(new OutputStreamWriter(System.out));
            final var err = new PrintWriter(new OutputStreamWriter(System.err))
        )
        {
//            try
//            {
                // check usage and parse arguments

                final var serverAddress = parseArgs(args);

                if (serverAddress == null)
                {
                    err.println("Usage: chirper <server_endpoint>");
                    err.flush();
                    System.exit(2);
                }

                // hack: suppress "illegal reflective access" warnings

                System.err.close();
                System.setErr(System.out);

                // run client and input loop

                try (final var client = new Client(serverAddress))
                {
                    client.start();
                    new Prompt(client, in, out).inputLoop();
                }
//            }
//            catch (Exception e)
//            {
//                Util.printError(err, e.getMessage());
//                System.exit(1);
//            }
        }
    }

    /**
     * TODO: document
     *
     * @param args TODO: document
     * @return TODO: document
     */
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
