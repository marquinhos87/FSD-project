package fsd;

import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class FutureEcho {

    public static void main(String[] args) throws Exception {

        FutureServerSocketChannel ssc = new FutureServerSocketChannel();
        ssc.bind(new InetSocketAddress(12345));

        ByteBuffer buf = ByteBuffer.allocate(1000);

        ssc.accept().thenAccept((s)-> {

            s.read(buf)
                    .thenCompose((n)-> {
                        buf.flip();
                        return s.write(buf);
                    })
                    .thenRun(() -> { System.out.println("Done!"); });
        });

        // batota... devia ser um while(!terminout) wait();
        while(true)
            Thread.sleep(1000);
    }
}
