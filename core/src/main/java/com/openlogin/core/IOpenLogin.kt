package com.openlogin.core

import java.util.concurrent.CompletableFuture

interface IOpenLogin {
    fun login(
        loginProvider: String? = null,
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: String? = null,
        extraLoginOptions: Any? = null,
    ): CompletableFuture<Array<String>>

    fun logout(
        fastLogin: Boolean = false,
        redirectUrl: String? = null,
        appState: String? = null
    ): CompletableFuture<Void>
}
