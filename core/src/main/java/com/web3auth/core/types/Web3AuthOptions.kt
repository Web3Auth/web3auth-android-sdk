package com.web3auth.core.types

import android.content.Context
import android.net.Uri

data class Web3AuthOptions(
    var context: Context,
    val clientId: String,
    val network: Network,
    var buildEnv: BuildEnv? = BuildEnv.PRODUCTION,
    @Transient var redirectUrl: Uri? = null,
    var sdkUrl: String = getSdkUrl(buildEnv),
    val whiteLabel: WhiteLabelData? = null,
    val loginConfig: HashMap<String, LoginConfigItem>? = null,
    val useCoreKitKey: Boolean? = false,
    val chainNamespace: ChainNamespace? = ChainNamespace.EIP155,
    val mfaSettings: MfaSettings? = null,
    val sessionTime: Int? = 86400,
    var walletSdkUrl: String? = getWalletSdkUrl(buildEnv),
    val chainConfig: ChainConfig
)

fun getSdkUrl(buildEnv: BuildEnv?): String {
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

fun getWalletSdkUrl(buildEnv: BuildEnv?): String {
    val sdkUrl: String = when (buildEnv) {
        BuildEnv.STAGING -> {
            "https://staging-wallet.web3auth.io/$walletServicesVersion"
        }

        BuildEnv.TESTING -> {
            "https://develop-wallet.web3auth.io"
        }

        else -> {
            "https://wallet.web3auth.io/$walletServicesVersion"
        }
    }
    return sdkUrl
}

const val openLoginVersion = "v7"
const val walletServicesVersion = "v1"
const val WALLET_URL = "walletUrl"