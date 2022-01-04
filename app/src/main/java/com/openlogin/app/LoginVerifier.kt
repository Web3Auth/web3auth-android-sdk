package com.openlogin.app

import com.openlogin.core.OpenLogin

data class LoginVerifier (
    val name : String,
    val loginProvider : OpenLogin.Provider
    )