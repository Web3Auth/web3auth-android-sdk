package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class SessionResponse(
    @Keep val sessionId: String
)

@Keep
data class RedirectResponse(
    @Keep val actionType: String
)
