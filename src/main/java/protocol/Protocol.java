package protocol;

import java.math.BigInteger;

/**
 * Represents the whole protocol from the article.
 * <p>
 * Source for the protocol: https://eprint.iacr.org/2017/1196.pdf
 * </p>
 */
class Protocol {
    // THIS IS NOT HOW TO DO IT !!! THIS IS JUST FOR PROOF-OF-CONCEPT !!! THIS IS NOT HOW TO DO IT !!!
    private static final String I = "identity123";
    private static final String PWD = "password123";
    private static final byte[] SALT = "salt123".getBytes();
    // THIS IS NOT HOW TO DO IT !!! THIS IS JUST FOR PROOF-OF-CONCEPT !!! THIS IS NOT HOW TO DO IT !!!
    private final BigInteger n;
    private final BigInteger q;
    private final Engine engine;
    private final Seeds clientsSeeds;
    public byte[] publicSeed;
    private final Polynomial aNtt;
    private final Ntt ntt;
    private final Mlkem mlkem;

    private Seeds createSeeds() {
        // seed1 = SHA3-256(salt||SHA3-256(I||pwd))
        String innerInput = I.concat(PWD);
        byte[] innerHash = new byte[32];
        engine.hash(innerHash, innerInput.getBytes());
        byte[] outerInput = new byte[SALT.length + innerHash.length];
        System.arraycopy(SALT, 0, outerInput, 0, SALT.length);
        System.arraycopy(innerHash, 0, outerInput, SALT.length, innerHash.length);
        byte[] seed1 = new byte[32];
        engine.hash(seed1, outerInput);
        // seed2 = SHA3-256(seed1)
        byte[] seed2 = new byte[32];
        engine.hash(seed2, seed1);

        return new Seeds(seed1, seed2);
    }

    Protocol(BigInteger n, BigInteger q) {
        this.n = n;
        this.q = q;
        this.engine = new Engine();
        this.clientsSeeds = createSeeds();
        this.publicSeed = new byte[34];
        this.ntt = new Ntt(n, q);
        this.mlkem = new Mlkem(n, q);
        this.aNtt = new Polynomial(new BigInteger[n.intValue()]);
        mlkem.generateUniformPolynomialNtt(engine, aNtt, publicSeed);
    }

    // TODO
    void phase0() {
    }

    // TODO
    void phase1() {
    }

    // TODO
    void phase2() {
    }
}
