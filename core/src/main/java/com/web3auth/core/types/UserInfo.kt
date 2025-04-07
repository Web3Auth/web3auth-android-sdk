package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class UserInfo (
    @Keep var email: String = "",
    @Keep var name: String = "",
    @Keep var profileImage: String = "",
    @Keep var aggregateVerifier: String = "",
    @Keep var verifier: String = "",
    @Keep var verifierId: String = "",
    @Keep var typeOfLogin: String = "",
    @Keep var dappShare: String = "",
    @Keep var idToken: String = "",
    @Keep var oAuthIdToken: String = "",
    @Keep var oAuthAccessToken: String = "",
    @Keep var isMfaEnabled: Boolean? = null
)