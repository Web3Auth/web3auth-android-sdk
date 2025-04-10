package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class InitParams(
    @Keep val authConnection: String? = null,
    @Keep val extraLoginOptions: String? = null,
    @Keep val redirectUrl: String,
    @Keep val mfaLevel: String? = null,
    @Keep val curve: String? = null,
    @Keep val dappShare: String? = null,
    @Keep val appState: String? = null,
    @Keep val dappUrl: String? = null,
)