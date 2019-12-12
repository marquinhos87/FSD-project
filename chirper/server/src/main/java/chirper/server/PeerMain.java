/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.OptionalInt;

/* -------------------------------------------------------------------------- */

public class PeerMain
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

                if (args.length != 1)
                {
                    err.println("Usage: chirper-server <config>");
                    err.flush();
                    System.exit(2);
                }

                // parse peer config

                final var config = Config.parseYamlFile(Path.of(args[0]));

                // run peer

                final var peer = new Peer(
                    config.getLocalPeerId(),
                    config.getLocalPeerPort(),
                    config.getRemotePeerIds()
                    );

                peer.run();
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
