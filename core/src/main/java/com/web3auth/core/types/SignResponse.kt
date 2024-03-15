package com.web3auth.core.types

data class SignResponse(
    val success: Boolean,
    val result: String?,
    val error: String?
)