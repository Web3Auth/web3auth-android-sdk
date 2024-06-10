package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class WhitelistResponse(
    val urls: List<String>,
    val signed_urls: Map<String, String>
)

@Keep
data class ProjectConfigResponse(
    val whitelabel: WhiteLabelData? = null,
    val sms_otp_enabled: Boolean,
    val wallet_connect_enabled: Boolean,
    val wallet_connect_project_id: String?,
    val whitelist: WhitelistResponse?,
)
