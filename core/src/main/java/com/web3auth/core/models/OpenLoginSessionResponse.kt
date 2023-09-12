package com.web3auth.core.models

data class OpenLoginSessionResponse(
    val sessionId: String,
    val sessionNamespace: String
)