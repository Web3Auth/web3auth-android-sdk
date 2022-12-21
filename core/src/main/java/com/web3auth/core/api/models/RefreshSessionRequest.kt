package com.web3auth.core.api.models

data class RefreshSessionRequest(
    val refresh_token: String? = null,
    val old_session_key: String? = null,
    val key: String? = null,
    val data: String? = null,
    val signature: String? = null,
    val namespace: String? = null
)