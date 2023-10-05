package com.web3auth.core.types

data class MfaSettings(
    private var deviceShareFactor: MfaSetting? = null,
    private var backUpShareFactor: MfaSetting? = null,
    private var socialBackupFactor: MfaSetting? = null,
    private var passwordFactor: MfaSetting? = null,
)