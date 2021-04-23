package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import java.util.concurrent.CompletableFuture

class OpenLogin(
    private val context: Context,
    private val clientId: String,
    private val network: Network,
    redirectUrl: String,
    iframeUrl: String? = null,
) {
    enum class Network {
        MAINNET, TESTNET, DEVELOPMENT
    }

    companion object {
        private val gson = Gson()
    }

    private val iframeUrl: Uri
    private val redirectUrl: Uri

    init {
        when {
            network === Network.MAINNET -> {
                this.iframeUrl = Uri.parse("https://app.openlogin.com");
            }
            network === Network.TESTNET -> {
                this.iframeUrl = Uri.parse("https://beta.openlogin.com");
            }
            iframeUrl != null -> {
                this.iframeUrl = Uri.parse(iframeUrl)
            }
            else -> throw Error("Unspecified network and iframeUrl");
        }
        this.redirectUrl = Uri.parse(redirectUrl)
    }

    fun login(loginProvider: String): CompletableFuture<String> {
        val pid = randomPid()
        val params = mapOf(
            "_clientId" to clientId,
            "_origin" to redirectUrl.authority,
            "_originData" to mapOf(redirectUrl.authority to ""),
            "redirectUrl" to redirectUrl.toString(),
            "loginProvider" to loginProvider,
        )
        val hash =
            Uri.Builder().scheme(iframeUrl.scheme).authority(iframeUrl.authority).path("/start")
                .appendQueryParameter("_pid", pid)
                .appendQueryParameter("_method", "openlogin_login")
                .appendQueryParameter(
                    "b64Params",
                    Base64.encodeBase64URLSafeString(gson.toJson(params).toByteArray())
                ).build()
        val url =
            Uri.Builder().scheme(iframeUrl.scheme).authority(iframeUrl.authority).path("/start")
                .encodedFragment(hash.encodedQuery).build()
        val intent = Intent(Intent.ACTION_VIEW, url)
        context.startActivity(intent)
        return CompletableFuture.completedFuture("<private key>")
    }

    fun logout(): CompletableFuture<Void> {
        // TODO: Implement logout
        return CompletableFuture.completedFuture(null)
    }
}
