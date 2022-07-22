package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class UserInfo (
    var email: String = "",
    var name: String = "",
    var profileImage: String = "",
    var aggregateVerifier: String = "",
    var verifier: String = "",
    var verifierId: String = "",
    var typeOfLogin: String = "",
    var dappShare: String = "",
    var idToken: String = "",
    var oAuthIdToken: String = "",
    var oAuthAccessToken: String = ""
)