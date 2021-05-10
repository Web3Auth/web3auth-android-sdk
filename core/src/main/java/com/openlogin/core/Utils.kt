package com.openlogin.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64

const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

fun ByteArray.toBase64URLString(): String = Base64.encodeToString(this, BASE64_URL_FLAGS)

fun decodeBase64URLString(src: String): ByteArray = Base64.decode(src, BASE64_URL_FLAGS)

val ALLOWED_CUSTOM_TABS_PACKAGES =
    arrayOf(
        "com.android.chrome", // Chrome stable
        "com.google.android.apps.chrome", // Chrome system
        "com.chrome.beta",// Chrome beta
    )

fun Context.doesDefaultBrowserSupportCustomTabs(): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openlogin.com"));
    val `package` = packageManager.resolveActivity(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName;
    return `package` != null && ALLOWED_CUSTOM_TABS_PACKAGES.contains(`package`)
}