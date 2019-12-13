/* -------------------------------------------------------------------------- */

package chirper.server;

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
                    err.println("Usage: chirper-server <config_file>");
                    err.flush();
                    System.exit(2);
                }

                // hack: suppress "illegal reflective access" warnings

                System.err.close();
                System.setErr(System.out);

                // parse server config

                final var config = ServerConfig.parseYamlFile(Path.of(args[0]));

                // run server

                try (final var server = new Server(config))
                {
                    server.start();
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
