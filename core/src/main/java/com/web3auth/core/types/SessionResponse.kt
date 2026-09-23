package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class SessionResponse(
    @Keep val sessionId: String,
    @Keep val accessToken: String? = null,
    @Keep val refreshToken: String? = null,
    @Keep val idToken: String? = null,
)

@Keep
data class RedirectResponse(
    @Keep val actionType: String,
    @Keep val sessionId: String? = null,
    @Keep val accessToken: String? = null,
    @Keep val refreshToken: String? = null,
    @Keep val idToken: String? = null,
)
