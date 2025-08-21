package protocol;

import java.math.BigInteger;
import java.util.*;

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

    Polynomial generateConstantPolynomialNtt(BigInteger c) {
        BigInteger[] coeffs = new BigInteger[n.intValue()];  // !!! Conversion to int
        Arrays.fill(coeffs, c);
        return new Polynomial(coeffs);
    }

    Polynomial add(Polynomial a, Polynomial b) {
        BigInteger[] resultingCoeffs = new BigInteger[n.intValue()];  // !!! Conversion to int
        for (int i = 0; i < n.intValue(); i = i + 1) {  // !!! Conversion to int
            resultingCoeffs[i] = a.getCoeffIndex(i).add(b.getCoeffIndex(i)).mod(q);
        }
        return new Polynomial(resultingCoeffs);
    }

    Polynomial inverse(Polynomial a) {
        BigInteger[] resultingCoeffs = new BigInteger[n.intValue()];  // !!! Conversion to int
        for (int i = 0; i < n.intValue(); i = i + 1) {  // !!! Conversion to int
            resultingCoeffs[i] = a.getCoeffIndex(i).negate().mod(q);
        }
        return new Polynomial(resultingCoeffs);
    }

    Polynomial sub(Polynomial a, Polynomial b) {
        return add(a, inverse(b));
    }

    Polynomial convertToNtt(Polynomial inputPoly) {
        Polynomial polyNtt = new Polynomial(inputPoly.getCoeffs());
        int zetaIndex = 0;

        for (BigInteger layer = BigInteger.ZERO; layer.compareTo(BigInteger.valueOf(n.bitLength() - 1)) < 0; layer = layer.add(BigInteger.ONE)) {
            BigInteger numOfSubpolys = BigInteger.TWO.pow(layer.intValue());  // !!! Conversion to int
            BigInteger lenOfSubpoly = n.divide(numOfSubpolys);
            for (BigInteger subpolyCounter = BigInteger.ZERO; subpolyCounter.compareTo(numOfSubpolys) < 0; subpolyCounter = subpolyCounter.add(BigInteger.ONE)) {
                BigInteger polyLstIndex = subpolyCounter.multiply(lenOfSubpoly).subtract(BigInteger.ONE);
                BigInteger limit = lenOfSubpoly.divide(BigInteger.TWO).add(polyLstIndex).add(BigInteger.ONE);
                for (BigInteger subpolyIndex = polyLstIndex.add(BigInteger.ONE); subpolyIndex.compareTo(limit) < 0; subpolyIndex = subpolyIndex.add(BigInteger.ONE)) {
                    BigInteger subpolyHalfIndex = lenOfSubpoly.divide(BigInteger.TWO).add(subpolyIndex);
                    BigInteger oldSubpolyCoeff = polyNtt.getCoeffIndex(subpolyIndex.intValue());
                    BigInteger oldSubpolyHalfCoeff = polyNtt.getCoeffIndex(subpolyHalfIndex.intValue());
                    polyNtt.setCoeffIndex(subpolyIndex.intValue(), (oldSubpolyCoeff.subtract(zetas.get(zetaIndex).multiply(oldSubpolyHalfCoeff))).mod(q));
                    polyNtt.setCoeffIndex(subpolyHalfIndex.intValue(), (oldSubpolyCoeff.add(zetas.get(zetaIndex).multiply(oldSubpolyHalfCoeff))).mod(q));
                }
                zetaIndex++;
            }
        }

        return polyNtt;
    }

    Polynomial multiplyNttPolys(Polynomial a, Polynomial b) {
        BigInteger[] resultingCoeffs = new BigInteger[n.intValue()];  // !!! Conversion to int
        for (int i = 0; i < n.intValue(); i = i + 1) {  // !!! Conversion to int
            resultingCoeffs[i] = a.getCoeffIndex(i).multiply(b.getCoeffIndex(i)).mod(q);
        }
        return new Polynomial(resultingCoeffs);
    }

    Polynomial convertFromNtt(Polynomial inputPoly) {
        Polynomial poly = new Polynomial(inputPoly.getCoeffs());
        int zetaIndex = zetasInverted.size() - 1;

        BigInteger numOfLayers = BigInteger.valueOf(n.bitLength() - 1);
        for (BigInteger layer = numOfLayers.subtract(BigInteger.ONE); layer.compareTo(BigInteger.ZERO) >= 0; layer = layer.subtract(BigInteger.ONE)) {
            BigInteger numOfSubpolys = BigInteger.TWO.pow(layer.intValue());  // !!! Conversion to int
            BigInteger lenOfSubpoly = n.divide(numOfSubpolys);
            for (BigInteger subpolyCounter = numOfSubpolys.subtract(BigInteger.ONE); subpolyCounter.compareTo(BigInteger.ZERO) >= 0; subpolyCounter = subpolyCounter.subtract(BigInteger.ONE)) {
                BigInteger polyLstIndex = subpolyCounter.multiply(lenOfSubpoly).add(lenOfSubpoly);
                BigInteger limit = polyLstIndex.subtract(BigInteger.ONE).subtract(lenOfSubpoly.divide(BigInteger.TWO));
                for (BigInteger subpolyHalfIndex = polyLstIndex.subtract(BigInteger.ONE); subpolyHalfIndex.compareTo(limit) > 0; subpolyHalfIndex = subpolyHalfIndex.subtract(BigInteger.ONE)) {
                    BigInteger subpolyIndex = subpolyHalfIndex.subtract(lenOfSubpoly.divide(BigInteger.TWO));
                    BigInteger oldSubpolyCoeff = poly.getCoeffIndex(subpolyIndex.intValue());
                    BigInteger oldSubpolyHalfCoeff = poly.getCoeffIndex(subpolyHalfIndex.intValue());
                    poly.setCoeffIndex(subpolyIndex.intValue(), oldSubpolyCoeff.add(oldSubpolyHalfCoeff).mod(q));
                    poly.setCoeffIndex(subpolyHalfIndex.intValue(), zetasInverted.get(zetaIndex).negate().multiply(oldSubpolyCoeff.subtract(oldSubpolyHalfCoeff)).mod(q));
                }
                zetaIndex--;
            }
        }

        BigInteger twoDivisor = BigInteger.TWO.modPow(numOfLayers.negate(), q);
        for (int i = 0; i < n.intValue(); i = i + 1) {  // !!! Conversion to int
            poly.setCoeffIndex(i, poly.getCoeffIndex(i).multiply(twoDivisor).mod(q));
        }
        return poly;
    }
}
