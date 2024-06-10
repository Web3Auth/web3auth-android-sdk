package com.web3auth.core.types

data class LoginConfigItem(
    var verifier: String,
    var typeOfLogin: TypeOfLogin,
    var name: String? = null,
    var description: String? = null,
    var clientId: String,
    var verifierSubIdentifier: String? = null,
    var logoHover: String? = null,
    var logoLight: String? = null,
    var logoDark: String? = null,
    var mainOption: Boolean? = false,
    var showOnModal: Boolean? = true,
    var showOnDesktop: Boolean? = true,
    var showOnMobile: Boolean? = true,
)