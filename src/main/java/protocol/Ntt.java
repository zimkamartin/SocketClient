package protocol;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.math3.primes.Primes;

/**
 * Represents all math operations with objects of class Polynomial. For efficiency, everything is done in NTT domain.
 * <p>
 * When an instance is constructed, arrays zetas and zetas inverted are computed.
 * Otherwise, provides just utility functions add, inverse, subtracts, multiply.
 * NTT stuff heavily inspired by https://electricdusk.com/ntt.html
 * </p>
 */
class Ntt {

    private final BigInteger n;
    private final BigInteger q;

    private final List<BigInteger> zetas = new ArrayList<>();
    private final List<BigInteger> zetasInverted = new ArrayList<>();

    private final List<List<ModuloPoly>> nttTree = new ArrayList<>();

    /**
     * Computes tree of modulo polynomials. Everything from layer X^(n//2) to X^1.
     */
    private void computeNttTree() {
        BigInteger powerX = n.divide(BigInteger.TWO);
        BigInteger indexZeta = BigInteger.valueOf(4);
        List<ModuloPoly> fstLayer = new ArrayList<>();
        fstLayer.add(new ModuloPoly(powerX, true, BigInteger.ONE, indexZeta));
        fstLayer.add(new ModuloPoly(powerX, false, BigInteger.ONE, indexZeta));
        nttTree.add(fstLayer);

        while (powerX.compareTo(BigInteger.ONE) > 0) {

            powerX = powerX.divide(BigInteger.TWO);
            indexZeta = indexZeta.multiply(BigInteger.TWO);

            List<ModuloPoly> newLayer = new ArrayList<>();
            List<ModuloPoly> lstLayer = nttTree.getLast();
            for (ModuloPoly poly : lstLayer) {
                BigInteger powerZeta = poly.getPowerZeta();
                if (poly.getPlus()) {
                    powerZeta = powerZeta.add(poly.getIndexZeta().divide(BigInteger.TWO));
                }
                ModuloPoly plusPoly = new ModuloPoly(powerX, true, powerZeta, indexZeta);
                ModuloPoly minusPoly = new ModuloPoly(powerX, false, powerZeta, indexZeta);
                newLayer.add(plusPoly);
                newLayer.add(minusPoly);
            }
            nttTree.add(newLayer);
        }
    }

    private void generateArrays(BigInteger zeta) {
        BigInteger nRoot = (BigInteger.TWO).multiply(n);
        for (List<ModuloPoly> layer: nttTree) {
            for (BigInteger i = BigInteger.ZERO; i.compareTo(BigInteger.valueOf(layer.size())) < 0; i = i.add(BigInteger.ONE)) {
                if (i.mod(BigInteger.TWO).compareTo(BigInteger.ONE) == 0) {
                    continue;
                }
                ModuloPoly poly = layer.get(i.intValue());  // !!! conversion from BigInteger to Int !!!
                BigInteger power = poly.getPowerZeta();
                BigInteger index = poly.getIndexZeta();
                BigInteger z = zeta.modPow(nRoot.divide(index), q).modPow(power, q);
                BigInteger zInverted = z.modPow(BigInteger.valueOf(-1), q);
                zetas.add(z);
                zetasInverted.add(zInverted);
            }
        }
    }

    private Set<BigInteger> findPrimeFactors(BigInteger x) {
        BigInteger i = BigInteger.TWO;
        Set<BigInteger> primeFactors = new HashSet<>();
        while ((i.multiply(i)).compareTo(x) <= 0) {
            if (x.mod(i).equals(BigInteger.ZERO)) {
                x = x.divide(i);
                primeFactors.add(i);
            } else {
                i = i.add(BigInteger.ONE);
            }
        }
        if (x.compareTo(BigInteger.ONE) > 0) {
            primeFactors.add(x);
        }
        return primeFactors;
    }

    /**
     * Compute 2*n-th primitive root of one modulo q.
     * <p>
     * Find generator g of a group Z_q. Primitive root is then g ^ ((q - 1) / 2 * n).
     * Firstly randomly incrementally choose possible g. Check that g ^ (q - 1) is congruent to 1 modulo q.
     * Factorize (q - 1) and check, that there is no smoller exponent y s. t. g ^ y is congruent to 1 modulo q.
     * If not, g is our generator. Compute primitive root and return it.
     * </p>
     */
    private BigInteger computePrimitiveRoot() {
        BigInteger exp = (q.subtract(BigInteger.ONE)).divide((BigInteger.TWO).multiply(n));
        Set<BigInteger> primeFactors = findPrimeFactors(q.subtract(BigInteger.ONE));
        for (BigInteger g = BigInteger.TWO; g.compareTo(q) < 0; g = g.add(BigInteger.ONE)) {
            BigInteger x = g.modPow(exp, q);
            if ((g.modPow(q.subtract(BigInteger.ONE), q)).compareTo(BigInteger.ONE) != 0) {
                continue;
            }
            boolean isPrimitive = true;
            for (BigInteger pf : primeFactors) {
                if (g.modPow((q.subtract(BigInteger.ONE)).divide(pf), q).compareTo(BigInteger.ONE) == 0) {
                    isPrimitive = false;
                    break;
                }
            }
            if (isPrimitive) {
                return x;
            }
        }
        return BigInteger.valueOf(-1);
    }

    private void computeZetaArrays() {
        computeNttTree();
        BigInteger zeta = computePrimitiveRoot();
        generateArrays(zeta);
    }

    Ntt(BigInteger n, BigInteger q) {
        this.n = n;
        this.q = q;
        computeZetaArrays();
    }
}
