package com.web3auth.core.types

import android.content.Context
import android.net.Uri

data class Web3AuthOptions(
    var context: Context,
    val clientId: String,
    val network: Network,
    var buildEnv: BuildEnv,
    @Transient var redirectUrl: Uri? = null,
    var sdkUrl: String = getSdkUrl(buildEnv),
    val whiteLabel: WhiteLabelData? = null,
    val loginConfig: HashMap<String, LoginConfigItem>? = null,
    val useCoreKitKey: Boolean? = false,
    val chainNamespace: ChainNamespace? = ChainNamespace.EIP155,
    val mfaSettings: MfaSettings? = null
)

fun getSdkUrl(buildEnv: BuildEnv): String {
    val sdkUrl: String = when (buildEnv) {
        BuildEnv.STAGING -> {
            "https://staging-auth.web3auth.io/$openLoginVersion"
        }
        BuildEnv.TESTING -> {
            "https://develop-auth.web3auth.io"
        }
        else -> {
            "https://auth.web3auth.io/$openLoginVersion"
        }
    }
    return sdkUrl
}

const val openLoginVersion = "v5"