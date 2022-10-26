package com.web3auth.app

import com.web3auth.core.types.Provider

data class LoginVerifier(
    val name: String,
    val loginProvider: Provider
)