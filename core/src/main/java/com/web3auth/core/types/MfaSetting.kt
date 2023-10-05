package com.web3auth.core.types

data class MfaSetting(
    var enable: Boolean?,
    var priority: Int,
    var mandatory: Boolean?
)