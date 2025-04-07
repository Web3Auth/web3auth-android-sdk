package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class MfaSettings(
    @Keep private var deviceShareFactor: MfaSetting? = null,
    @Keep private var backUpShareFactor: MfaSetting? = null,
    @Keep private var socialBackupFactor: MfaSetting? = null,
    @Keep private var passwordFactor: MfaSetting? = null,
    @Keep private var passkeysFactor: MfaSetting? = null,
    @Keep private var authenticatorFactor: MfaSetting? = null,
)