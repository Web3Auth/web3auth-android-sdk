package com.openlogin.core

import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom

val secureRandom = SecureRandom()
val gson = Gson()

fun randomId(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return Hex.encodeHexString(bytes)
}

fun Any.toBase64URLSafeString(): String {
    return Base64.encodeBase64URLSafeString(gson.toJson(this).toByteArray())
}