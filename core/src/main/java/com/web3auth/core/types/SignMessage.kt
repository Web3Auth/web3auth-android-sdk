package com.web3auth.core.types

data class SignMessage(
    val loginId: String,
    val sessionId: String,
    val platform: String = "android",
    val request: RequestData,
    val appState: String? = null //Added for mocaverse use-case
)

data class RequestData(
    val method: String,
    val params: String
)