/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Util;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

/* -------------------------------------------------------------------------- */

public class PeerMain
{
    public static void main(String[] args)
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
                    err.println("Usage: chirper-peer <config_file>");
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
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
