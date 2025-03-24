package com.web3auth.core.types

interface WebViewResultCallback {
    fun onSignResponseReceived(signResponse: SignResponse?)
    fun onSessionResponseReceived(sessionResponse: SessionResponse?)
    fun onWebViewCancelled()
}