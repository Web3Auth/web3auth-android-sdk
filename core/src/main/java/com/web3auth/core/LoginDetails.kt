package com.web3auth.core

import androidx.annotation.Keep
import com.web3auth.core.types.UserInfo

@Keep
data class LoginDetails(
    val privKey: String? = null,
    val ed25519PrivKey: String? = null,
    val error: String? = null,
    val sessionId: String? = null,
    val share: UserInfo? = null
)
