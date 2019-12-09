/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Config;
import chirper.shared.Util;
import io.atomix.utils.net.Address;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                final var n_servers = Integer.parseUnsignedInt(args[1]);

                if (port.isEmpty() || args.length != 2)
                {
                    err.println("Usage: chirper-server <N servers> [<port>]");
                    err.flush();
                    System.exit(2);
                }

                // run server

                int p = port.getAsInt();
                List<Address> servers = new ArrayList<>();

                for (int i = p; i < (p + n_servers); i+=1)
                {
                    if (i > p)
                    {
                        servers.add(Address.from("localhost",i));
                    }
                }

                new Server(port.getAsInt(),servers).run();

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

        if (args.length == 1)
        {
            port = Config.DEFAULT_PORT;
        }
        else if (args.length == 2)
        {
            try
            {
                port = Integer.parseUnsignedInt(args[2]);
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
