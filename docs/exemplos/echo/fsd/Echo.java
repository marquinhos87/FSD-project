package fsd;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Echo {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();

        ssc.bind(new InetSocketAddress(12345));

        while(true) {

            SocketChannel sc = ssc.accept();

            ByteBuffer buf = ByteBuffer.allocate(1024);

            sc.read(buf);
            buf.flip();

            sc.write(buf.duplicate());
            buf.clear();

            sc.close();

        }
    }
}
