import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    public static void main(String[] args){
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String[] ip = args;
        if(args.length < 2) {
            System.out.println("Coloque o IP e a porta do servidor a que se pretende ligar");
            try {
                ip = in.readLine().split(" ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //Address ad = Address.from(ip[0],Integer.parseInt(ip[1]));
            Socket s = new Socket(ip[0],Integer.parseInt(ip[1]));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
