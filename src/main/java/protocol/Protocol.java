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
    // THIS IS NOT HOW TO DO IT !!! THIS IS JUST FOR PROOF-OF-CONCEPT !!! THIS IS NOT HOW TO DO IT !!!
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
    }

    private Seeds createSeeds() {
        // seed1 = SHA3-256(salt||SHA3-256(I||pwd))
        byte[] salt =  new byte[11];  // I just guessed this constant, look into security of that.
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
        byte[] publicSeed = new byte[34];
        engine.getRandomBytes(publicSeed);
        return publicSeed;
    }

    private void getEtaNoise(Polynomial r, byte[] seed) {
        byte[] buf = new byte[n * eta / 4];
        engine.prf(buf, seed);
        mlkem.generateCbdPolynomial(r, buf, eta);
    }

    // TODO send I, salt, v to server
    void phase0(byte[] publicSeed) {
        // v = asv + 2ev  //
        // seed1 = SHA3-256(salt||SHA3-256(I||pwd)); seed2 = SHA3-256(seed1)  //
        // Create polynomial a from public seed.
        Polynomial aNtt = new Polynomial(new BigInteger[n]);
        mlkem.generateUniformPolynomialNtt(engine, aNtt, publicSeed);
        // Based on seeds (computed from private values) generate sv, ev.
        Seeds seeds = createSeeds();
        Polynomial sv = new Polynomial(new BigInteger[n]);
        Polynomial ev = new Polynomial(new BigInteger[n]);
        getEtaNoise(sv, seeds.getSeed1());
        getEtaNoise(ev, seeds.getSeed2());
        Polynomial svNtt = ntt.convertToNtt(sv);
        Polynomial evNtt = ntt.convertFromNtt(ev);
        // Do all the math.
        Polynomial aSvNtt = ntt.multiplyNttPolys(aNtt, svNtt);
        Polynomial twoEvNtt = ntt.multiplyNttPolys(ntt.generateConstantTwoPolynomialNtt(), evNtt);
        Polynomial vNtt = ntt.add(aSvNtt, twoEvNtt);
    }

    // TODO add sending and receiving to and from server
    void phase1(byte[] publicSeed) {
        Polynomial constantTwoPolyNtt = ntt.generateConstantTwoPolynomialNtt();
        // pi = as1 + 2e1 //
        // Compute a.
        Polynomial aNtt = new Polynomial(new BigInteger[n]);
        mlkem.generateUniformPolynomialNtt(engine, aNtt, publicSeed);
        // Compute s1.
        Polynomial s1 = new Polynomial(new BigInteger[n]);
        byte[] s1RandomSeed = new byte[34];
        engine.getRandomBytes(s1RandomSeed);
        getEtaNoise(s1, s1RandomSeed);
        Polynomial s1Ntt = ntt.convertToNtt(s1);
        // Compute e1.
        Polynomial e1 = new Polynomial(new BigInteger[n]);
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
