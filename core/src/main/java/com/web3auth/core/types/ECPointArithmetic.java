package com.web3auth.core.types;

import java.math.BigInteger;
import java.security.spec.ECFieldFp;
import java.security.spec.EllipticCurve;

// http://stackoverflow.com/questions/11190860/point-multiplication-in-elliptic-curvves
public class ECPointArithmetic {
    EllipticCurve ec;
    ECFieldFp ef;
    private final BigInteger x;
    private final BigInteger y;
    private final BigInteger z;
    private BigInteger zinv;
    private final BigInteger one = BigInteger.ONE;
    private final BigInteger zero = BigInteger.ZERO;
    private boolean infinity;

    public ECPointArithmetic(EllipticCurve ec, BigInteger x, BigInteger y, BigInteger z) {
        this.ec = ec;
        this.x = x;
        this.y = y;
        this.ef = (ECFieldFp) ec.getField();

        // Projective coordinates: either zinv == null or z * zinv == 1
        // z and zinv are just BigIntegers, not fieldElements
        if (z == null) {
            this.z = BigInteger.ONE;
        } else {
            this.z = z;
        }
        this.zinv = null;
        infinity = false;
    }

    public BigInteger getX() {
        if (this.zinv == null) {
            this.zinv = this.z.modInverse(this.ef.getP());
        }
        return this.x.multiply(this.zinv).mod(this.ef.getP());
    }

    public BigInteger getY() {
        if (this.zinv == null) {
            this.zinv = this.z.modInverse(this.ef.getP());
        }
        return this.y.multiply(this.zinv).mod(this.ef.getP());
    }

    public boolean pointEquals(ECPointArithmetic other) {
        if (other == this) {
            return true;
        }
        if (this.isInfinity()) {
            return other.isInfinity();
        }
        if (other.isInfinity()) {
            return this.isInfinity();
        }
        BigInteger u, v;
        // u = Y2 * Z1 - Y1 * Z2
        u = other.y.multiply(this.z).subtract(this.y.multiply(other.z)).mod(this.ef.getP());
        if (!u.equals(BigInteger.ZERO)) {
            return false;
        }
        // v = X2 * Z1 - X1 * Z2
        v = other.x.multiply(this.z).subtract(this.x.multiply(other.z)).mod(this.ef.getP());
        return v.equals(BigInteger.ZERO);
    }

    public boolean isInfinity() {

        if ((this.x == zero) && (this.y == zero)) {
            return true;
        }
        return this.z.equals(BigInteger.ZERO) && !this.y.equals(BigInteger.ZERO);

    }

    public ECPointArithmetic negate() {
        return new ECPointArithmetic(this.ec, this.x, this.y.negate(), this.z);
    }

    public ECPointArithmetic add(ECPointArithmetic b) {
        if (this.isInfinity()) {
            return b;
        }
        if (b.isInfinity()) {
            return this;
        }
        ECPointArithmetic R = new ECPointArithmetic(this.ec, zero, zero, null);
        // u = Y2 * Z1 - Y1 * Z2
        BigInteger u = b.y.multiply(this.z).subtract(this.y.multiply(b.z)).mod(this.ef.getP());
        // v = X2 * Z1 - X1 * Z2
        BigInteger v = b.x.multiply(this.z).subtract(this.x.multiply(b.z)).mod(this.ef.getP());

        if (BigInteger.ZERO.equals(v)) {
            if (BigInteger.ZERO.equals(u)) {
                return this.twice(); // this == b, so double
            }

            infinity = true; // this = -b, so infinity
            return R;
        }

        BigInteger THREE = new BigInteger("3");
        BigInteger x1 = this.x;
        BigInteger y1 = this.y;
        BigInteger x2 = b.x;
        BigInteger y2 = b.y;

        BigInteger v2 = v.pow(2);
        BigInteger v3 = v2.multiply(v);
        BigInteger x1v2 = x1.multiply(v2);
        BigInteger zu2 = u.pow(2).multiply(this.z);

        // x3 = v * (z2 * (z1 * u^2 - 2 * x1 * v^2) - v^3)
        BigInteger x3 = zu2.subtract(x1v2.shiftLeft(1)).multiply(b.z).subtract(v3).multiply(v).mod(this.ef.getP());

        // y3 = z2 * (3 * x1 * u * v^2 - y1 * v^3 - z1 * u^3) + u * v^3
        BigInteger y3 = x1v2.multiply(THREE).multiply(u).subtract(y1.multiply(v3)).subtract(zu2.multiply(u)).multiply(b.z).add(u.multiply(v3)).mod(this.ef.getP());

        // z3 = v^3 * z1 * z2
        BigInteger z3 = v3.multiply(this.z).multiply(b.z).mod(this.ef.getP());

        return new ECPointArithmetic(this.ec, x3, y3, z3);
    }

    public ECPointArithmetic twice() {
        if (this.isInfinity()) {
            return this;
        }
        ECPointArithmetic R = new ECPointArithmetic(this.ec, zero, zero, null);
        if (this.y.signum() == 0) {
            infinity = true;
            return R;
        }

        BigInteger THREE = new BigInteger("3");
        BigInteger x1 = this.x;
        BigInteger y1 = this.y;

        BigInteger y1z1 = y1.multiply(this.z);
        BigInteger y1sqz1 = y1z1.multiply(y1).mod(this.ef.getP());
        BigInteger a = this.ec.getA();

        // w = 3 * x1^2 + a * z1^2
        BigInteger w = x1.pow(2).multiply(THREE);

        if (!BigInteger.ZERO.equals(a)) {
            w = w.add(this.z.pow(2).multiply(a));
        }

        w = w.mod(this.ef.getP());

        // x3 = 2 * y1 * z1 * (w^2 - 8 * x1 * y1^2 * z1)
        BigInteger x3 = w.pow(2).subtract(x1.shiftLeft(3).multiply(y1sqz1)).shiftLeft(1).multiply(y1z1).mod(this.ef.getP());

        // y3 = 4 * y1^2 * z1 * (3 * w * x1 - 2 * y1^2 * z1) - w^3
        BigInteger y3 = (w.multiply(THREE).multiply(x1).subtract(y1sqz1.shiftLeft(1))).shiftLeft(2).multiply(y1sqz1).subtract(w.pow(2).multiply(w)).mod(this.ef.getP());

        // z3 = 8 * (y1 * z1)^3
        BigInteger z3 = y1z1.pow(2).multiply(y1z1).shiftLeft(3).mod(this.ef.getP());

        return new ECPointArithmetic(this.ec, x3, y3, z3);
    }

    public ECPointArithmetic multiply(BigInteger k) {
        if (this.isInfinity()) {
            return this;
        }

        ECPointArithmetic R = new ECPointArithmetic(this.ec, zero, zero, null);
        if (k.signum() == 0) {
            infinity = true;
            return R;
        }

        BigInteger e = k;
        BigInteger h = e.multiply(new BigInteger("3"));

        ECPointArithmetic neg = this.negate();
        R = this;

        int i;
        for (i = h.bitLength() - 2; i > 0; --i) {
            R = R.twice();
            boolean hBit = h.testBit(i);
            boolean eBit = e.testBit(i);

            if (hBit != eBit) {
                R = R.add(hBit ? this : neg);
            }
        }

        return R;
    }
}