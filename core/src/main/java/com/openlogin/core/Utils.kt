package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsService

const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

fun ByteArray.toBase64URLString(): String = Base64.encodeToString(this, BASE64_URL_FLAGS)

fun decodeBase64URLString(src: String): ByteArray = Base64.decode(src, BASE64_URL_FLAGS)

fun Context.getCustomTabsPackages(): List<String> {
    val browserIntent = Intent()
    browserIntent.addCategory(Intent.CATEGORY_BROWSABLE)
    browserIntent.action = Intent.ACTION_VIEW
    browserIntent.data = Uri.fromParts("http", "", null)

    val resolvedPackages =
        packageManager.queryIntentActivities(browserIntent, 0).map { it.activityInfo.packageName }
    val customTabsPackages = arrayListOf<String>()

    for (`package` in resolvedPackages) {
        val customTabsServiceIntent = Intent()
        customTabsServiceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
        customTabsServiceIntent.`package` = `package`
        if (packageManager.resolveService(customTabsServiceIntent, 0) != null) {
            customTabsPackages += `package`
        }
    }
    return customTabsPackages
}