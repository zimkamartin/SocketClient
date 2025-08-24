package protocol;

/**
 * Represents seed1 and seed2 from the protocol.
 */
class Seeds {
    private final byte[] seed1;
    private final byte[] seed2;

    Seeds(byte[] seed1, byte[] seed2) {
        this.seed1 = seed1;
        this.seed2 = seed2;
    }

    byte[] getSeed1() {
        return this.seed1;
    }

    byte[] getSeed2() {
        return this.seed2;
    }
}
