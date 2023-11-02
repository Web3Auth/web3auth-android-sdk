package com.web3auth.core.types

data class LoginConfigItem(
    var verifier: String,
    private var typeOfLogin: TypeOfLogin,
    private var name: String? = null,
    private var description: String? = null,
    private var clientId: String? = null,
    private var verifierSubIdentifier: String? = null,
    private var logoHover: String? = null,
    private var logoLight: String? = null,
    private var logoDark: String? = null,
    private var mainOption: Boolean? = false,
    private var showOnModal: Boolean? = true,
    private var showOnDesktop: Boolean? = true,
    private var showOnMobile: Boolean? = true,
)