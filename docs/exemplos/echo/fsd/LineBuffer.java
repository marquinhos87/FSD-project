package fsd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class LineBuffer {

    private final SocketChannel s;
    private ByteBuffer input = ByteBuffer.allocate(1000);
    private ByteBuffer line = ByteBuffer.allocate(1000);

    public LineBuffer(SocketChannel s) {
        this.s = s;
        input.flip();
    }

    public String readLine() throws IOException {
        while(input.hasRemaining()) {
            byte c = input.get();
            if (c == '\n') {
                line.flip();
                byte[] data = new byte[line.remaining()];
                line.get(data);
                line.clear();
                return new String(data);
            }
            line.put(c);
        }
        input.clear();
        s.read(input);
	input.flip();
        return readLine();
    }
}
