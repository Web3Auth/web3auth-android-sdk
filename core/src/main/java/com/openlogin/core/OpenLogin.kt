package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson

class OpenLogin(
    private val context: Context,
    params: Map<String, Any>,
    resultUrl: Uri? = null,
    sdkUrl: String = "https://sdk.openlogin.com",
) {
    private val gson = Gson()

    private val sdkUrl = Uri.parse(sdkUrl)
    private val initParams = params

    private var _state: Map<String, Any> = emptyMap()
    val state: Map<String, Any>
        get() = _state

    init {
        val hash = resultUrl?.fragment
        if (hash != null) {
            _state = gson.fromJson<Map<String, Any>>(
                decodeBase64URLString(hash).toString(Charsets.UTF_8),
                Map::class.java
            )
        }
    }

    private fun request(path: String, params: Map<String, Any>?) {
        val hash = gson.toJson(
            mapOf(
                "init" to initParams,
                "params" to params
            )
        ).toByteArray(Charsets.UTF_8).toBase64URLString()
        val url = Uri.Builder().scheme(sdkUrl.scheme)
            .encodedAuthority(sdkUrl.encodedAuthority)
            .encodedPath(sdkUrl.encodedPath)
            .appendPath(path)
            .fragment(hash)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    fun login(params: Map<String, Any>? = null) {
        request("login.html", params)
    }

    fun logout(params: Map<String, Any>? = null) {
        request("logout.html", params)
    }
}
