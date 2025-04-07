package com.web3auth.core.types

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class ExtraLoginOptions(
    @Keep private var additionalParams: HashMap<String, String>? = null,
    @Keep private var domain: String? = null,
    @Keep private var client_id: String? = null,
    @Keep private var leeway: String? = null,
    @Keep private var verifierIdField: String? = null,
    @Keep private var isVerifierIdCaseSensitive: Boolean? = null,
    @Keep private var display: Display? = null,
    @Keep private var prompt: Prompt? = null,
    @Keep private var max_age: String? = null,
    @Keep private var ui_locales: String? = null,
    @Keep private var id_token: String? = null,
    @Keep private var id_token_hint: String? = null,
    @Keep var login_hint: String? = null,
    @Keep private var acr_values: String? = null,
    @Keep private var scope: String? = null,
    @Keep private var audience: String? = null,
    @Keep private var connection: String? = null,
    @Keep private var state: String? = null,
    @Keep private var response_type: String? = null,
    @Keep private var nonce: String? = null,
    @Keep private var redirect_uri: String? = null
) : Serializable
