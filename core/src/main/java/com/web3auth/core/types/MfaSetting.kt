package com.web3auth.core.types

import androidx.annotation.Keep

@Keep
data class MfaSetting(
    @Keep var enable: Boolean,
    @Keep var priority: Int?,
    @Keep var mandatory: Boolean?
)