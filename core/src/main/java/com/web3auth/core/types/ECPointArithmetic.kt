package com.web3auth.core.types

import java.math.BigInteger
import java.security.spec.ECFieldFp
import java.security.spec.EllipticCurve

// http://stackoverflow.com/questions/11190860/point-multiplication-in-elliptic-curvves
class ECPointArithmetic(
    var ec: EllipticCurve,
    private val x: BigInteger,
    private val y: BigInteger,
    z: BigInteger?
) {
    var ef: ECFieldFp = ec.field as ECFieldFp
    private var z: BigInteger? = null
    private var zinv: BigInteger?
    private val one = BigInteger.ONE
    private val zero = BigInteger.ZERO
    private var infinity: Boolean

    fun getX(): BigInteger {
        if (zinv == null) {
            zinv = z!!.modInverse(ef.p)
        }
        return x.multiply(zinv).mod(ef.p)
    }

    fun getY(): BigInteger {
        if (zinv == null) {
            zinv = z!!.modInverse(ef.p)
        }
        return y.multiply(zinv).mod(ef.p)
    }

    fun pointEquals(other: ECPointArithmetic): Boolean {
        if (other === this) {
            return true
        }
        if (isInfinity()) {
            return other.isInfinity()
        }
        if (other.isInfinity()) {
            return isInfinity()
        }
        val u: BigInteger
        val v: BigInteger
        // u = Y2 * Z1 - Y1 * Z2
        u = other.y.multiply(z).subtract(y.multiply(other.z)).mod(ef.p)
        if (u != BigInteger.ZERO) {
            return false
        }
        // v = X2 * Z1 - X1 * Z2
        v = other.x.multiply(z).subtract(x.multiply(other.z)).mod(ef.p)
        return v == BigInteger.ZERO
    }

    fun isInfinity(): Boolean {
        return if (x === zero && y === zero) {
            true
        } else z == BigInteger.ZERO && y != BigInteger.ZERO
    }

    fun negate(): ECPointArithmetic {
        return ECPointArithmetic(ec, x, y.negate(), z)
    }

    fun add(b: ECPointArithmetic): ECPointArithmetic {
        if (isInfinity()) {
            return b
        }
        if (b.isInfinity()) {
            return this
        }
        val R = ECPointArithmetic(ec, zero, zero, null)
        // u = Y2 * Z1 - Y1 * Z2
        val u = b.y.multiply(z).subtract(y.multiply(b.z)).mod(ef.p)
        // v = X2 * Z1 - X1 * Z2
        val v = b.x.multiply(z).subtract(x.multiply(b.z)).mod(ef.p)
        if (BigInteger.ZERO == v) {
            if (BigInteger.ZERO == u) {
                return twice() // this == b, so double
            }
            infinity = true // this = -b, so infinity
            return R
        }
        val THREE = BigInteger("3")
        val x1 = x
        val y1 = y
        val x2 = b.x
        val y2 = b.y
        val v2 = v.pow(2)
        val v3 = v2.multiply(v)
        val x1v2 = x1.multiply(v2)
        val zu2 = u.pow(2).multiply(z)

        // x3 = v * (z2 * (z1 * u^2 - 2 * x1 * v^2) - v^3)
        val x3 = zu2.subtract(x1v2.shiftLeft(1)).multiply(b.z).subtract(v3).multiply(v).mod(ef.p)

        // y3 = z2 * (3 * x1 * u * v^2 - y1 * v^3 - z1 * u^3) + u * v^3
        val y3 =
            x1v2.multiply(THREE).multiply(u).subtract(y1.multiply(v3)).subtract(zu2.multiply(u))
                .multiply(b.z).add(u.multiply(v3)).mod(
                    ef.p
                )

        // z3 = v^3 * z1 * z2
        val z3 = v3.multiply(z).multiply(b.z).mod(ef.p)
        return ECPointArithmetic(ec, x3, y3, z3)
    }

    fun twice(): ECPointArithmetic {
        if (isInfinity()) {
            return this
        }
        val R = ECPointArithmetic(ec, zero, zero, null)
        if (y.signum() == 0) {
            infinity = true
            return R
        }
        val THREE = BigInteger("3")
        val x1 = x
        val y1 = y
        val y1z1 = y1.multiply(z)
        val y1sqz1 = y1z1.multiply(y1).mod(ef.p)
        val a = ec.a

        // w = 3 * x1^2 + a * z1^2
        var w = x1.pow(2).multiply(THREE)
        if (BigInteger.ZERO != a) {
            w = w.add(z!!.pow(2).multiply(a))
        }
        w = w.mod(ef.p)

        // x3 = 2 * y1 * z1 * (w^2 - 8 * x1 * y1^2 * z1)
        val x3 =
            w.pow(2).subtract(x1.shiftLeft(3).multiply(y1sqz1)).shiftLeft(1).multiply(y1z1).mod(
                ef.p
            )

        // y3 = 4 * y1^2 * z1 * (3 * w * x1 - 2 * y1^2 * z1) - w^3
        val y3 = w.multiply(THREE).multiply(x1).subtract(y1sqz1.shiftLeft(1)).shiftLeft(2)
            .multiply(y1sqz1).subtract(w.pow(2).multiply(w)).mod(
                ef.p
            )

        // z3 = 8 * (y1 * z1)^3
        val z3 = y1z1.pow(2).multiply(y1z1).shiftLeft(3).mod(ef.p)
        return ECPointArithmetic(ec, x3, y3, z3)
    }

    fun multiply(k: BigInteger): ECPointArithmetic {
        if (isInfinity()) {
            return this
        }
        var R = ECPointArithmetic(ec, zero, zero, null)
        if (k.signum() == 0) {
            infinity = true
            return R
        }
        val h = k.multiply(BigInteger("3"))
        val neg = negate()
        R = this
        var i: Int
        i = h.bitLength() - 2
        while (i > 0) {
            R = R.twice()
            val hBit = h.testBit(i)
            val eBit = k.testBit(i)
            if (hBit != eBit) {
                R = R.add(if (hBit) this else neg)
            }
            --i
        }
        return R
    }

    init {
        // Projective coordinates: either zinv == null or z * zinv == 1
        // z and zinv are just BigIntegers, not fieldElements
        if (z == null) {
            this.z = BigInteger.ONE
        } else {
            this.z = z
        }
        zinv = null
        infinity = false
    }
}