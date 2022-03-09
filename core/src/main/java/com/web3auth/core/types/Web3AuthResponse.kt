package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class Web3AuthResponse(
    val privKey: String? = null,
    val userInfo: UserInfo? = null,
    val error: String? = null
)