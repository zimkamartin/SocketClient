package protocol;

import java.io.IOException;
import java.math.BigInteger;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * This is a demo (and proof-of-concept) of the protocol https://eprint.iacr.org/2017/1196.pdf
 * <p>
 * N - polynomial size - must be power of 2 and fit into int data type
 * Q - defines Z_Q for coefficients in the polynomials - must be prime, must be congruent with 1 modulo 2 * N
 * ETA - defines Central binomial distribution when generating error polynomials.
 * SOURCE for Client communication logic: https://www.baeldung.com/java-unix-domain-socket
 * SOURCE for simple message exchange: CHATGPT
 * </p>
 */
public class Main {

    private static final int N = 1024;
    private static final BigInteger Q = BigInteger.valueOf(1073479681);
    private static final int ETA = 3;

    static byte[] publicSeed;

    private static String readMessage(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(buffer);
        if (bytesRead < 0) return null;

        buffer.flip();
        byte[] bytes = new byte[bytesRead];
        buffer.get(bytes);
        return new String(bytes);
    }

    public static void main(String[] args) throws IOException {

        Protocol protocol = new Protocol(N, Q, ETA);
        publicSeed = protocol.generatePublicSeed();

        Path socketPath = Path.of(System.getProperty("user.home")).resolve("socket");
        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);

        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(socketAddress);

            if (!protocol.phase0(channel, publicSeed)) {
                // TODO do smth
            }

//            // 1. Send message to server
//            String msg1 = "Hello Server!";
//            sendMessage(channel, msg1);
//            System.out.println("[Client] Sent: " + msg1);
//
//            // 2. Receive reply
//            String reply1 = readMessage(channel);
//            System.out.println("[Server] " + reply1);
//
//            // 3. Send another message
//            String msg2 = "Nice to talk to you.";
//            sendMessage(channel, msg2);
//            System.out.println("[Client] Sent: " + msg2);
//
//            // 4. Receive final reply
//            String reply2 = readMessage(channel);
//            System.out.println("[Server] " + reply2);
        }
    }
}
