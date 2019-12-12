package fsd;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestAtomix {
    public static class Msg {
        int a; String b;
    }

    public static void main(String[] args) throws Exception {

        ExecutorService e = Executors.newFixedThreadPool(1);

        ManagedMessagingService ms = new NettyMessagingService(
                "teste",
                Address.from(12345),
                new MessagingConfig());
        ms.start();

        Serializer s = new SerializerBuilder()
                .addType(Msg.class)
                .build();

        // Receber
        ms.registerHandler("msg", (a,b)-> {
            Msg m = s.decode(b);

            System.out.println("Recebi "+m+" de "+a);
        }, e);

        // Enviar
        Msg m = new Msg();

        ms.sendAsync(Address.from("localhost", 12345), "msg", s.encode(m))
                .thenRun(() -> {
                    ms.sendAsync(Address.from("localhost", 12345), "msg", s.encode(m));
                });

    }
}
