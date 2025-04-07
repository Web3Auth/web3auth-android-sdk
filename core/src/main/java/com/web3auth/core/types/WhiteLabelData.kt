package com.web3auth.core.types

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class WhiteLabelData(
    @Keep var appName: String? = null,
    @Keep var appUrl: String? = null,
    @Keep var logoLight: String? = null,
    @Keep var logoDark: String? = null,
    @Keep var defaultLanguage: Language? = Language.EN,
    @Keep var mode: ThemeModes? = null,
    @Keep var useLogoLoader: Boolean? = false,
    @Keep var theme: HashMap<String, String?>? = null
) : Serializable