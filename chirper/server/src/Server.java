/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Server
{

    private ManagedMessagingService ms;
    private ExecutorService es;
    private Serializer s;
    private Map<Address,Client> clients;
    private Map<String,List<Msg>> chirps;

    public Server()
    {
        this.ms = new NettyMessagingService(
                "servidor", Address.from("localhost",12345),
                new MessagingConfig());

        this.es = Executors.newFixedThreadPool(1);

        this.clients = new HashMap<>();

        this.chirps = new HashMap<>();

        //List<Msg> list = new ArrayList<>();
        //list.add(new Msg("ola"));
        //list.add(new Msg("ole"));
        //list.add(new Msg("olu"));
        //chirps.put("ola",list);

        this.s = new SerializerBuilder().addType(Msg.class).build();
    }

    public void run()
    {
        ms.start();
        handleClient();
        handleServer();
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

        List < String > topics = new ArrayList<>(Config.getChirpTopics(m.getMsg()));

        return topics;
    }

    public void addChirp(Msg m)
    {

        //Nas mensagens adicionar os timeStamp e serverID

        final var topics = parseTopics(m);

        for(final var topic: topics)
        {

            if(chirps.containsKey(topic)) {
                List<Msg> msgs = chirps.get(topic);

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
                List<Msg> list = new ArrayList<>();
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
