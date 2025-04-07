package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class SignResponse(
    @Keep val success: Boolean,
    @Keep val result: String?,
    @Keep val error: String?
)