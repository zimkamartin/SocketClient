package protocol;

import org.bouncycastle.crypto.digests.SHAKEDigest;

/**
 * Represents XOF function.
 * <p>
 * Heavily inspired by https://github.com/bcgit/bc-java/blob/2f4d33d57797dcc3fe9bd4ecb07ee0557ff58185/core/src/main/java/org/bouncycastle/pqc/crypto/mlkem/Symmetric.java
 * </p>
 */
class ShakeSymmetric {
    private final int xofBlockBytes;
    private final SHAKEDigest xof;

    ShakeSymmetric() {
        this.xofBlockBytes = 168;
        this.xof = new SHAKEDigest(128);
    }

    void xofAbsorb(byte[] seed) {
        xof.reset();
        xof.update(seed, 0, seed.length);
    }

    void xofSqueezeBlocks(byte[] out, int outOffset, int outLen) {
        xof.doOutput(out, outOffset, outLen);
    }

}