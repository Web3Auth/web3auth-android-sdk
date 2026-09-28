package com.web3auth.core.types

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import org.torusresearch.fetchnodedetails.types.Web3AuthNetwork

@Keep
data class Web3AuthOptions(
    @Keep val clientId: String,
    @Keep var redirectUrl: String,
    @Keep var originData: Map<String, String>? = null,
    @SerializedName("buildEnv")
    @Keep var authBuildEnv: BuildEnv = BuildEnv.PRODUCTION,
    @Keep var sdkUrl: String = getSdkUrl(authBuildEnv),
    @Keep var storageServerUrl: String? = null,
    @Keep var sessionSocketUrl: String? = null,
    @Keep var citadelServerUrl: String? = null,
    @Keep var authConnectionConfig: List<AuthConnectionConfig>? = emptyList(),
    @Keep var whiteLabel: WhiteLabelData? = null,
    @Keep var dashboardUrl: String? = getDashboardUrl(authBuildEnv),
    @Keep var accountAbstractionConfig: String? = null,
    @Keep var walletSdkUrl: String? = getWalletSdkUrl(authBuildEnv),
    @Keep var includeUserDataInToken: Boolean? = true,
    @Keep var chains: Chains? = null,
    @Keep var defaultChainId: String? = "0x1",
    @Keep var enableLogging: Boolean = false,
    /**
     * Session lifetime in seconds. When null, hydrated from project config on
     * [com.web3auth.core.Web3Auth.initialize], otherwise [DEFAULT_SESSION_TIME].
     */
    @Keep var sessionTime: Int? = null,
    @SerializedName("network")
    @Keep val web3AuthNetwork: Web3AuthNetwork,
    @Keep val useSFAKey: Boolean? = false,
    @Keep var walletServicesConfig: WalletServicesConfig? = null,
    @Keep var mfaSettings: MfaSettings? = null,
    @Keep var sessionNamespace: String? = null,
    @Keep var wsEmbedDappClientId: String? = null,
    /** Set from project config when smartAccounts.walletScope is `all`. */
    @Keep var useAAWithExternalWallet: Boolean? = null,
) {
    init {
        if (dashboardUrl == null) {
            dashboardUrl = getDashboardUrl(authBuildEnv)
        }
        if (storageServerUrl.isNullOrBlank()) {
            storageServerUrl = Web3AuthUrls.storageServerUrl(authBuildEnv)
        }
        if (sessionSocketUrl.isNullOrBlank()) {
            sessionSocketUrl = Web3AuthUrls.sessionSocketUrl(authBuildEnv)
        }
        if (citadelServerUrl.isNullOrBlank()) {
            citadelServerUrl = Web3AuthUrls.citadelServerUrl(authBuildEnv)
        }
    }
}

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

fun getDashboardUrl(buildEnv: BuildEnv?): String {
    val sdkUrl: String = when (buildEnv) {
        BuildEnv.STAGING -> {
            "https://staging-account.web3auth.io/$authDashboardVersion/$walletAccountConstant"
        }

        BuildEnv.TESTING -> {
            "https://develop-account.web3auth.io/$walletAccountConstant"
        }

        else -> {
            "https://account.web3auth.io/$authDashboardVersion/$walletAccountConstant"
        }
    }
    return sdkUrl
}

const val authServiceVersion = "v11"
/** Auth v11 + ws-embed 6.x use wallet v6 (citadel sessionId + accessToken). */
const val walletServicesVersion = "v6"
const val authDashboardVersion = "v11"
const val walletAccountConstant = "wallet/account"
const val DEFAULT_SESSION_TIME = 30 * 86400
const val WEBVIEW_URL = "walletUrl"
const val REDIRECT_URL = "redirectUrl"
const val CUSTOM_TABS_URL = "customTabsUrl"