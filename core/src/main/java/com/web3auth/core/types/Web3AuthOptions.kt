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
    var dashboardUrl: String? = getDashBoardUrl(buildEnv),
    var originData: Map<String, String>? = null
)

fun getSdkUrl(buildEnv: BuildEnv?): String {
    val sdkUrl: String = when (buildEnv) {
        BuildEnv.STAGING -> {
            "https://staging-auth.web3auth.io/$authServiceVersion"
        }

        BuildEnv.TESTING -> {
            "https://develop-auth.web3auth.io"
        }

        else -> {
            "https://auth.web3auth.io/$authServiceVersion"
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

fun getDashBoardUrl(buildEnv: BuildEnv?): String {
    val sdkUrl: String = when (buildEnv) {
        BuildEnv.STAGING -> {
            "https://staging-account.web3auth.io/v9/$walletAccountConstant"
        }

        BuildEnv.TESTING -> {
            "https://develop-account.web3auth.io/$walletAccountConstant"
        }

        else -> {
            "https://account.web3auth.io/v9/$walletAccountConstant"
        }
    }
    return sdkUrl
}

const val authServiceVersion = "v9"
const val walletServicesVersion = "v3"
const val walletAccountConstant = "wallet/account"
const val WEBVIEW_URL = "walletUrl"
const val REDIRECT_URL = "redirectUrl"
const val CUSTOM_TABS_URL = "customTabsUrl"