package com.web3auth.core.api.models

data class LogoutApiRequest (
    val key: String? = null,
    val data: String? = null,
    val signature: String? = null,
    val timeout: Int = 0
)