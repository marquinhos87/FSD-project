package fsd;

import spullara.nio.channels.FutureSocketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

public class FutureLineBuffer {

    private final FutureSocketChannel s;
    private ByteBuffer input = ByteBuffer.allocate(1000);
    private ByteBuffer line = ByteBuffer.allocate(1000);

    public FutureLineBuffer(FutureSocketChannel s) {
        this.s = s;
        input.flip();
    }

    public CompletableFuture<String> readLine() {
        while(input.hasRemaining()) {
            byte c = input.get();
            if (c == '\n') {
                line.flip();
                byte[] data = new byte[line.remaining()];
                line.get(data);
                line.clear();
                return CompletableFuture.completedFuture(new String(data));
            }
            line.put(c);
        }
        input.clear();
        return s.read(input).thenCompose((n)->{input.flip();return readLine()});
    }
}
