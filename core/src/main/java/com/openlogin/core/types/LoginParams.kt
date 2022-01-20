package com.openlogin.core.types

import com.openlogin.core.OpenLogin

data class LoginParams (
    val loginProvider: OpenLogin.Provider,
    val reLogin: Boolean? = null,
    val skipTKey: Boolean? = null,
    val extraLoginOptions: ExtraLoginOptions? = null,
    val baseRedirectParams: BaseRedirectParams? = null
)