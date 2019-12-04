/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import io.netty.util.concurrent.CompleteFuture;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        List<Msg> list = new ArrayList<>();
        list.add(new Msg("ola"));
        list.add(new Msg("ole"));
        list.add(new Msg("olu"));
        chirps.put("ola",list);

        this.s = new SerializerBuilder().addType(Msg.class).build();
    }

    public void run()
    {
        ms.start();
        handleClient();
        receberServer();
        while(true); // so para testar
    }

    public void handleClient()
    {
        this.ms.registerHandler("cliente",(a,b) -> {
            Msg m = this.s.decode(b);
            StringBuilder sb = new StringBuilder(); //Só para testar enquanto não há login
            if(!clients.containsKey(a)) {
                Client c = new Client(a);
                clients.put(a, c);
            }
            if (m.getMsg().contains("!sub")) {
                String[] aux = m.getMsg().split(" ",2);
                Client c = clients.get(a);
                c.setTopicos(List.of(aux[1].split(" ")));
            }
            else if (m.getMsg().equals("!get")) {
                for (final var topic : clients.get(a).getTopicos())
                    if (chirps.containsKey(topic))
                        for (final var mg : chirps.get(topic))
                            sb.append(mg.getMsg()).append("\n");
            }
            else {
                addChirp(m);
            }
            if(sb == null)
                return " ".getBytes();
            return sb.toString().getBytes();
        },  this.es);
    }

    public void addChirp(Msg m)
    {
        //fazer parse do Chirp para retirar os tópicos e colocar no map
        //Nas mensagens adicionar os timeStamp e serverID
        final var topics = parseTopics(m);
        for(final var topic: topics) {
            if(chirps.containsKey(topic)) {
                List<Msg> msgs = chirps.get(topic);
                if(msgs.size() == 10) {
                    msgs.remove(0);
                    msgs.add(9,m);
                }
                else {
                    msgs.add(m);
                }
            }
            else {
                List<Msg> list = new ArrayList<>();
                list.add(m);
                chirps.put(topic,list);
            }
        }
    }

    private List<String> parseTopics(Msg m)
    {
        //TODO : implement
        return new ArrayList<>();
    }

    public void receberServer()
    {
        this.ms.registerHandler("server",(a,b) -> {
            Msg m = this.s.decode(b);
            System.out.println(m.getMsg());
        },this.es);
    }
}

/* -------------------------------------------------------------------------- */
