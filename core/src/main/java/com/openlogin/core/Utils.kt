package com.openlogin.core

import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom

val secureRandom = SecureRandom()

fun randomPid(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return Hex.encodeHexString(bytes)
}
