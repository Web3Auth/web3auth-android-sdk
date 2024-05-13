package com.web3auth.core.types

data class WhiteLabelData(
    var appName: String? = null,
    var appUrl: String? = null,
    var logoLight: String? = null,
    var logoDark: String? = null,
    var defaultLanguage: Language? = Language.EN,
    var mode: ThemeModes? = null,
    var useLogoLoader: Boolean? = false,
    var theme: HashMap<String, String?>? = null
)