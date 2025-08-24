package protocol;

/**
 * Represents seed1 and seed2 from the protocol. Also represents salt used when computing those seeds.
 */
class Seeds {
    private final byte[] seed1;
    private final byte[] seed2;
    private final byte[] salt;

    Seeds(byte[] seed1, byte[] seed2, byte[] salt) {
        this.seed1 = seed1;
        this.seed2 = seed2;
        this.salt = salt;
    }

    byte[] getSeed1() {
        return this.seed1;
    }

    byte[] getSeed2() {
        return this.seed2;
    }
}
