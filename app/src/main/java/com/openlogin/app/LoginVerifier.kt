package com.openlogin.app

import com.web3auth.core.OpenLogin

data class LoginVerifier (
    val name : String,
    val loginProvider : OpenLogin.Provider
    )