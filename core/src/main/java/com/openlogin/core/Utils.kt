package com.openlogin.core

import android.util.Base64

private const val base64URLFlags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

fun ByteArray.toBase64URLString(): String =
    Base64.encodeToString(this, base64URLFlags)

fun bytesFromBase64URLString(src: String): ByteArray =
    Base64.decode(src, base64URLFlags)
