package com.openlogin.core.types

import android.net.Uri

data class BaseRedirectParams (
    val redirectUrl: Uri? = null,
    val appState: String? = null
)