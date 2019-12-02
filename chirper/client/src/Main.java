/* -------------------------------------------------------------------------- */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Optional;

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

//
//        String[] ip = args;
//        if(args.length < 2) {
//            System.out.println("Coloque o IP e a porta do servidor a que se pretende ligar");
//            try {
//                ip = in.readLine().split(" ");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            //Address ad = Address.from(ip[0],Integer.parseInt(ip[1]));
//            Socket s = new Socket(ip[0],Integer.parseInt(ip[1]));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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

                // if (Pattern.mat("^\\s*\\\\(\w+)\s+(.*)$"))
                handleLine(client, line, out);
            }
        }
    }

    private static void handleLine(Client client, String line, PrintWriter out)
    {
        printWarning(out, Config.getChirpTopics(line).toString());
        out.flush();
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
