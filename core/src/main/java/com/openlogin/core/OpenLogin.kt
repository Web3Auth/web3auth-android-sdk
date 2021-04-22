package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.concurrent.CompletableFuture

class OpenLogin(
    private val context: Context,
    private val clientId: String,
    private val network: Network,
    iframeUrl: String? = null,
) {
    enum class Network {
        MAINNET, TESTNET, DEVELOPMENT
    }

    private val iframeUrl: String

    init {
        if (network === Network.MAINNET) {
            this.iframeUrl = "https://app.openlogin.com";
        } else if (network === Network.TESTNET) {
            this.iframeUrl = "https://beta.openlogin.com";
        } else if (iframeUrl != null) {
            this.iframeUrl = iframeUrl
        } else {
            throw Error("Unspecified network and iframeUrl.");
        }
    }

    fun login(
        loginProvider: String? = null,
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: String? = null,
        extraLoginOptions: Any? = null
    ): CompletableFuture<Array<String>> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(iframeUrl))
        context.startActivity(intent)
        return CompletableFuture.completedFuture(arrayOf("<private key>"))
    }

    fun logout(
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: Any? = null
    ): CompletableFuture<Void> {
        // TODO: Log out
        return CompletableFuture.completedFuture(null)
    }
}
