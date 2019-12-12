package fsd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;

public class AsyncLineBuffer {

    private final AsynchronousSocketChannel s;
    private ByteBuffer input = ByteBuffer.allocate(1000);
    private ByteBuffer line = ByteBuffer.allocate(1000);

    public AsyncLineBuffer(AsynchronousSocketChannel s) {
        this.s = s;
        input.flip();
    }

    public <A> void readLine(final A attachment, final CompletionHandler<String, A> ch) {
        while(input.hasRemaining()) {
            byte c = input.get();
            if (c == '\n') {
                line.flip();
                byte[] data = new byte[line.remaining()];
                line.get(data);
                line.clear();
                ch.completed(new String(data), attachment);
            }
            line.put(c);
        }
        input.clear();

        s.read(input, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer integer, Object o) {
		input.flip();
                readLine(attachment, ch);
            }

            @Override
            public void failed(Throwable throwable, Object o) {
                ch.failed(throwable, attachment);
            }
        });
        
    }
}
