package com.web3auth.core.types

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.web3auth.core.analytics.AnalyticsEvents
import com.web3auth.core.analytics.AnalyticsSdkType
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
    @Keep var authConnectionConfig: List<AuthConnectionConfig>? = emptyList(),
    @Keep var whiteLabel: WhiteLabelData? = null,
    @Keep var dashboardUrl: String? = getDashboardUrl(authBuildEnv),
    @Keep var accountAbstractionConfig: String? = null,
    @Keep var walletSdkUrl: String? = getWalletSdkUrl(authBuildEnv),
    @Keep var includeUserDataInToken: Boolean? = true,
    @Keep var chains: Chains? = null,
    @Keep var defaultChainId: String? = "0x1",
    @Keep var enableLogging: Boolean = false,
    @Keep val sessionTime: Int = 30 * 86400,
    @SerializedName("network")
    @Keep val web3AuthNetwork: Web3AuthNetwork,
    @Keep val useSFAKey: Boolean? = false,
    @Keep val walletServicesConfig: WalletServicesConfig? = null,
    @Keep var mfaSettings: MfaSettings? = null,
) {

    @Transient
    private var isFlutterAnalytics: Boolean? = false
    private var sdkVersion: String? = null
    fun setFlutterAnalytics(analytics: Boolean?, sdkVersion: String? = null) {
        this.isFlutterAnalytics = analytics ?: false
        this.sdkVersion = sdkVersion
    }

    fun getFlutterAnalytics(): Boolean? = isFlutterAnalytics
    fun getSdkVersion(): String {
        return if (sdkVersion.isNullOrEmpty()) {
            AnalyticsEvents.ANDROID_SDK_VERSION // android sdk version
        } else {
            sdkVersion ?: "unknown" // flutter sdk version
        }
    }

    fun getSdkName(): String {
        return if (isFlutterAnalytics == true) AnalyticsSdkType.FLUTTER else AnalyticsSdkType.ANDROID
    }

    init {
        if (dashboardUrl == null) {
            dashboardUrl = getDashboardUrl(authBuildEnv)
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

const val authServiceVersion = "v10"
const val walletServicesVersion = "v5"
const val authDashboardVersion = "v10"
const val walletAccountConstant = "wallet/account"
const val WEBVIEW_URL = "walletUrl"
const val REDIRECT_URL = "redirectUrl"
const val CUSTOM_TABS_URL = "customTabsUrl"