package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.concurrent.CompletableFuture

class OpenLogin(val context: Context) : IOpenLogin {
    override fun login(
        loginProvider: String?,
        fastLogin: Boolean,
        redirectUrl: String?,
        appState: String?,
        extraLoginOptions: Any?
    ): CompletableFuture<Array<String>> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.openlogin.com"))
        context.startActivity(intent)
        return CompletableFuture.completedFuture(arrayOf("<private key>"))
    }

    override fun logout(
        fastLogin: Boolean,
        redirectUrl: String?,
        appState: String?
    ): CompletableFuture<Void> {
        // TODO: Log out
        return CompletableFuture.completedFuture(null)
    }
}