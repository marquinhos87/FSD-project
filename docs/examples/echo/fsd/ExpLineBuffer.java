package fsd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ExpLineBuffer {

    /*
    private final SocketChannel s;
    private ByteBuffer input = ByteBuffer.allocate(1000);
    private ByteBuffer line = ByteBuffer.allocate(1000);

    public ExpLineBuffer(SocketChannel s) {
        this.s = s;
        input.flip();
    }

    public Box<String> readLine() throws IOException {

        Box<String> box = new Box<>();

        while(input.hasRemaining()) {
            byte c = input.get();
            if (c == '\n') {
                line.flip();
                byte[] data = new byte[line.remaining()];
                line.get(data);
                line.clear();
                box.put(new String(data));
            }
            line.put(c);
        }
        input.clear();
        s.read(input, ...);
          // completed -> readLine();
        return box;
    }
    */
}
