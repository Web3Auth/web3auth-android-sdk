package com.web3auth.core.types

data class ExtraLoginOptions(
    private var additionalParams : HashMap<String, String>? = null,
    private var domain : String? = null,
    private var client_id : String? = null,
    private var leeway : String? = null,
    private var verifierIdField : String? =null,
    private var isVerifierIdCaseSensitive : Boolean? = null,
    private var display : Display? = null,
    private var prompt : Prompt? = null,
    private var max_age : String? = null,
    private var ui_locales : String? = null,
    private var id_token : String? = null,
    private var id_token_hint : String? = null,
    private var login_hint : String? = null,
    private var acr_values : String? = null,
    private var scope : String? = null,
    private var audience : String? = null,
    private var connection : String? = null,
    private var state : String? = null,
    private var response_type : String? = null,
    private var nonce : String? = null,
    private var redirect_uri : String? = null
)
