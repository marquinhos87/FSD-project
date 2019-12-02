/* -------------------------------------------------------------------------- */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

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
            new Server().run();
        }
    }
}

/* -------------------------------------------------------------------------- */
