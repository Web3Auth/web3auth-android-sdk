package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class WhitelistResponse(
    @Keep val urls: List<String>,
    @Keep val signed_urls: Map<String, String>
)

@Keep
data class ProjectConfigResponse(
    @Keep val whitelabel: WhiteLabelData? = null,
    @Keep val sms_otp_enabled: Boolean,
    @Keep val wallet_connect_enabled: Boolean,
    @Keep val wallet_connect_project_id: String?,
    @Keep val whitelist: WhitelistResponse?,
)
