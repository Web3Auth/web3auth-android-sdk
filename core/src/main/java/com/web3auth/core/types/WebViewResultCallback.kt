package com.web3auth.core.types

import com.google.gson.JsonObject

interface WebViewResultCallback {
    fun onSignResponseReceived(signResponse: JsonObject?)
    fun onWebViewCancelled()
}