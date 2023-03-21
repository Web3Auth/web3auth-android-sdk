package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class Web3AuthResponse(
    var privKey: String? = null,
    var ed25519PrivKey: String? = null,
    val userInfo: UserInfo? = null,
    val error: String? = null,
    val sessionId: String? = null,
    val coreKitKey: String? = null,
    val coreKitEd25519PrivKey: String? = null
)