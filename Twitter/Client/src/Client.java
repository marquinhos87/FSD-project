import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        if(args.length < 2) {

        }
        else {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader((System.in)));
                Socket s = new Socket(args[0],Integer.parseInt(args[1]));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
