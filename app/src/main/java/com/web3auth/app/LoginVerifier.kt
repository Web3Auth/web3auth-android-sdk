package com.web3auth.app

import com.web3auth.core.Web3Auth

data class LoginVerifier (
    val name : String,
    val loginProvider : Web3Auth.Provider
    )