package com.web3auth.core.types

data class InitParams(
    val loginProvider: String? = null,
    val extraLoginOptions: String? = null,
    val redirectUrl: String,
    val mfaLevel: String? = null,
    val curve: String? = null,
    val dappShare: String? = null
)