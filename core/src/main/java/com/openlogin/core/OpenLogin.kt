package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson

class OpenLogin(
    private val context: Context,
    params: Map<String, Any>
) {
    private val gson = Gson()

    private val sdkUrl = Uri.parse("http://10.0.2.2:3000")
    private val initParams = params

    private var _state: Map<String, Any> = emptyMap()
    val state: Map<String, Any>
        get() = _state

    fun onLoggedIn(uri: Uri) {
        val hash = uri.fragment ?: return
        _state = gson.fromJson<Map<String, Any>>(
            bytesFromBase64URLString(hash).toString(Charsets.UTF_8),
            Map::class.java
        )
    }

    fun login(params: Map<String, Any> = emptyMap()) {
        val hash = gson.toJson(
            mapOf(
                "init" to initParams,
                "params" to params
            )
        ).toByteArray(Charsets.UTF_8).toBase64URLString()
        val url = Uri.Builder().scheme(sdkUrl.scheme)
            .encodedAuthority(sdkUrl.encodedAuthority)
            .encodedPath(sdkUrl.encodedPath)
            .appendPath("login.html")
            .fragment(hash)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    fun logout(params: Map<String, Any> = emptyMap()) {
        val hash = gson.toJson(
            mapOf(
                "init" to initParams,
                "params" to params
            )
        ).toByteArray(Charsets.UTF_8).toBase64URLString()
        val url = Uri.Builder().scheme(sdkUrl.scheme)
            .encodedAuthority(sdkUrl.encodedAuthority)
            .encodedPath(sdkUrl.encodedPath)
            .appendPath("logout.html")
            .fragment(hash)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }
}
