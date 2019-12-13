/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.shared.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;

/* -------------------------------------------------------------------------- */

public class Main
{
    public static void main(String[] args) throws IOException
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

                if (args.length != 1)
                {
                    err.println("Usage: chirper-peer <config_file>");
                    err.flush();
                    System.exit(2);
                }

                // parse peer config

                final var config = PeerConfig.parseYamlFile(Path.of(args[0]));

                // run peer

                try (final var peer = new Peer(config))
                {
                    peer.start();
                    in.readLine();
                }
//            }
//            catch (Exception e)
//            {
//                Util.printError(err, e.getMessage());
//                System.exit(1);
//            }
        }
    }
}

/* -------------------------------------------------------------------------- */
