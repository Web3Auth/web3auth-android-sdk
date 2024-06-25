package com.web3auth.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.util.Patterns
import androidx.browser.customtabs.CustomTabsService
import com.web3auth.core.types.Network
import com.web3auth.core.types.WhiteLabelData
import org.torusresearch.fetchnodedetails.types.TorusNetwork
import java.nio.ByteBuffer

const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

fun ByteArray.toBase64URLString(): String = Base64.encodeToString(this, BASE64_URL_FLAGS)

fun decodeBase64URLString(src: String): ByteArray = Base64.decode(src, BASE64_URL_FLAGS)

val ALLOWED_CUSTOM_TABS_PACKAGES =
    arrayOf(
        "com.android.chrome", // Chrome stable
        "com.google.android.apps.chrome", // Chrome system
        "com.chrome.beta",// Chrome beta
        "com.chrome.dev" // Chrome dev
    )

fun Context.doesDefaultBrowserSupportCustomTabs(): Boolean {
    val defaultBrowserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web3auth.io"))

    val `package` = packageManager.resolveActivity(
        defaultBrowserIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName ?: return false
    if (!ALLOWED_CUSTOM_TABS_PACKAGES.contains(`package`)) return false

    val customTabsIntent = Intent()
    customTabsIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
    customTabsIntent.`package` = `package`
    return packageManager.resolveService(customTabsIntent, 0) != null
}

fun String.isEmailValid(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isPhoneNumberValid(): Boolean {
    return Patterns.PHONE.matcher(this).matches()
}

fun Context.getDefaultBrowser(): String? {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web3auth.io"))
    val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?: return null
    val activityInfo = resolveInfo.activityInfo ?: return null
    return activityInfo.packageName
}

fun Context.getCustomTabsBrowsers(): List<String> {
    val customTabsBrowsers: MutableList<String> = ArrayList()
    for (browser in ALLOWED_CUSTOM_TABS_PACKAGES) {
        val customTabsIntent = Intent()
        customTabsIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
        customTabsIntent.setPackage(browser)

        // Check if this package also resolves the Custom Tabs service.
        if (packageManager.resolveService(customTabsIntent, 0) != null) {
            customTabsBrowsers.add(browser)
        }
    }
    return customTabsBrowsers
}

fun WhiteLabelData.merge(other: WhiteLabelData): WhiteLabelData {
    val mergedTheme = HashMap<String, String?>()
    this.theme.let {
        if (it != null) {
            mergedTheme.putAll(it)
        }
    }
    other.theme?.forEach { (key, value) ->
        if (!mergedTheme.containsKey(key)) {
            mergedTheme[key] = value ?: mergedTheme[key]
        }
    }

    return WhiteLabelData(
        appName = this.appName ?: other.appName,
        appUrl = this.appUrl ?: other.appUrl,
        logoLight = this.logoLight ?: other.logoLight,
        logoDark = this.logoDark ?: other.logoDark,
        defaultLanguage = this.defaultLanguage ?: other.defaultLanguage,
        mode = this.mode ?: other.mode,
        useLogoLoader = this.useLogoLoader ?: other.useLogoLoader,
        theme = mergedTheme
    )
}

fun Map<String, String>?.mergeMaps(other: Map<String, String>?): Map<String, String>? {
    if (this == null && other == null) {
        return null
    } else if (this == null) {
        return other
    } else if (other == null) {
        return this
    }

    val mergedMap = LinkedHashMap<String, String>()
    mergedMap.putAll(this)

    other.forEach { (key, value) ->
        mergedMap[key] = value
    }

    return mergedMap
}

val TORUS_NETWORK_MAP: Map<Network, TorusNetwork> = mapOf(
    Network.MAINNET to TorusNetwork.MAINNET,
    Network.TESTNET to TorusNetwork.TESTNET,
    Network.AQUA to TorusNetwork.AQUA,
    Network.CYAN to TorusNetwork.CYAN,
    Network.SAPPHIRE_DEVNET to TorusNetwork.SAPPHIRE_DEVNET,
    Network.SAPPHIRE_MAINNET to TorusNetwork.SAPPHIRE_MAINNET
)

fun ByteArray.toLong(): Long {
    return ByteBuffer.wrap(this).long
}

fun ByteArray.toShort(): Short {
    return ByteBuffer.wrap(this).short
}

fun decode(data: ByteArray): AttestationStruct {
    // Placeholder for actual decoding logic
    return AttestationStruct(data)
}

// A placeholder for the attestation structure as it would be parsed in a real-world scenario
data class AttestationStruct(
    val authData: ByteArray
)

