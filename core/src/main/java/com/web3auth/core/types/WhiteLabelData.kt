package com.web3auth.core.types

data class WhiteLabelData(
    private var appName: String? = null,
    private var appUrl: String? = null,
    private var logoLight: String? = null,
    private var logoDark: String? = null,
    private var defaultLanguage: Language? = Language.EN,
    private var mode: ThemeModes? = null,
    private var useLogoLoader: Boolean? = false,
    private var theme: HashMap<String, String>? = null
)