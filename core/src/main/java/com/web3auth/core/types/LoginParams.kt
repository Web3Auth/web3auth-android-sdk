package com.web3auth.core.types

import android.net.Uri
import com.web3auth.core.OpenLogin

data class LoginParams (
    val loginProvider: OpenLogin.Provider,
    val relogin: Boolean? = null,
    val skipTKey: Boolean? = null,
    val extraLoginOptions: ExtraLoginOptions? = null,
    val redirectUrl: Uri? = null,
    val appState: String? = null
)