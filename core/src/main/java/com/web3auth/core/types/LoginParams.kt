package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class LoginParams(
    @Keep val authConnection: AuthConnection,
    @Keep val authConnectionId: String? = null,
    @Keep val groupedAuthConnectionId: String? = null,
    @Keep val appState: String? = null,
    @Keep val mfaLevel: MFALevel? = null,
    @Keep val extraLoginOptions: ExtraLoginOptions? = null,
    @Keep var dappShare: String? = null,
    @Keep val curve: Curve? = Curve.SECP256K1,
    @Keep var dappUrl: String? = null,
    @Keep var loginHint: String? = null,
    @Keep val idToken: String? = null,
    @Keep val recordId: String? = null,
    @Keep val loginSource: String? = null,
)
