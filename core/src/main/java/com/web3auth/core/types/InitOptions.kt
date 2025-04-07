package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class InitOptions(
    @Keep val clientId: String,
    @Keep val network: String,
    @Keep var redirectUrl: String? = null,
    @Keep val whiteLabel: String? = null,
    @Keep val loginConfig: String? = null,
    @Keep val buildEnv: String? = null,
    @Keep val mfaSettings: String? = null,
    @Keep val sessionTime: Int? = null,
    @Keep val originData: String? = null,
    @Keep val dashboardUrl: String? = null,
)