package com.openlogin.core

import java.util.concurrent.CompletableFuture

class OpenLogin : IOpenLogin {
    override fun login(
        loginProvider: String?,
        fastLogin: Boolean,
        redirectUrl: String?,
        appState: String?,
        extraLoginOptions: Any?
    ): CompletableFuture<Array<String>> {
        return CompletableFuture.completedFuture(arrayOf("<private key>"))
    }

    override fun logout(
        fastLogin: Boolean,
        redirectUrl: String?,
        appState: String?
    ): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}