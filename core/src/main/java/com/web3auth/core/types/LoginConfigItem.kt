package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class LoginConfigItem(
    @Keep var verifier: String,
    @Keep var typeOfLogin: TypeOfLogin,
    @Keep var name: String? = null,
    @Keep var description: String? = null,
    @Keep var clientId: String,
    @Keep var verifierSubIdentifier: String? = null,
    @Keep var logoHover: String? = null,
    @Keep var logoLight: String? = null,
    @Keep var logoDark: String? = null,
    @Keep var mainOption: Boolean? = false,
    @Keep var showOnModal: Boolean? = true,
    @Keep var showOnDesktop: Boolean? = true,
    @Keep var showOnMobile: Boolean? = true,
)