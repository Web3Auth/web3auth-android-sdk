package com.web3auth.core.api.models

import androidx.annotation.Keep

@Keep
data class RefreshSessionResponse(
    val refresh_token: String? = null,
    val id_token: String? = null
)