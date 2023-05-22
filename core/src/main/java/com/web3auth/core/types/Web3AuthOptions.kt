package com.web3auth.core.types

import android.content.Context
import android.net.Uri

data class Web3AuthOptions(
    var context: Context,
    val clientId: String,
    val network: Network,
    @Transient var redirectUrl: Uri? = null,
    var sdkUrl: String = getSdkUrl(network),
    val whiteLabel: WhiteLabelData? = null,
    val loginConfig: HashMap<String, LoginConfigItem>? = null,
    val useCoreKitKey: Boolean? = false,
    val chainNamespace: ChainNamespace? = ChainNamespace.EIP155
)

fun getSdkUrl(network: Network): String {
    val sdkUrl: String = if (network == Network.TESTNET) {
        "https://dev-sdk.openlogin.com"
    } else {
        "https://sdk.openlogin.com"
    }
    return sdkUrl
}