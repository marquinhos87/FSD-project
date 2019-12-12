package fsd;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsynchEcho {
    public static void main(String[] args) throws Exception {
        AsynchronousChannelGroup g = AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory());

        AsynchronousServerSocketChannel ssc = AsynchronousServerSocketChannel.open(g);

        ssc.bind(new InetSocketAddress(12345));

        ssc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(final AsynchronousSocketChannel sc, Object o) {
                final ByteBuffer buf = ByteBuffer.allocate(1024);

                sc.read(buf, null, new CompletionHandler<Integer, Object>() {
                    @Override
                    public void completed(Integer integer, Object o) {
                        buf.flip();

                        sc.write(buf, null, new CompletionHandler<Integer, Object>() {
                            @Override
                            public void completed(Integer integer, Object o) {
                                buf.clear();


                            }

                            @Override
                            public void failed(Throwable throwable, Object o) {

                            }
                        });
                    }

                    @Override
                    public void failed(Throwable throwable, Object o) {

                    }
                });

            }

            @Override
            public void failed(Throwable throwable, Object o) {

            }
        });


        g.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
