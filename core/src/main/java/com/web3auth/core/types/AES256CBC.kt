package com.web3auth.core.types

import java.math.BigInteger
import java.security.Key
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECFieldFp
import java.security.spec.EllipticCurve
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AES256CBC(privateKeyHex: String?, ephemPublicKeyHex: String?, encryptionIvHex: String) {

    private val AES_ENCRYPTION_KEY: ByteArray
    private val ENCRYPTION_IV: ByteArray

    @Throws(TorusException::class)
    fun encrypt(src: ByteArray?): String {
        val cipher: Cipher
        return try {
            cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, makeKey(), makeIv())
            Base64.encodeBytes(cipher.doFinal(src))

        } catch (e: Exception) {
            e.printStackTrace()
            throw TorusException("Torus Internal Error", e)
        }
    }

    @Throws(TorusException::class)
    fun decrypt(src: String?): ByteArray {
        val cipher: Cipher
        return try {
            cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, makeKey(), makeIv())
            cipher.doFinal(Base64.decode(src))
        } catch (e: Exception) {
            e.printStackTrace()
            throw TorusException("Torus Internal Error", e)
        }
    }

    private fun ecdh(privateKeyHex: String?, ephemPublicKeyHex: String?): BigInteger {
        val affineX = ephemPublicKeyHex?.substring(2, 66)
        val affineY = ephemPublicKeyHex?.substring(66)
        val ecPoint = ECPointArithmetic(
            EllipticCurve(
                ECFieldFp(BigInteger("115792089237316195423570985008687907853269984665640564039457584007908834671663")),
                BigInteger("0"),
                BigInteger("7")
            ), BigInteger(affineX, 16), BigInteger(affineY, 16), null
        )
        return ecPoint.multiply(BigInteger(privateKeyHex, 16)).getX()
    }

    private fun makeKey(): Key {
        return SecretKeySpec(AES_ENCRYPTION_KEY, "AES")
    }

    private fun makeIv(): AlgorithmParameterSpec {
        return IvParameterSpec(ENCRYPTION_IV)
    }

    companion object {
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

        /**
         * Utility method to convert a BigInteger to a byte array in unsigned
         * format as needed in the handshake messages. BigInteger uses
         * 2's complement format, i.e. it prepends an extra zero if the MSB
         * is set. We remove that.
         */
        fun toByteArray(bi: BigInteger): ByteArray {
            var b = bi.toByteArray()
            if (b.size > 1 && b[0].compareTo(1) == 0) {
                val n = b.size - 1
                val newArray = ByteArray(n)
                System.arraycopy(b, 1, newArray, 0, n)
                b = newArray
            }
            return b
        }

        fun toByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((s[i].digitToIntOrNull(16) ?: -1 shl 4)
                + s[i + 1].digitToIntOrNull(16)!! ?: -1).toByte()
                i += 2
            }
            return data
        }
    }

    init {
        val hash: ByteArray = SHA512.digest(toByteArray(ecdh(privateKeyHex, ephemPublicKeyHex)))
        val encKeyBytes = hash.copyOfRange(0, 32)
        AES_ENCRYPTION_KEY = encKeyBytes
        ENCRYPTION_IV = toByteArray(encryptionIvHex)
    }
}