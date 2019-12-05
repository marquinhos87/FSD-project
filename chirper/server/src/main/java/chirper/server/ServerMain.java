/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import chirper.shared.Util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.OptionalInt;

/* -------------------------------------------------------------------------- */

public class ServerMain
{
    public static void main(String[] args) throws IOException
    {
        try (
            final var out = new PrintWriter(new OutputStreamWriter(System.out));
            final var err = new PrintWriter(new OutputStreamWriter(System.err))
        )
        {
            try
            {
                // check usage and parse arguments

                final var port = parseArgs(args);

                if (port.isEmpty())
                {
                    err.println("Usage: chirper-server [<port>]");
                    err.flush();
                    System.exit(2);
                }

                // run server

                new Server(port.getAsInt()).run();
            }
            catch (Exception e)
            {
                Util.printError(err, e.getMessage());
                System.exit(1);
            }
        }
    }

    private static OptionalInt parseArgs(String[] args)
    {
        int port;

        if (args.length == 0)
        {
            port = Config.DEFAULT_PORT;
        }
        else if (args.length == 1)
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
            return OptionalInt.empty();
        }

        return OptionalInt.of(port);
    }
}

/* -------------------------------------------------------------------------- */
