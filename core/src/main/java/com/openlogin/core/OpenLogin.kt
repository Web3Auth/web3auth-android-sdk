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
        when {
            network === Network.MAINNET -> {
                this.iframeUrl = "https://app.openlogin.com";
            }
            network === Network.TESTNET -> {
                this.iframeUrl = "https://beta.openlogin.com";
            }
            iframeUrl != null -> {
                this.iframeUrl = iframeUrl
            }
            else -> throw Error("Unspecified network and iframeUrl");
        }
    }

    fun login(
        loginProvider: String? = null,
    ): CompletableFuture<String> {
        // TODO: Implement login
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(iframeUrl))
        context.startActivity(intent)
        return CompletableFuture.completedFuture("<private key>")
    }

    fun logout(): CompletableFuture<Void> {
        // TODO: Implement logout
        return CompletableFuture.completedFuture(null)
    }
}
