/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server
{

    private ManagedMessagingService ms;
    private ExecutorService es;
    private Serializer s;

    public Server()
    {
        this.ms = new NettyMessagingService(
                "servidor", Address.from("localhost",12345),
                new MessagingConfig());

        this.es = Executors.newFixedThreadPool(1);

        this.s = new SerializerBuilder().addType(Msg.class).build();
    }

    public void run()
    {
        echo();
    }

    public void echo()
    {
        this.ms.registerHandler("cliente",(a,b) -> {
            Msg m = this.s.decode(b);
            System.out.println(m.getMsg());
        },this.es);
    }
}

/* -------------------------------------------------------------------------- */
