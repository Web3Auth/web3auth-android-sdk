package com.web3auth.core.types

import android.net.Uri

data class LoginParams(
    val loginProvider: Provider,
    var dappShare: String? = null,
    val extraLoginOptions: ExtraLoginOptions? = null,
    val redirectUrl: Uri? = null,
    val appState: String? = null,
    val mfaLevel: MFALevel? = null,
    val sessionTime: Int? = null,
    val curve: Curve? = null
)