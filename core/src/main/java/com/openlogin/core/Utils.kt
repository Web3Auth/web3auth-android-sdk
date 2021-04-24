package com.openlogin.core

import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom

internal val secureRandom = SecureRandom()
internal val gson = Gson()

internal fun randomPid(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return Hex.encodeHexString(bytes)
}

internal fun Gson.toBase64URLSafeString(src: Any): String =
    Base64.encodeBase64URLSafeString(toJson(src).toByteArray())