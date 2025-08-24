package protocol;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Represents the whole protocol from the article. (Together with sending and receiving messages to and from server.)
 * <p>
 * Source for the protocol: https://eprint.iacr.org/2017/1196.pdf
 * </p>
 */
class Protocol {
    // THIS IS NOT HOW TO DO IT !!! THIS IS JUST FOR PROOF-OF-CONCEPT !!! THIS IS NOT HOW TO DO IT !!!
    private static final String I = "identity123";
    private static final String PWD = "password123";
    // THIS IS NOT HOW TO DO IT !!! THIS IS JUST FOR PROOF-OF-CONCEPT !!! THIS IS NOT HOW TO DO IT !!!
    private static final int IDENTITYBYTESIZE = 11;  // all characters are ASCII, so 1 char per byte // that size is just made up
    private static final int PUBLICSEEDBYTESIZE = 34;
    private static final int SALTBYTESIZE = 11;  // that size is just made up
    private final int coeffsByteSize;
    private final int n;
    private final BigInteger q;
    private final int eta;
    private final Engine engine;
    private final Ntt ntt;
    private final Mlkem mlkem;

    Protocol(int n, BigInteger q, int eta) {
        this.n = n;
        this.q = q;
        this.eta = eta;
        this.engine = new Engine();
        this.ntt = new Ntt(this.n, this.q);
        this.mlkem = new Mlkem(this.n, this.q);
        this.coeffsByteSize = n * (int) ((q.subtract(BigInteger.ONE).bitLength() + 1 + 7) / 8);  // + 1 because of the sign bit
        // ^^ ceiling
    }

    private Seeds createSeeds() {
        // seed1 = SHA3-256(salt||SHA3-256(I||pwd))
        byte[] salt =  new byte[SALTBYTESIZE];
        engine.getRandomBytes(salt);
        String innerInput = I.concat(PWD);
        byte[] innerHash = new byte[32];
        engine.hash(innerHash, innerInput.getBytes());
        byte[] outerInput = new byte[salt.length + innerHash.length];
        System.arraycopy(salt, 0, outerInput, 0, salt.length);
        System.arraycopy(innerHash, 0, outerInput, salt.length, innerHash.length);
        byte[] seed1 = new byte[32];
        engine.hash(seed1, outerInput);
        // seed2 = SHA3-256(seed1)
        byte[] seed2 = new byte[32];
        engine.hash(seed2, seed1);

        return new Seeds(seed1, seed2, salt);
    }

    public byte[] generatePublicSeed() {
        byte[] publicSeed = new byte[PUBLICSEEDBYTESIZE];
        engine.getRandomBytes(publicSeed);
        return publicSeed;
    }

    private void getEtaNoise(Polynomial r, byte[] seed) {
        byte[] buf = new byte[n * eta / 4];
        engine.prf(buf, seed);
        mlkem.generateCbdPolynomial(r, buf, eta);
    }

    private static boolean sendMessage(SocketChannel channel, byte[] message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean sendPhase0ToServer(SocketChannel channel, byte[] publicSeed, String identity, byte[] salt, Polynomial vNtt) {
        // Build the message //
        byte[] identityBytes = identity.getBytes(StandardCharsets.UTF_8);
        byte[] vNttBytes = vNtt.toBytes();
        int totalLen = PUBLICSEEDBYTESIZE + IDENTITYBYTESIZE + SALTBYTESIZE + coeffsByteSize;
        ByteBuffer msg = ByteBuffer.allocate(totalLen);
        msg.put(publicSeed);
        msg.put(identityBytes);
        msg.put(salt);
        msg.put(vNttBytes);
        byte[] msgByteArray = msg.array();
        // Send the message //
        if (!sendMessage(channel, msgByteArray)) {
            System.out.println("PHASE 0 : Sending message to Server FAILED !");
            return false;
        }
        System.out.println("PHASE 0 : Sending message to Server SUCCEEDED !");
        return true;
    }

    // TODO send publicSeed, I, salt, v to server
    boolean phase0(SocketChannel channel, byte[] publicSeed) {
        // v = asv + 2ev  //
        // Create polynomial a from public seed.
        Polynomial aNtt = new Polynomial(new BigInteger[n], q);
        mlkem.generateUniformPolynomialNtt(engine, aNtt, publicSeed);
        // Based on seeds (computed from private values) generate sv, ev.
        Seeds seeds = createSeeds();
        Polynomial sv = new Polynomial(new BigInteger[n], q);
        Polynomial ev = new Polynomial(new BigInteger[n], q);
        getEtaNoise(sv, seeds.getSeed1());
        getEtaNoise(ev, seeds.getSeed2());
        Polynomial svNtt = ntt.convertToNtt(sv);
        Polynomial evNtt = ntt.convertFromNtt(ev);
        // Do all the math.
        Polynomial aSvNtt = ntt.multiplyNttPolys(aNtt, svNtt);
        Polynomial twoEvNtt = ntt.multiplyNttPolys(ntt.generateConstantTwoPolynomialNtt(), evNtt);
        Polynomial vNtt = ntt.add(aSvNtt, twoEvNtt);

        // Send it to the server.
        return sendPhase0ToServer(channel, publicSeed, I, seeds.getSalt(), vNtt);
    }

    // TODO add sending and receiving to and from server
    void phase1(byte[] publicSeed) {
        Polynomial constantTwoPolyNtt = ntt.generateConstantTwoPolynomialNtt();
        // pi = as1 + 2e1 //
        // Compute a.
        Polynomial aNtt = new Polynomial(new BigInteger[n], q);
        mlkem.generateUniformPolynomialNtt(engine, aNtt, publicSeed);
        // Compute s1.
        Polynomial s1 = new Polynomial(new BigInteger[n], q);
        byte[] s1RandomSeed = new byte[34];
        engine.getRandomBytes(s1RandomSeed);
        getEtaNoise(s1, s1RandomSeed);
        Polynomial s1Ntt = ntt.convertToNtt(s1);
        // Compute e1.
        Polynomial e1 = new Polynomial(new BigInteger[n], q);
        byte[] e1RandomSeed = new byte[34];
        engine.getRandomBytes(e1RandomSeed);
        getEtaNoise(e1, e1RandomSeed);
        Polynomial e1Ntt = ntt.convertFromNtt(e1);
        // Do all the math
        Polynomial aS1Ntt = ntt.multiplyNttPolys(aNtt, s1Ntt);
        Polynomial twoE1Ntt = ntt.multiplyNttPolys(constantTwoPolyNtt, e1Ntt);
        Polynomial piNtt = ntt.add(aS1Ntt, twoE1Ntt);


    }

    // TODO
    void phase2() {
    }
}
