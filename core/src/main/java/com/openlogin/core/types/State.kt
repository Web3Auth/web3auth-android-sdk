package com.openlogin.core.types

import androidx.annotation.Keep

@Keep
data class State(
    val privKey: String? = null,
    val userInfo: UserInfo? = null,
)