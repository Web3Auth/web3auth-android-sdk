package com.web3auth.core.types

data class WhiteLabelData(
    private var name: String? = null,
    private var logoLight: String? = null,
    private var logoDark: String? = null,
    private var defaultLanguage: String? = "en",
    private var dark: Boolean? = false,
    private var theme: HashMap<String, String>? = null
)