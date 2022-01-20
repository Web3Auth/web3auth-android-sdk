package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.openlogin.core.types.*
import java.util.*
import java8.util.concurrent.CompletableFuture

class OpenLogin(openLoginOptions: OpenLoginOptions) {
    enum class Network {
        MAINNET, TESTNET
    }

    enum class Provider {
        GOOGLE, FACEBOOK, REDDIT, DISCORD, TWITCH, APPLE, LINE, GITHUB, KAKAO, LINKEDIN, TWITTER, WEIBO, WECHAT, EMAIL_PASSWORDLESS
    }

    private val gson = Gson()

    private val sdkUrl = Uri.parse(openLoginOptions.sdkUrl)
    private val initParams: Map<String, Any>
    private val context : Context

    private var loginCompletableFuture: CompletableFuture<OpenLoginResponse> = CompletableFuture()
    private var logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()

    private var openLoginResponse = OpenLoginResponse()

    init {
        // Build init params
        val initParams = mutableMapOf(
            "clientId" to openLoginOptions.clientId,
            "network" to openLoginOptions.network.name.lowercase(Locale.ROOT)
        )
        if (openLoginOptions.redirectUrl != null) initParams["redirectUrl"] = openLoginOptions.redirectUrl.toString()
        this.initParams = initParams
        this.context = openLoginOptions.context
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


        val defaultBrowser = context.getDefaultBrowser()
        val customTabsBrowsers = context.getCustomTabsBrowsers()

        if (customTabsBrowsers.contains(defaultBrowser)) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(defaultBrowser)
            customTabs.launchUrl(context, url)
        } else if (customTabsBrowsers.isNotEmpty()) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(customTabsBrowsers[0])
            customTabs.launchUrl(context, url)
        } else {
            // Open in browser externally
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        }
    }

    fun setResultUrl(uri: Uri?) {
        val hash = uri?.fragment
        if (hash == null) {
            loginCompletableFuture.completeExceptionally(UserCancelledException())
            return
        }
        val error = uri.getQueryParameter("error")
        if (error != null) {
            loginCompletableFuture.completeExceptionally(UnKnownException(error))
        }

        openLoginResponse = gson.fromJson(
            decodeBase64URLString(hash).toString(Charsets.UTF_8),
            OpenLoginResponse::class.java
        )
        if (openLoginResponse.error?.isNotBlank() == true ) {
            loginCompletableFuture.completeExceptionally(UnKnownException(openLoginResponse.error ?: "Something went wrong"))
        }

        if (openLoginResponse.privKey.isNullOrBlank()) {
            logoutCompletableFuture.complete(null)
        }

        loginCompletableFuture.complete(openLoginResponse)
    }

    fun login(loginParams: LoginParams) : CompletableFuture<OpenLoginResponse> {
        val params = mutableMapOf<String, Any>(
            "loginProvider" to loginParams.loginProvider.name.lowercase(Locale.ROOT),
        )
        if (loginParams.relogin != null) params["relogin"] = loginParams.relogin
        if (loginParams.skipTKey != null) params["skipTKey"] = loginParams.skipTKey
        if (loginParams.extraLoginOptions != null) params["extraLoginOptions"] = loginParams.extraLoginOptions
        if (loginParams.redirectUrl != null) params["redirectUrl"] = loginParams.redirectUrl.toString()
        if (loginParams.appState != null) params["appState"] = loginParams.appState
        request("login", params)

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

    fun logout(params: Map<String, Any>? = null) : CompletableFuture<Void> {
        request("logout", params)

        logoutCompletableFuture = CompletableFuture()
        return logoutCompletableFuture
    }

    fun logout(
        redirectUrl: Uri? = null,
        appState: String? = null
    ) {
        val params = mutableMapOf<String, Any>()
        if (redirectUrl != null) params["redirectUrl"] = redirectUrl.toString()
        if (appState != null) params["appState"] = appState
        logout(params)
    }
}
