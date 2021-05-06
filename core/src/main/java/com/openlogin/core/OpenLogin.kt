package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import java.util.*

class OpenLogin(
    private val context: Context,
    clientId: String,
    network: Network,
    redirectUrl: Uri? = null,
    resultUrl: Uri? = null,
    sdkUrl: String = "https://sdk.openlogin.com",
) {
    enum class Network {
        TESTNET, MAINNET
    }

    enum class Provider {
        GOOGLE, FACEBOOK, REDDIT, DISCORD, TWITCH, APPLE, LINE, GITHUB, KAKAO, LINKEDIN, TWITTER, WEIBO, WECHAT, EMAIL_PASSWORDLESS, WEBAUTHN
    }

    data class State(
        val privKey: String? = null,
        val walletKey: String? = null,
        val tKey: String? = null,
        val oAuthPrivateKey: String? = null
    )

    private val gson = Gson()

    private val sdkUrl = Uri.parse(sdkUrl)
    private val initParams: Map<String, Any>

    private var _state = State()
    val state
        get() = _state

    init {
        // Build init params
        val initParams = mutableMapOf(
            "clientId" to clientId,
            "network" to network.name.toLowerCase(Locale.ROOT)
        )
        if (redirectUrl != null) initParams["redirectUrl"] = redirectUrl.toString()
        this.initParams = initParams

        // Parse result hash
        val hash = resultUrl?.fragment
        if (hash != null) {
            _state = gson.fromJson(
                decodeBase64URLString(hash).toString(Charsets.UTF_8),
                State::class.java
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

        val customTabsPackages = context.getCustomTabsPackages()
        if (customTabsPackages.isNotEmpty()) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.launchUrl(context, url)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        }
    }

    fun login(params: Map<String, Any>? = null) {
        request("login", params)
    }

    fun login(
        loginProvider: Provider,
        fastLogin: Boolean? = null,
        relogin: Boolean? = null,
        skipTKey: Boolean? = null,
        getWalletKey: Boolean? = null,
        extraLoginOptions: Map<String, Any>? = null,
        redirectUrl: Uri? = null,
        appState: String? = null,
    ) {
        val params = mutableMapOf<String, Any>(
            "loginProvider" to loginProvider.name.toLowerCase(Locale.ROOT),
        )
        if (fastLogin != null) params["fastLogin"] = fastLogin
        if (relogin != null) params["relogin"] = relogin
        if (skipTKey != null) params["skipTKey"] = skipTKey
        if (getWalletKey != null) params["getWalletKey"] = getWalletKey
        if (extraLoginOptions != null) params["extraLoginOptions"] = extraLoginOptions
        if (redirectUrl != null) params["redirectUrl"] = redirectUrl.toString()
        if (appState != null) params["appState"] = appState
        login(params)
    }

    fun logout(params: Map<String, Any>? = null) {
        request("logout", params)
    }

    fun logout(
        fastLogin: Boolean? = null,
        redirectUrl: Uri? = null,
        appState: String? = null
    ) {
        val params = mutableMapOf<String, Any>()
        if (fastLogin != null) params["fastLogin"] = fastLogin
        if (redirectUrl != null) params["redirectUrl"] = redirectUrl.toString()
        if (appState != null) params["appState"] = appState
        logout(params)
    }
}
