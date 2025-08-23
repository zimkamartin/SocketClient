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
    private final int eta;
    private final Engine engine;
    private final Ntt ntt;
    private final Mlkem mlkem;

    Protocol(BigInteger n, BigInteger q, int eta) {
        this.n = n;
        this.q = q;
        this.eta = eta;
        this.engine = new Engine();
        this.ntt = new Ntt(this.n, this.q);
        this.mlkem = new Mlkem(this.n, this.q);
    }

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

    public byte[] generatePublicSeed() {
        byte[] publicSeed = new byte[34];
        engine.getRandomBytes(publicSeed);
        return publicSeed;
    }

    private void getEtaNoise(Polynomial r, byte[] seed) {
        byte[] buf = new byte[n.intValue() * eta / 4];
        engine.prf(buf, seed);
        mlkem.generateCbdPolynomial(r, buf, eta);
    }

    // TODO send I, salt, v to server
    void phase0(byte[] publicSeed) {
        // v = asv + 2ev  //
        // seed1 = SHA3-256(salt||SHA3-256(I||pwd)); seed2 = SHA3-256(seed1)  //
        // Create polynomial a from public seed.
        Polynomial aNtt = new Polynomial(new BigInteger[n.intValue()]);  // !!! conversion to int !!!
        mlkem.generateUniformPolynomialNtt(engine, aNtt, publicSeed);
        // Based on seeds (computed from private values) generate sv, ev.
        Seeds seeds = createSeeds();
        Polynomial sv = new Polynomial(new BigInteger[n.intValue()]);  // !!! conversion to int !!!
        Polynomial ev = new Polynomial(new BigInteger[n.intValue()]);  // !!! conversion to int !!!
        getEtaNoise(sv, seeds.getSeed1());
        getEtaNoise(ev, seeds.getSeed1());
        Polynomial svNtt = ntt.convertToNtt(sv);
        Polynomial evNtt = ntt.convertFromNtt(ev);
        // Do all the math.
        Polynomial aSvNtt = ntt.multiplyNttPolys(aNtt, svNtt);
        Polynomial twoEvNtt = ntt.multiplyNttPolys(ntt.generateConstantPolynomialNtt(BigInteger.TWO), evNtt);
        Polynomial vNtt = ntt.add(aSvNtt, twoEvNtt);
    }

    // TODO
    void phase1() {
    }

    // TODO
    void phase2() {
    }
}
