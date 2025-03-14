package com.web3auth.core.types

data class SessionResponse(
    val sessionId: String
)

data class RedirectResponse(
    val actionType: String
)
