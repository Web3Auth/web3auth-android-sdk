package com.web3auth.core.types

import android.content.Context
import android.net.Uri
import com.web3auth.core.Web3Auth

data class Web3AuthOptions (
    val context: Context,
    val clientId: String,
    val network: Web3Auth.Network,
    val redirectUrl: Uri? = null,
    val sdkUrl: String = "https://sdk.openlogin.com",
)