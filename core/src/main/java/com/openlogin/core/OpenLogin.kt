package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson

class OpenLogin(
    private val context: Context,
    params: Map<String, Any>
) {
    private val gson = Gson()

    private val init = params

    private var _state: Map<String, Any> = emptyMap()
    val state: Map<String, Any>
        get() = _state

    fun login(params: Map<String, Any> = emptyMap()) {
        val url = Uri.Builder().scheme("http")
            .encodedAuthority("10.0.2.2:3000")
            .appendPath("login.html")
            .appendQueryParameter(
                "init",
                gson.toJson(init).toByteArray(Charsets.UTF_8).toBase64URLString()
            )
            .appendQueryParameter(
                "params",
                gson.toJson(params).toByteArray(Charsets.UTF_8).toBase64URLString()
            )
            .build()
        Log.d("OpenLogin#login url", url.toString())
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    fun logout(params: Map<String, Any> = emptyMap()) {
        val url = Uri.Builder().scheme("http")
            .encodedAuthority("10.0.2.2:3000")
            .appendPath("logout.html")
            .appendQueryParameter(
                "init",
                gson.toJson(init).toByteArray(Charsets.UTF_8).toBase64URLString()
            )
            .appendQueryParameter(
                "params",
                gson.toJson(params).toByteArray(Charsets.UTF_8).toBase64URLString()
            )
            .build()
        Log.d("OpenLogin#logout url", url.toString())
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }
}
