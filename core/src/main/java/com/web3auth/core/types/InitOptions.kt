package com.web3auth.core.types

data class InitOptions(
    val clientId: String,
    val network: String,
    var redirectUrl: String? = null,
    val whiteLabel: String? = null,
    val loginConfig: String? = null,
    val buildEnv: String? = null,
    val mfaSettings: String? = null,
    val sessionTime: Int? = null,
    val originData: String? = null,
    val dashboardUrl: String? = null,
)