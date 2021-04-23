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

    data class LoginOptions(
        val loginProvider: String? = null,
        val fastLogin: Boolean = false,
        val redirectUrl: String? = null,
        val appState: String? = null,
        val extraLoginOptions: Any? = null
    )

    data class LogoutOptions(
        val fastLogin: Boolean = false,
        val redirectUrl: String? = null,
        val appState: Any? = null
    )

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
        opts: LoginOptions = LoginOptions()
    ): CompletableFuture<String> {
        context.startActivity(getLoginIntent(opts))
        // TODO: Implement login
        return CompletableFuture.completedFuture("<private key>")
    }

    fun login(
        loginProvider: String? = null,
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: String? = null,
        extraLoginOptions: Any? = null
    ) = login(
        LoginOptions(
            loginProvider = loginProvider,
            fastLogin = fastLogin,
            redirectUrl = redirectUrl,
            appState = appState,
            extraLoginOptions = extraLoginOptions
        )
    )

    fun getLoginIntent(opts: LoginOptions = LoginOptions()): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(iframeUrl))
    }

    fun getLoginIntent(
        loginProvider: String? = null,
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: String? = null,
        extraLoginOptions: Any? = null
    ) = getLoginIntent(
        LoginOptions(
            loginProvider = loginProvider,
            fastLogin = fastLogin,
            redirectUrl = redirectUrl,
            appState = appState,
            extraLoginOptions = extraLoginOptions
        )
    )

    fun logout(opts: LogoutOptions = LogoutOptions()): CompletableFuture<Void> {
        // TODO: Implement logout
        return CompletableFuture.completedFuture(null)
    }

    fun logout(
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: Any? = null
    ) = logout(
        LogoutOptions(
            fastLogin = fastLogin,
            redirectUrl = redirectUrl,
            appState = appState
        )
    )
}
