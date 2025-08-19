package protocol;

import java.math.BigInteger;

/**
 * Represents all modulo polynomials in NTT tree.
 * <p>
 * These polynomials are of the following form: X^powerX P (zeta_indexZeta)^powerZeta,
 * where P is plus if plus, else it is minus.
 * For more details see https://electricdusk.com/ntt.html
 * </p>
 */
class ModuloPoly {

    private final BigInteger powerX;
    private final boolean plus;
    private final BigInteger powerZeta;
    private final BigInteger indexZeta;

    ModuloPoly(BigInteger powerX, boolean plus, BigInteger powerZeta, BigInteger indexZeta) {
        this.powerX = powerX;
        this.plus = plus;
        this.powerZeta = powerZeta;
        this.indexZeta = indexZeta;
    }

    BigInteger getPowerX() {
        return powerX;
    }

    boolean getPlus() {
        return plus;
    }

    BigInteger getPowerZeta() {
        return powerZeta;
    }

    BigInteger getIndexZeta() {
        return indexZeta;
    }
}