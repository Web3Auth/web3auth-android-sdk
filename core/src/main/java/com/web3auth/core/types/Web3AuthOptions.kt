package com.web3auth.core.types

import android.net.Uri

data class Web3AuthOptions(
    val clientId: String,
    val network: Network,
    var buildEnv: BuildEnv? = BuildEnv.PRODUCTION,
    @Transient var redirectUrl: Uri,
    var sdkUrl: String = getSdkUrl(buildEnv),
    var whiteLabel: WhiteLabelData? = null,
    val loginConfig: HashMap<String, LoginConfigItem>? = null,
    val useCoreKitKey: Boolean? = false,
    val chainNamespace: ChainNamespace? = ChainNamespace.EIP155,
    val mfaSettings: MfaSettings? = null,
    val sessionTime: Int? = 86400,
    var walletSdkUrl: String? = getWalletSdkUrl(buildEnv),
    var originData: Map<String, String>? = null
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

const val openLoginVersion = "v8"
const val walletServicesVersion = "v2"
const val WEBVIEW_URL = "walletUrl"
const val REDIRECT_URL = "redirectUrl"
const val CUSTOM_TABS_URL = "customTabsUrl"