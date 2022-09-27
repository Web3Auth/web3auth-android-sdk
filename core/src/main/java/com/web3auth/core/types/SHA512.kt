package com.web3auth.core.types

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object SHA512 {
    @Throws(NoSuchAlgorithmException::class)
    fun digest(buf: ByteArray?): ByteArray {
        val digest = MessageDigest.getInstance("SHA-512")
        digest.update(buf)
        return digest.digest()
    }
}