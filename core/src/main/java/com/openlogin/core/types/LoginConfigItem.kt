package com.openlogin.core.types

data class LoginConfigItem(
    private var verifier: String,
    private var typeOfLogin: TypeOfLogin,
    private var name: String,
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