package com.web3auth.core.models

import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.Web3AuthOptions

data class OpenloginSessionConfig(
    val actionType: String,
    val options: Web3AuthOptions,
    val params: LoginParams
)