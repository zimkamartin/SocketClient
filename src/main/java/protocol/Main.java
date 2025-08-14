package protocol;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

// SOURCE for Client logic: https://www.baeldung.com/java-unix-domain-socket
public class Main {
    public static void main(String[] args) throws IOException {
        Path socketPath = Path
                .of(System.getProperty("user.home"))
                .resolve("socket");
        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);

        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(socketAddress);

        String message1 = "Hello World!";

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        buffer.put(message1.getBytes());
        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        Files.deleteIfExists(socketPath);
    }
}