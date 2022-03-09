package com.openlogin.app

import com.openlogin.core.types.Provider

data class LoginVerifier (
    val name : String,
    val loginProvider : Provider
    )