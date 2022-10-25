package com.web3auth.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.util.Patterns
import androidx.browser.customtabs.CustomTabsService

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