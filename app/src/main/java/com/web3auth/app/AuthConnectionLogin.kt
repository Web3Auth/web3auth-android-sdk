package com.web3auth.app

import com.web3auth.core.types.AUTH_CONNECTION

data class AuthConnectionLogin(
    val name: String,
    val authConnection: AUTH_CONNECTION
)