/* -------------------------------------------------------------------------- */

package chirper.server.prev;

import chirper.shared.Msg;
import chirper.shared.Util;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/* -------------------------------------------------------------------------- */

public class Server
{

    private ManagedMessagingService ms;
    private ExecutorService es;
    private Serializer s;
    private long timeStamp;
    private List<Address> servers;
    private Map<Address, Client > clients;
    private Map<String,List< Msg >> chirps;

    public Server(String args)
    {
        File f = new File(args);

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));

            String aux = br.readLine();
            String[] p = aux.split(":");

            this.ms = new NettyMessagingService(
                "servidor", Address.from(p[0], Integer.parseInt(p[1])),
                new MessagingConfig());

            this.servers = new ArrayList<>();

            while((aux = br.readLine()) != null) {
                p = aux.split(":");
                this.servers.add(Address.from(p[0],Integer.parseInt(p[1])));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.es = Executors.newFixedThreadPool(1);

        this.clients = new HashMap<>();

        this.chirps = new HashMap<>();

        this.s = new SerializerBuilder().addType(Msg.class).build();

        this.timeStamp = 0;
    }

    public void run()
    {
        ms.start();
        handleServer();
        handleClient();
        while(true); // temporario so para poder testar
    }

    // commands

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^\\s*!\\s*(?<command>\\w+)(?:\\s+(?<args>.*))?"
    );

    public void handleClient()
    {
        this.ms.registerHandler("cliente",(a,b) -> {

            Client c;
            Msg m;
            byte[] subbedchirps = new byte[0];

            if(!clients.containsKey(a))
            {
                c = new Client(a);
                clients.put(a, c);
            }
            else {
                c = clients.get(a);
            }

            m = this.s.decode(b);

            final var matcher = COMMAND_PATTERN.matcher(m.getMsg());

            if (matcher.matches()) {
                // treat line as command

                switch (matcher.group("command")) {
                    case "get":


                        subbedchirps = handleGet(c);

                        break;

                    case "sub":
                    case "subscribe":

                        handleSubscribe(c, matcher.group("args"));

                        break;

                    default:

                        break;
                }
            }
            else {

                addChirp(m);

            }

            return subbedchirps;

        },  this.es);
    }

    private byte[] handleGet(Client c)
    {

        StringBuilder sb = new StringBuilder();

        for (final var topic : c.getTopicos())
            if (chirps.containsKey(topic))
                for (final var mg : chirps.get(topic))
                    sb.append(mg.getMsg()).append("\n");

        return sb.toString().getBytes();
    }

    private void handleSubscribe(Client c, String topics)
    {
        String[] aux = topics.split(" ");
        c.setTopicos(List.of(aux));
    }

    private List<String> parseTopics(Msg m)
    {

        List < String > topics = new ArrayList<>(Util.getChirpTopics(m.getMsg()));

        return topics;
    }

    public void addChirp(Msg m)
    {

        //Nas mensagens adicionar os timeStamp e serverID

        final var topics = parseTopics(m);

        for(final var topic: topics)
        {

            if(chirps.containsKey(topic)) {
                List< Msg > msgs = chirps.get(topic);

                if (msgs.size() == 10)
                {
                    msgs.remove(0);
                    msgs.add(9, m);
                }

                else
                {
                        msgs.add(m);
                }
            }
            else
            {
                List< Msg > list = new ArrayList<>();
                list.add(m);
                chirps.put(topic,list);
            }
        }
    }

    public void handleServer()
    {
        this.ms.registerHandler("server",(a,b) -> {
            Msg m = this.s.decode(b);
            System.out.println(m.getMsg());
        },this.es);
    }


}

/* -------------------------------------------------------------------------- */
