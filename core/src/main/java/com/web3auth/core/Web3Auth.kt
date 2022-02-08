package com.web3auth.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.web3auth.core.types.*
import java.util.*
import java8.util.concurrent.CompletableFuture

class Web3Auth(web3AuthOptions: Web3AuthOptions) {
    enum class Network {
        MAINNET, TESTNET
    }

    enum class Provider {
        @SerializedName("google")GOOGLE,
        @SerializedName("facebook")FACEBOOK,
        @SerializedName("reddit")REDDIT,
        @SerializedName("discord")DISCORD,
        @SerializedName("twitch")TWITCH,
        @SerializedName("apple")APPLE,
        @SerializedName("line")LINE,
        @SerializedName("github")GITHUB,
        @SerializedName("kakao")KAKAO,
        @SerializedName("linkedin")LINKEDIN,
        @SerializedName("twitter")TWITTER,
        @SerializedName("weibo")WEIBO,
        @SerializedName("wechat")WECHAT,
        @SerializedName("email_passwordless")EMAIL_PASSWORDLESS
    }

    private val gson = Gson()

    private val sdkUrl = Uri.parse(web3AuthOptions.sdkUrl)
    private val initParams: Map<String, Any>
    private val context : Context

    private var loginCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
    private var logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()

    private var web3AuthResponse = Web3AuthResponse()

    init {
        // Build init params
        val initParams = mutableMapOf(
            "clientId" to web3AuthOptions.clientId,
            "network" to web3AuthOptions.network.name.lowercase(Locale.ROOT)
        )
        if (web3AuthOptions.redirectUrl != null) initParams["redirectUrl"] = web3AuthOptions.redirectUrl.toString()
        this.initParams = initParams
        this.context = web3AuthOptions.context
    }

    private fun request(path: String, params: LoginParams? = null, extraParams: Map<String, Any>? = null) {
        val paramMap = mapOf(
            "init" to initParams,
            "params" to params
        )
        extraParams?.let{ paramMap.plus("params" to extraParams) }

        val hash = gson.toJson(paramMap).toByteArray(Charsets.UTF_8).toBase64URLString()

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

        web3AuthResponse = gson.fromJson(
            decodeBase64URLString(hash).toString(Charsets.UTF_8),
            Web3AuthResponse::class.java
        )
        if (web3AuthResponse.error?.isNotBlank() == true ) {
            loginCompletableFuture.completeExceptionally(UnKnownException(web3AuthResponse.error ?: "Something went wrong"))
        }

        if (web3AuthResponse.privKey.isNullOrBlank()) {
            logoutCompletableFuture.complete(null)
        }

        loginCompletableFuture.complete(web3AuthResponse)
    }

    fun login(loginParams: LoginParams) : CompletableFuture<Web3AuthResponse> {
        request("login", loginParams)

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

    fun logout(params: Map<String, Any>? = null) : CompletableFuture<Void> {
        request("logout", extraParams = params)

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
