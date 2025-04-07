package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class SignMessage(
    @Keep val loginId: String,
    @Keep val sessionId: String,
    @Keep val platform: String = "android",
    @Keep val request: RequestData,
    @Keep val appState: String? = null //Added for mocaverse use-case
)

data class RequestData(
    @Keep val method: String,
    @Keep val params: String
)