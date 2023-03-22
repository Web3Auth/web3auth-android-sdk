package com.web3auth.core.types

import android.content.Context
import android.net.Uri
import com.web3auth.core.Web3Auth

data class Web3AuthOptions(
    var context: Context,
    val clientId: String,
    val network: Web3Auth.Network,
    @Transient var redirectUrl: Uri? = null,
    var sdkUrl: String = getSdkUrl(network),
    val whiteLabel: WhiteLabelData? = null,
    val loginConfig: HashMap<String, LoginConfigItem>? = null,
    val useCoreKitKey: Boolean? = false,
    val chainNamespace: Web3Auth.ChainNamespace? = null
)

fun getSdkUrl(network: Web3Auth.Network): String {
    var sdkUrl: String = ""
    sdkUrl = if (network == Web3Auth.Network.TESTNET) {
        "https://dev-sdk.openlogin.com"
    } else {
        "https://sdk.openlogin.com"
    }
    return sdkUrl
}