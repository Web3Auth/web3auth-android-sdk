package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
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
        object Method {
            const val LOGIN = "openlogin_login"
            const val LOGOUT = "openlogin_logout"
        }
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
        val pid = randomId()

        val origin = Uri.Builder().scheme(redirectUrl.scheme)
            .encodedAuthority(redirectUrl.encodedAuthority)
            .toString()

        val params = mapOf(
            "loginProvider" to loginProvider,
            "redirectUrl" to redirectUrl.toString(),
            "_clientId" to clientId,
            "_origin" to origin,
            "_originData" to emptyMap<String, Nothing>()
        )

        val hash = Uri.Builder().scheme(iframeUrl.scheme)
            .encodedAuthority(iframeUrl.encodedAuthority)
            .encodedPath(iframeUrl.encodedPath)
            .appendPath("start")
            .appendQueryParameter("b64Params", params.toBase64URLSafeString())
            .appendQueryParameter("_pid", pid)
            .appendQueryParameter("_method", Method.LOGIN)
            .build().encodedQuery ?: ""

        val url = Uri.Builder().scheme(iframeUrl.scheme)
            .encodedAuthority(iframeUrl.encodedAuthority)
            .encodedPath(iframeUrl.encodedPath)
            .appendPath("start")
            .encodedFragment(hash)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, url)
        context.startActivity(intent)

        return CompletableFuture.completedFuture("<private key>")
    }

    fun logout(): CompletableFuture<Void> {
        // TODO: Implement logout
        return CompletableFuture.completedFuture(null)
    }
}
