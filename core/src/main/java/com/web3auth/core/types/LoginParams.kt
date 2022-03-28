package com.web3auth.core.types

import android.net.Uri
import com.web3auth.core.Web3Auth

data class LoginParams (
    val loginProvider: Provider,
    val relogin: Boolean? = null,
    val dappShare: String? = null,
    val extraLoginOptions: ExtraLoginOptions? = null,
    val redirectUrl: Uri? = null,
    val appState: String? = null
)