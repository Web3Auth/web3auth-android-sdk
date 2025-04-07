package com.web3auth.core.types

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class LoginParams(
    @Keep val loginProvider: Provider,
    @Keep var dappShare: String? = null,
    @Keep val extraLoginOptions: ExtraLoginOptions? = null,
    @Keep @Transient var redirectUrl: Uri? = null,
    @Keep val appState: String? = null,
    @Keep val mfaLevel: MFALevel? = null,
    @Keep val curve: Curve? = Curve.SECP256K1,
    @Keep val dappUrl: String? = null,
)