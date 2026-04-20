package com.web3auth.core

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.auth0.android.jwt.JWT
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.web3auth.core.analytics.AnalyticsEvents
import com.web3auth.core.analytics.AnalyticsManager
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.api.ApiService
import com.web3auth.core.keystore.IS_SFA
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.keystore.SharedPrefsHelper
import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.ErrorCode
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.ProjectConfigResponse
import com.web3auth.core.types.REDIRECT_URL
import com.web3auth.core.types.RedirectResponse
import com.web3auth.core.types.SessionResponse
import com.web3auth.core.types.SignResponse
import com.web3auth.core.types.UnKnownException
import com.web3auth.core.types.UserCancelledException
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.WEBVIEW_URL
import com.web3auth.core.types.Web3AuthError
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.Web3AuthSubVerifierInfo
import com.web3auth.core.types.WebViewResultCallback
import com.web3auth.session_manager_android.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.torusresearch.fetchnodedetails.FetchNodeDetails
import org.torusresearch.fetchnodedetails.types.NodeDetails
import org.torusresearch.torusutils.TorusUtils
import org.torusresearch.torusutils.types.VerifierParams
import org.torusresearch.torusutils.types.VerifyParams
import org.torusresearch.torusutils.types.common.SessionToken
import org.torusresearch.torusutils.types.common.TorusKey
import org.torusresearch.torusutils.types.common.TorusOptions
import org.web3j.crypto.Hash
import java.util.Locale
import java.util.concurrent.CompletableFuture

class Web3Auth(web3AuthOptions: Web3AuthOptions, context: Context) : WebViewResultCallback,
    ContextWrapper(context) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private lateinit var loginCompletableFuture: CompletableFuture<Web3AuthResponse>
    private lateinit var enableMfaCompletableFuture: CompletableFuture<Boolean>
    private lateinit var manageMfaCompletableFuture: CompletableFuture<Boolean>
    private lateinit var signMsgCF: CompletableFuture<SignResponse>

    private var nodeDetailManager: FetchNodeDetails =
        FetchNodeDetails(web3AuthOptions.web3AuthNetwork)
    private val torusUtils: TorusUtils
    private var web3AuthResponse: Web3AuthResponse? = null
    private var web3AuthOption = web3AuthOptions
    private var sessionManager: SessionManager
    private var projectConfigResponse: ProjectConfigResponse? = null
    private var loginParams: LoginParams? = null
    private val startTime: Long = System.currentTimeMillis()
    private var actionType: String? = null

    init {
        //Segment Analytics initialization
        AnalyticsManager.initialize(context.applicationContext)
        AnalyticsManager.identify(
            web3AuthOptions.clientId, mapOf(
                "web3auth_client_id" to web3AuthOptions.clientId,
                "web3auth_network" to web3AuthOptions.web3AuthNetwork,
            )
        )
        AnalyticsManager.setGlobalProperties(
            mapOf(
                "sdk_name" to web3AuthOptions.getSdkName(),
                "sdk_version" to web3AuthOptions.getSdkVersion(),
                "web3auth_client_id" to web3AuthOptions.clientId,
                "web3auth_network" to web3AuthOptions.web3AuthNetwork,
            )
        )

        val torusOptions = TorusOptions(
            web3AuthOptions.clientId, web3AuthOptions.web3AuthNetwork, null,
            0, true
        )
        torusUtils = TorusUtils(torusOptions)
        SharedPrefsHelper.init(context.applicationContext)
        val isSFAValue = SharedPrefsHelper.getBoolean(IS_SFA)

        //Session Manager initialization
        sessionManager = SessionManager(
            context,
            web3AuthOptions.sessionTime,
            web3AuthOptions.redirectUrl,
            sessionNamespace = if (isSFAValue) "sfa" else ""
        )
    }

    /**
     * Initializes the KeyStoreManager.
     */
    private fun initiateKeyStoreManager() {
        KeyStoreManagerUtils.getKeyGenerator()
    }

    /**
     * Makes a request with the specified action type and login parameters.
     *
     * @param actionType The type of action to perform.
     * @param params The login parameters required for the request.
     */
    private fun processRequest(
        actionType: String, params: LoginParams?
    ) {
        if ((actionType == "enable_mfa" || actionType == "manage_mfa") && !params?.idToken.isNullOrEmpty()) {
            throwEnableMFAError(ErrorCode.ENABLE_MFA_NOT_ALLOWED)
            return
        }
        val sdkUrl = Uri.parse(web3AuthOption.sdkUrl)

        val initParamsJson = params?.let {
            JSONObject(gson.toJson(it))
        } ?: JSONObject()

        if (actionType == "manage_mfa") {
            initParamsJson.put("redirectUrl", web3AuthOption.dashboardUrl)
            initParamsJson.put("dappUrl", web3AuthOption.redirectUrl)
        } else {
            initParamsJson.put("redirectUrl", web3AuthOption.redirectUrl)
        }

        val redirectUrl = if (actionType == "manage_mfa") {
            web3AuthOption.dashboardUrl
        } else {
            web3AuthOption.redirectUrl
        }

        if (redirectUrl != null) {
            web3AuthOption.redirectUrl = redirectUrl
        }

        val initOptionsJson = JSONObject(gson.toJson(web3AuthOption))
        initOptionsJson.put(
            "network",
            web3AuthOption.web3AuthNetwork.toString().lowercase(Locale.ROOT)
        )

        val sessionId = SessionManager.generateRandomSessionKey()

        val paramMap = JSONObject()
        paramMap.put(
            "options", initOptionsJson
        )
        paramMap.put("actionType", actionType)

        if (actionType == "enable_mfa" || actionType == "manage_mfa") {
            val userInfo = web3AuthResponse?.userInfo
            initParamsJson.put("authConnection", userInfo?.authConnection)
            initParamsJson.put("authConnectionId", userInfo?.authConnectionId)
            initParamsJson.put("groupedAuthConnectionId", userInfo?.groupedAuthConnectionId)
            val extraOptionsString: String
            var existingExtraLoginOptions = ExtraLoginOptions()
            if (initParamsJson.has("extraLoginOptions")) {
                extraOptionsString = initParamsJson.getString("extraLoginOptions")
                existingExtraLoginOptions =
                    gson.fromJson(extraOptionsString, ExtraLoginOptions::class.java)
            }
            existingExtraLoginOptions.login_hint = userInfo?.userId
            initParamsJson.put("extraLoginOptions", gson.toJson(existingExtraLoginOptions))
            initParamsJson.put("mfaLevel", MFALevel.MANDATORY.name.lowercase(Locale.ROOT))
            val loginIdObject = mapOf("loginId" to sessionId, "platform" to "android")
            initParamsJson.put(
                "appState",
                gson.toJson(loginIdObject).toByteArray(Charsets.UTF_8).toBase64URLString()
            )
            paramMap.put("sessionId", SessionManager.getSessionIdFromStorage())
        }
        paramMap.put("params", initParamsJson)

        val jsonObject = JSONObject(paramMap.toString())

        var paramsString = jsonObject.toString()
        paramsString = paramsString.replace("\\/", "/")

        val loginIdCf = getLoginId(sessionId, paramsString)
        loginIdCf.whenComplete { loginId, error ->
            if (error == null) {
                val jsonObject = mapOf("loginId" to loginId)

                val hash = "b64Params=" + gson.toJson(jsonObject).toByteArray(Charsets.UTF_8)
                    .toBase64URLString()

                val url =
                    Uri.Builder().scheme(sdkUrl.scheme).encodedAuthority(sdkUrl.encodedAuthority)
                        .encodedPath(sdkUrl.encodedPath).appendPath("start").fragment(hash).build()
                //print("url: => $url")
                val intent = Intent(baseContext, CustomChromeTabsActivity::class.java)
                intent.putExtra(WEBVIEW_URL, url.toString())
                baseContext.startActivity(intent)
            }
        }
    }

    /**
     * Initializes the Web3Auth class asynchronously.
     *
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun initialize(): CompletableFuture<Void> {
        val initializeCf = CompletableFuture<Void>()
        KeyStoreManagerUtils.initializePreferences(baseContext.applicationContext)

        //initiate keyStore
        initiateKeyStoreManager()

        //fetch project config
        fetchProjectConfig().whenComplete { _, err ->
            if (err == null) {
                val chainIds = web3AuthOption.chains?.chainId?.let { arrayOf(it) } ?: emptyArray()
                val properties = mutableMapOf(
                    "chain_ids" to chainIds,
                    "chain_nameSpaces" to listOf("eip155", "solana", "other"),
                    "logging_enabled" to web3AuthOption.enableLogging,
                    "auth_build_env" to web3AuthOption.authBuildEnv,
                    "auth_mfa_settings" to web3AuthOption.mfaSettings,
                    "whitelabel_logo_light_enabled" to (web3AuthOption.whiteLabel?.logoLight != null),
                    "whitelabel_logo_dark_enabled" to (web3AuthOption.whiteLabel?.logoDark != null),
                    "whitelabel_theme_mode" to (web3AuthOption.whiteLabel?.theme),
                    "duration" to System.currentTimeMillis() - startTime,
                    "integration_type" to web3AuthOption.getSdkName(),
                    "dapp_url" to this.loginParams?.dappUrl,
                )

                AnalyticsManager.trackEvent(
                    AnalyticsEvents.SDK_INITIALIZATION_COMPLETED,
                    properties
                )
                //authorize session
                sessionManager.setSessionId(SessionManager.getSessionIdFromStorage())
                this.authorizeSession(web3AuthOption.redirectUrl, baseContext)
                    .whenComplete { resp, error ->
                        runOnUIThread {
                            if (error == null) {
                                web3AuthResponse = resp
                                initializeCf.complete(null)
                            } else {
                                SessionManager.deleteSessionIdFromStorage()
                                sessionManager.setSessionId("")
                                initializeCf.completeExceptionally(error)
                            }
                        }
                    }
            } else {
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.SDK_INITIALIZATION_FAILED,
                    mutableMapOf<String, Any>(
                        "integration_type" to web3AuthOption.getSdkName(),
                        "dapp_url" to "this.loginParams?.dappUrl.toString()",
                        "duration" to System.currentTimeMillis() - startTime,
                        "error_message" to "Fetch project config API error. ${err.message}"
                    )
                )
                initializeCf.completeExceptionally(err)
            }
        }
        return initializeCf
    }

    /**
     * Sets the result URL.
     *
     * @param uri The URI representing the result URL.
     */
    fun setResultUrl(uri: Uri?) {
        val hash = uri?.fragment
        if (hash == null) {
            if (::loginCompletableFuture.isInitialized) {
                loginCompletableFuture.completeExceptionally(UserCancelledException())
                return
            }
        }
        val hashUri = Uri.parse(uri?.host + "?" + uri?.fragment)
        val error = uri?.getQueryParameter("error")
        if (error != null) {
            if (::loginCompletableFuture.isInitialized) loginCompletableFuture.completeExceptionally(
                UnKnownException(error)
            )

            if (::enableMfaCompletableFuture.isInitialized) {
                enableMfaCompletableFuture.completeExceptionally(
                    UnKnownException(error)
                )
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.MFA_ENABLEMENT_FAILED,
                    mutableMapOf<String, Any>(
                        "duration" to System.currentTimeMillis() - startTime,
                        "error_message" to "MFA Enablement Failed: $error"
                    )
                )
            }

            if (::manageMfaCompletableFuture.isInitialized) {
                manageMfaCompletableFuture.completeExceptionally(
                    UnKnownException(error)
                )
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.MFA_MANAGEMENT_FAILED,
                    mutableMapOf<String, Any>(
                        "integration_type" to web3AuthOption.getSdkName(),
                        "dapp_url" to this.loginParams?.dappUrl.toString(),
                        "connector" to "auth",
                        "duration" to System.currentTimeMillis() - startTime,
                        "error_message" to "MFA Management Failed: $error"
                    )
                )
            }
            return
        }

        val b64Params = hashUri.getQueryParameter("b64Params")
        if (b64Params.isNullOrBlank()) {
            throwLoginError(ErrorCode.INVALID_LOGIN)
            throwEnableMFAError(ErrorCode.INVALID_LOGIN)
            throwManageMFAError(ErrorCode.INVALID_LOGIN)
            actionType?.let { processRequestFailAnalytics(it, ErrorCode.INVALID_LOGIN) }
            return
        }
        val b64ParamString = decodeBase64URLString(b64Params).toString(Charsets.UTF_8)

        if (b64ParamString.contains("actionType")) {
            val response = gson.fromJson(b64ParamString, RedirectResponse::class.java)
            if (response.actionType == "manage_mfa") {
                actionType?.let { processRequestCompleteAnalytics(it) }
                if (::manageMfaCompletableFuture.isInitialized)
                    manageMfaCompletableFuture.complete(true)
                return
            }
        }

        val sessionResponse = gson.fromJson(b64ParamString, SessionResponse::class.java)
        val sessionId = sessionResponse.sessionId

        if (sessionId.isNotBlank() && sessionId.isNotEmpty()) {
            SessionManager.saveSessionIdToStorage(sessionId)
            sessionManager.setSessionId(sessionId)

            //Rehydrate Session
            this.authorizeSession(web3AuthOption.redirectUrl, baseContext)
                .whenComplete { resp, error ->

                    runOnUIThread {
                        if (error == null) {
                            web3AuthResponse = resp
                            if (web3AuthResponse?.error?.isNotBlank() == true) {
                                throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwManageMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                                actionType?.let {
                                    processRequestFailAnalytics(
                                        it,
                                        ErrorCode.SOMETHING_WENT_WRONG
                                    )
                                }
                            } else if (web3AuthResponse?.privateKey.isNullOrBlank() && web3AuthResponse?.factorKey.isNullOrBlank()) {
                                throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwManageMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                                actionType?.let {
                                    processRequestFailAnalytics(
                                        it,
                                        ErrorCode.SOMETHING_WENT_WRONG
                                    )
                                }
                            } else {
                                web3AuthResponse?.sessionId?.let {
                                    SessionManager.saveSessionIdToStorage(it)
                                    sessionManager.setSessionId(it)
                                }

                                if (web3AuthResponse?.userInfo?.dappShare?.isNotEmpty() == true) {
                                    KeyStoreManagerUtils.encryptData(
                                        web3AuthResponse?.userInfo?.authConnectionId.plus(" | ")
                                            .plus(web3AuthResponse?.userInfo?.userId),
                                        web3AuthResponse?.userInfo?.dappShare!!,
                                    )
                                }

                                actionType?.let { processRequestCompleteAnalytics(it) }

                                if (::loginCompletableFuture.isInitialized)
                                    loginCompletableFuture.complete(web3AuthResponse)

                                if (::enableMfaCompletableFuture.isInitialized)
                                    enableMfaCompletableFuture.complete(true)

                                if (::manageMfaCompletableFuture.isInitialized)
                                    manageMfaCompletableFuture.complete(true)

                            }
                        } else {
                            print(error)
                            actionType?.let { processRequestFailAnalytics(it) }
                        }
                    }
                }
        } else {
            throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
            throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
            throwManageMFAError(ErrorCode.SOMETHING_WENT_WRONG)
        }
    }

    /**
     * Performs a login operation asynchronously.
     *
     * @param loginParams The login parameters required for authentication.
     * @return A CompletableFuture<Web3AuthResponse> representing the asynchronous operation, containing the Web3AuthResponse upon successful login.
     */
    private fun login(loginParams: LoginParams): CompletableFuture<Web3AuthResponse> {
        web3AuthOption.authConnectionConfig
            ?.firstOrNull()
            ?.let { config ->
                val decryptedShare = KeyStoreManagerUtils.decryptData(config.authConnectionId)
                if (!decryptedShare.isNullOrEmpty()) {
                    loginParams.dappShare = decryptedShare
                }
            }

        processRequest("login", loginParams)

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

    fun connectTo(
        loginParams: LoginParams
    ): CompletableFuture<Web3AuthResponse> {
        actionType = "login"

        this.loginParams = loginParams
        sessionManager = SessionManager(
            baseContext,
            web3AuthOption.sessionTime,
            web3AuthOption.redirectUrl,
            sessionNamespace = if (!loginParams.idToken.isNullOrEmpty()) "sfa" else ""
        )

        val analyticsProps = mutableMapOf<String, Any>(
            "connector" to "auth",
            "auth_connection" to loginParams.authConnection,
            "auth_connection_id" to loginParams.authConnectionId.toString(),
            "group_auth_connection_id" to loginParams.groupedAuthConnectionId.toString(),
            "chain_id" to web3AuthOption.defaultChainId.toString(),
            "dapp_url" to loginParams.dappUrl.toString(),
            "chain_id" to web3AuthOption.defaultChainId.toString(),
            "chains" to (web3AuthOption.chains?.toString() ?: "[]"),
        )

        if (loginParams.idToken.isNullOrEmpty()) {
            AnalyticsManager.trackEvent(
                AnalyticsEvents.CONNECTION_STARTED,
                analyticsProps + mutableMapOf<String, Any>(
                    "is_sfa" to false,
                )
            )
            if (!loginParams.loginHint.isNullOrEmpty()) {
                val updatedExtraLoginOptions = loginParams.extraLoginOptions?.copy(
                    login_hint = loginParams.loginHint
                ) ?: ExtraLoginOptions(login_hint = loginParams.loginHint)

                loginParams.copy(extraLoginOptions = updatedExtraLoginOptions)
            } else {
                loginParams
            }.also {
                login(it) // PnP login
            }
        } else {
            SharedPrefsHelper.putBoolean(IS_SFA, true)
            AnalyticsManager.trackEvent(
                AnalyticsEvents.CONNECTION_STARTED,
                analyticsProps + mutableMapOf<String, Any>(
                    "is_sfa" to true,
                )
            )
            loginParams.groupedAuthConnectionId?.let {
                if (it.isNullOrEmpty()) {
                    connect(loginParams, baseContext)
                } else {
                    val _loginParams = LoginParams(
                        AuthConnection.CUSTOM,
                        authConnectionId = loginParams.groupedAuthConnectionId,
                        idToken = loginParams.idToken
                    )
                    val subVerifierInfoArray = arrayOf(
                        Web3AuthSubVerifierInfo(
                            loginParams.authConnectionId.toString(),
                            idToken = loginParams.idToken.toString()
                        )
                    )
                    connect(
                        _loginParams,
                        baseContext,
                        subVerifierInfoArray = subVerifierInfoArray
                    ) // SFA login
                }
            }
            connect(loginParams, baseContext) // SFA login
        }

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

    private fun connect(
        loginParams: LoginParams,
        ctx: Context,
        subVerifierInfoArray: Array<Web3AuthSubVerifierInfo>? = null,
    ) {
        val torusKey = subVerifierInfoArray.let {
            if (it.isNullOrEmpty()) {
                getTorusKey(loginParams)
            } else {
                getTorusKey(loginParams, it)
            }
        }

        val privateKey = if (torusKey.finalKeyData?.privKey?.isEmpty() == true) {
            torusKey.getoAuthKeyData().privKey
        } else {
            torusKey.finalKeyData?.privKey
        }

        var decodedUserInfo: UserInfo?

        try {
            val jwt = loginParams.idToken?.let { decodeJwt(it) }
            jwt.let {
                decodedUserInfo = UserInfo(
                    email = it?.getClaim("email")?.asString() ?: "",
                    name = it?.getClaim("name")?.asString() ?: "",
                    profileImage = it?.getClaim("picture")?.asString() ?: "",
                    authConnectionId = loginParams.authConnectionId.toString(),
                    authConnection = AuthConnection.CUSTOM.name.lowercase(Locale.ROOT),
                    groupedAuthConnectionId = loginParams.groupedAuthConnectionId ?: "",
                    userId = it?.getClaim("user_id")?.asString() ?: "",
                )
            }
        } catch (e: Exception) {
            throw Exception(Web3AuthError.getError(ErrorCode.INVALID_LOGIN))
        }

        val response = Web3AuthResponse(
            privateKey = privateKey.toString(),
            signatures = getSignatureData(torusKey.sessionData.sessionTokenData),
            userInfo = decodedUserInfo
        )

        val sessionId = SessionManager.generateRandomSessionKey()
        sessionManager.setSessionId(sessionId)
        sessionManager.createSession(gson.toJson(response), ctx)
            .whenComplete { result, err ->
                runOnUIThread {
                    if (err == null) {
                        web3AuthResponse = response
                        SessionManager.saveSessionIdToStorage(result)
                        sessionManager.setSessionId(result)
                        val analyticsProps = mutableMapOf<String, Any>(
                            "connector" to "auth",
                            "auth_connection" to loginParams.authConnection.toString(),
                            "auth_connection_id" to loginParams.authConnectionId.toString(),
                            "group_auth_connection_id" to loginParams.groupedAuthConnectionId.toString(),
                            "chain_id" to web3AuthOption.defaultChainId.toString(),
                            "dapp_url" to loginParams.dappUrl.toString(),
                            "chain_id" to web3AuthOption.defaultChainId.toString(),
                            "chains" to (web3AuthOption.chains?.toString() ?: "[]"),
                            "integration_type" to web3AuthOption.getSdkName(),
                            "is_mfa_enabled" to (actionType == "enable_mfa"),
                            "is_sfa" to true
                        )
                        val properties =
                            analyticsProps + mapOf("duration" to System.currentTimeMillis() - startTime)

                        AnalyticsManager.trackEvent(
                            AnalyticsEvents.CONNECTION_COMPLETED,
                            properties
                        )
                        if (::loginCompletableFuture.isInitialized)
                            loginCompletableFuture.complete(web3AuthResponse)
                    } else {
                        if (::loginCompletableFuture.isInitialized)
                            loginCompletableFuture.completeExceptionally(err)
                    }
                }
            }
    }

    private fun getTorusKey(
        loginParams: LoginParams,
        subVerifierInfoArray: Array<Web3AuthSubVerifierInfo>? = null
    ): TorusKey {
        lateinit var retrieveSharesResponse: TorusKey

        val userId = getUserIdFromJWT(loginParams.idToken.toString())
        val nodeDetails: NodeDetails =
            nodeDetailManager.getNodeDetails(loginParams.authConnectionId, userId)
                .get()

        subVerifierInfoArray?.let {
            val aggregateIdTokenSeeds: ArrayList<String> = ArrayList()
            val subVerifierIds: ArrayList<String> = ArrayList()
            val verifyParams: ArrayList<VerifyParams> = ArrayList()

            for (value: Web3AuthSubVerifierInfo in it) {
                aggregateIdTokenSeeds.add(value.idToken)
                val verifyParam = VerifyParams(userId, value.idToken)
                verifyParams.add(verifyParam)
                subVerifierIds.add(value.verifier)
            }

            aggregateIdTokenSeeds.sort()
            val verifierParams = VerifierParams(
                userId.toString(), null,
                subVerifierIds.toTypedArray(), verifyParams.toTypedArray()
            )

            val aggregateIdToken = Hash.sha3String(
                java.lang.String.join(
                    29.toChar().toString(),
                    aggregateIdTokenSeeds
                )
            ).replace("0x", "")
            retrieveSharesResponse = torusUtils.retrieveShares(
                nodeDetails.torusNodeEndpoints,
                loginParams.authConnectionId.toString(),
                verifierParams,
                aggregateIdToken,
                null
            )
        } ?: run {
            val verifierParams = VerifierParams(userId.toString(), null, null, null)
            retrieveSharesResponse = torusUtils.retrieveShares(
                nodeDetails.torusNodeEndpoints,
                loginParams.authConnectionId.toString(),
                verifierParams,
                loginParams.idToken.toString(),
                null
            )
        }

        val isUpgraded = retrieveSharesResponse.metadata?.isUpgraded

        if (isUpgraded == true) {
            throw Exception(Web3AuthError.getError(ErrorCode.USER_ALREADY_ENABLED_MFA))
        }

        return retrieveSharesResponse
    }

    private fun decodeJwt(token: String): JWT {
        return try {
            JWT(token)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to decode JWT token", e)
        }
    }

    private fun getUserIdFromJWT(token: String): String? {
        return try {
            val jwt = JWT(token)
            jwt.getClaim("user_id").asString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getSignatureData(sessionTokenData: List<SessionToken>): List<String> {
        return sessionTokenData
            .filterNotNull()
            .map { session ->
                """{"data":"${session.token}","sig":"${session.signature}"}"""
            }
    }


    /**
     * Logs out the user asynchronously.
     *
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun logout(): CompletableFuture<Void> {
        AnalyticsManager.trackEvent(
            AnalyticsEvents.LOGOUT_STARTED
        )
        val logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()
        val sessionResponse: CompletableFuture<Boolean>? =
            sessionManager.invalidateSession(baseContext)
        sessionResponse?.whenComplete { _, error ->
            SessionManager.deleteSessionIdFromStorage()
            runOnUIThread {
                if (error == null) {
                    AnalyticsManager.trackEvent(
                        AnalyticsEvents.LOGOUT_COMPLETED
                    )
                    logoutCompletableFuture.complete(null)
                } else {
                    AnalyticsManager.trackEvent(
                        AnalyticsEvents.LOGOUT_FAILED,
                        mutableMapOf<String, Any>(
                            "error_message" to "Logout Failed: ${error.message}"
                        )
                    )
                    logoutCompletableFuture.completeExceptionally(Exception(error))
                }
                AnalyticsManager.reset()
            }
        }
        SharedPrefsHelper.clear()
        web3AuthResponse = Web3AuthResponse()
        return logoutCompletableFuture
    }

    /**
     * Enables Multi-Factor Authentication (MFA) asynchronously.
     *
     * @param loginParams The optional login parameters required for authentication. Default is null.
     * @return A CompletableFuture<Boolean> representing the asynchronous operation, indicating whether MFA was successfully enabled.
     */
    fun enableMFA(loginParams: LoginParams? = null): CompletableFuture<Boolean> {
        actionType = "enable_mfa"
        AnalyticsManager.trackEvent(
            AnalyticsEvents.MFA_ENABLEMENT_STARTED,
            mutableMapOf<String, Any>(
                "integration_type" to web3AuthOption.getSdkName(),
                "dapp_url" to this.loginParams?.dappUrl.toString(),
                "connector" to "auth",
                "duration" to System.currentTimeMillis() - startTime,
            )
        )
        enableMfaCompletableFuture = CompletableFuture()
        if (web3AuthResponse?.userInfo?.isMfaEnabled == true) {
            throwEnableMFAError(ErrorCode.MFA_ALREADY_ENABLED)
            return enableMfaCompletableFuture
        }
        val sessionId = sessionManager.getSessionId()
        if (sessionId.isBlank()) {
            throwEnableMFAError(ErrorCode.NOUSERFOUND)
            return enableMfaCompletableFuture
        }
        processRequest("enable_mfa", loginParams)
        return enableMfaCompletableFuture
    }


    fun manageMFA(loginParams: LoginParams? = null): CompletableFuture<Boolean> {
        actionType = "manage_mfa"
        AnalyticsManager.trackEvent(
            AnalyticsEvents.MFA_MANAGEMENT_STARTED,
            mutableMapOf<String, Any>(
                "integration_type" to web3AuthOption.getSdkName(),
                "dapp_url" to this.loginParams?.dappUrl.toString(),
                "connector" to "auth"
            )
        )
        manageMfaCompletableFuture = CompletableFuture()
        if (web3AuthResponse?.userInfo?.isMfaEnabled == false) {
            throwManageMFAError(ErrorCode.MFA_NOT_ENABLED)
            return manageMfaCompletableFuture
        }
        val sessionId = sessionManager.getSessionId()
        if (sessionId.isBlank()) {
            throwManageMFAError(ErrorCode.NOUSERFOUND)
            return manageMfaCompletableFuture
        }
        processRequest("manage_mfa", loginParams)
        return manageMfaCompletableFuture
    }

    /**
     * Authorize User session in order to avoid re-login
     */
    private fun authorizeSession(
        origin: String,
        context: Context
    ): CompletableFuture<Web3AuthResponse> {
        val sessionCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
        val sessionResponse: CompletableFuture<String> =
            sessionManager.authorizeSession(origin, context)
        sessionResponse.whenComplete { response, error ->
            if (error != null) {
                sessionCompletableFuture.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.NOUSERFOUND
                        )
                    )
                )
            } else {
                val tempJson = JSONObject(response)
                web3AuthResponse = gson.fromJson(tempJson.toString(), Web3AuthResponse::class.java)
                if (web3AuthResponse?.error?.isNotBlank() == true) {
                    sessionCompletableFuture.completeExceptionally(
                        UnKnownException(
                            web3AuthResponse?.error ?: Web3AuthError.getError(
                                ErrorCode.SOMETHING_WENT_WRONG
                            )
                        )
                    )
                } else if (web3AuthResponse?.privateKey.isNullOrBlank() && web3AuthResponse?.factorKey.isNullOrBlank()) {
                    sessionCompletableFuture.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)
                        )
                    )
                } else {
                    sessionCompletableFuture.complete(web3AuthResponse)
                }
            }
        }
        return sessionCompletableFuture
    }

    private fun fetchProjectConfig(): CompletableFuture<Boolean> {
        val projectConfigCompletableFuture: CompletableFuture<Boolean> = CompletableFuture()
        val web3AuthApi =
            ApiHelper.getInstance(web3AuthOption.web3AuthNetwork.name)
                .create(ApiService::class.java)
        if (!ApiHelper.isNetworkAvailable(baseContext)) {
            throw Exception(
                Web3AuthError.getError(ErrorCode.RUNTIME_ERROR)
            )
        }
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val result = web3AuthApi.fetchProjectConfig(
                    project_id = web3AuthOption.clientId,
                    network = web3AuthOption.web3AuthNetwork.name.lowercase(),
                    build_env = web3AuthOption.authBuildEnv.name.lowercase()
                )
                if (result.isSuccessful && result.body() != null) {
                    projectConfigResponse = result.body()
                    // Set global properties for analytics after fetching project config
                    AnalyticsManager.setGlobalProperties(
                        mapOf(
                            "sdk_name" to web3AuthOption.getSdkName(),
                            "sdk_version" to web3AuthOption.getSdkVersion(),
                            "web3auth_client_id" to web3AuthOption.clientId,
                            "web3auth_network" to web3AuthOption.web3AuthNetwork,
                            "team_id" to projectConfigResponse?.teamId.toString(),
                        )
                    )
                    val response = result.body()
                    web3AuthOption.originData =
                        web3AuthOption.originData.mergeMaps(response?.whitelist?.signed_urls)
                    response?.whitelabel?.let { whitelabel ->
                        web3AuthOption.whiteLabel =
                            web3AuthOption.whiteLabel?.merge(whitelabel) ?: whitelabel

                        web3AuthOption.walletServicesConfig?.apply {
                            whiteLabel = whiteLabel?.merge(whitelabel) ?: whitelabel
                        }
                    }
                    web3AuthOption.authConnectionConfig =
                        (web3AuthOption.authConnectionConfig.orEmpty() + projectConfigResponse?.embeddedWalletAuth.orEmpty())
                    web3AuthOption.mfaSettings =
                        web3AuthOption.mfaSettings?.merge(projectConfigResponse?.mfaSettings)
                            ?: projectConfigResponse?.mfaSettings
                    projectConfigCompletableFuture.complete(true)
                } else {
                    projectConfigCompletableFuture.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.PROJECT_CONFIG_NOT_FOUND_ERROR
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                projectConfigCompletableFuture.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.SOMETHING_WENT_WRONG
                        )
                    )
                )
            }
        }
        return projectConfigCompletableFuture
    }


    /**
     * Retrieves the login ID from the provided JSONObject asynchronously.
     *
     * @param jsonObject The JSONObject from which to retrieve the login ID.
     * @return A CompletableFuture<String> representing the asynchronous operation, containing the login ID.
     */
    private fun getLoginId(sessionId: String, jsonObject: String): CompletableFuture<String> {
        sessionManager.setSessionId(sessionId)
        return sessionManager.createSession(
            jsonObject,
            baseContext,
        )
    }

    /**
     * Launches the wallet services asynchronously.
     *
     * @param path The path where the wallet services will be launched. Default value is "wallet".
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun showWalletUI(
        path: String? = "wallet",
    ): CompletableFuture<Void> {
        AnalyticsManager.trackEvent(
            AnalyticsEvents.WALLET_UI_CLICKED,
            mutableMapOf<String, Any>(
                "integration_type" to web3AuthOption.getSdkName(),
                "dapp_url" to this.loginParams?.dappUrl.toString(),
            )
        )
        val launchWalletServiceCF: CompletableFuture<Void> = CompletableFuture()
        val savedSessionId = SessionManager.getSessionIdFromStorage()
        if (savedSessionId.isNotBlank()) {
            val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)

            // If chains are not present in project config, throw an error
            if (projectConfigResponse?.chains == null) {
                throw Exception(Web3AuthError.getError(ErrorCode.PROJECT_CONFIG_NOT_FOUND_ERROR))
            }
            val initOptions = JSONObject(gson.toJson(web3AuthOption)).apply {
                put("network", web3AuthOption.web3AuthNetwork.toString().lowercase(Locale.ROOT))
                projectConfigResponse?.chains?.let {
                    put("chains", gson.toJson(it))
                    put(
                        "defaultChainId",
                        it.firstOrNull()?.chainId ?: web3AuthOption.defaultChainId ?: "0x1"
                    )
                    put(
                        "chainId",
                        it.firstOrNull()?.chainId ?: web3AuthOption.defaultChainId ?: "0x1"
                    )
                }
                projectConfigResponse?.embeddedWalletAuth?.let {
                    put("embeddedWalletAuth", JSONArray(gson.toJson(it)))
                }
                projectConfigResponse?.smartAccounts?.let {
                    put("accountAbstractionConfig", JSONObject(gson.toJson(it)))
                }
            }

            val paramMap = JSONObject()
            paramMap.put(
                "options", initOptions
            )
            val sessionId = SessionManager.generateRandomSessionKey()
            val loginIdCf = getLoginId(sessionId, paramMap.toString())

            loginIdCf.whenComplete { loginId, error ->
                if (error == null) {
                    val walletMap = JsonObject()
                    walletMap.addProperty(
                        "loginId", loginId
                    )
                    walletMap.addProperty("sessionId", savedSessionId)
                    walletMap.addProperty("platform", "android")
                    val isSFAValue = SharedPrefsHelper.getBoolean(IS_SFA)
                    if (isSFAValue) {
                        walletMap.addProperty("sessionNamespace", "sfa")
                    }

                    val walletHash =
                        "b64Params=" + gson.toJson(walletMap).toByteArray(Charsets.UTF_8)
                            .toBase64URLString()

                    val url =
                        Uri.Builder().scheme(sdkUrl.scheme)
                            .encodedAuthority(sdkUrl.encodedAuthority)
                            .encodedPath(sdkUrl.encodedPath).appendPath(path)
                            .fragment(walletHash).build()
                    //print("wallet launch url: => $url")
                    val intent = Intent(baseContext, WebViewActivity::class.java)
                    intent.putExtra(WEBVIEW_URL, url.toString())
                    baseContext.startActivity(intent)
                    launchWalletServiceCF.complete(null)
                }
            }
        } else {
            AnalyticsManager.trackEvent(
                AnalyticsEvents.WALLET_SERVICES_FAILED,
                mutableMapOf<String, Any>(
                    "integration_type" to web3AuthOption.getSdkName(),
                    "dapp_url" to this.loginParams?.dappUrl.toString(),
                    "duration" to System.currentTimeMillis() - startTime,
                    "error" to "Wallet Services Error: Session ID is not found. Please login first."
                )
            )
            launchWalletServiceCF.completeExceptionally(Exception("Please login first to launch wallet"))
        }
        return launchWalletServiceCF
    }

    /**
     * Signs a message asynchronously.
     *
     * @param method The method name of the request.
     * @param requestParams The parameters of the request in JSON array format.
     * @param path The path where the signing service is located. Default value is "wallet/request".
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun request(
        method: String,
        requestParams: JsonArray,
        path: String? = "wallet/request",
        appState: String? = null
    ): CompletableFuture<SignResponse> {
        AnalyticsManager.trackEvent(
            AnalyticsEvents.REQUEST_FUNCTION_STARTED
        )
        signMsgCF = CompletableFuture()
        WebViewActivity.webViewResultCallback = this

        val sessionId = SessionManager.getSessionIdFromStorage()
        if (sessionId.isNotBlank()) {
            val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)

            // If chains are not present in project config, throw an error
            if (projectConfigResponse?.chains == null) {
                throw Exception(Web3AuthError.getError(ErrorCode.PROJECT_CONFIG_NOT_FOUND_ERROR))
            }

            val initOptions = JSONObject(gson.toJson(web3AuthOption))
            initOptions.apply {
                put("network", web3AuthOption.web3AuthNetwork.toString().lowercase(Locale.ROOT))
                projectConfigResponse?.chains?.let {
                    put("chains", gson.toJson(it))
                    put(
                        "defaultChainId",
                        it.firstOrNull()?.chainId ?: web3AuthOption.defaultChainId ?: "0x1"
                    )
                    put(
                        "chainId",
                        it.firstOrNull()?.chainId ?: web3AuthOption.defaultChainId ?: "0x1"
                    )
                }
                projectConfigResponse?.embeddedWalletAuth?.let {
                    initOptions.put("embeddedWalletAuth", JSONArray(gson.toJson(it)))
                }
                projectConfigResponse?.smartAccounts?.let {
                    put("accountAbstractionConfig", JSONObject(gson.toJson(it)))
                }
            }

            val paramMap = JSONObject()
            paramMap.put(
                "options", initOptions
            )

            val loginId = SessionManager.generateRandomSessionKey()
            val loginIdCf = getLoginId(loginId, paramMap.toString())

            loginIdCf.whenComplete { loginId, error ->
                if (error == null) {
                    val signMessageMap = mutableMapOf<String, Any>(
                        "loginId" to loginId,
                        "sessionId" to sessionId,
                        "platform" to "android",
                        "request" to mapOf(
                            "method" to method,
                            "params" to gson.toJson(requestParams)
                        ),
                        "appState" to gson.toJson(appState)
                    )

                    val isSFAValue = SharedPrefsHelper.getBoolean(IS_SFA)
                    if (isSFAValue) {
                        signMessageMap["sessionNamespace"] = "sfa"
                    }

                    val signMessageHash =
                        "b64Params=" + gson.toJson(signMessageMap).toByteArray(Charsets.UTF_8)
                            .toBase64URLString()

                    val url =
                        Uri.Builder().scheme(sdkUrl.scheme)
                            .encodedAuthority(sdkUrl.encodedAuthority)
                            .encodedPath(sdkUrl.encodedPath).appendEncodedPath(path)
                            .fragment(signMessageHash).build()
                    //print("message signing url: => $url")
                    val intent = Intent(baseContext, WebViewActivity::class.java)
                    intent.putExtra(WEBVIEW_URL, url.toString())
                    intent.putExtra(REDIRECT_URL, web3AuthOption.redirectUrl)
                    baseContext.startActivity(intent)
                }
            }
        } else {
            runOnUIThread {
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.REQUEST_FUNCTION_FAILED,
                    mutableMapOf<String, Any>(
                        "duration" to System.currentTimeMillis() - startTime,
                        "error" to "Request Function Error: Session ID is not found. Please login first."
                    )
                )
                signMsgCF.completeExceptionally(Exception("Please login first to launch wallet"))
            }
        }
        return signMsgCF
    }

    private fun runOnUIThread(action: () -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(action)
    }

    private fun throwEnableMFAError(error: ErrorCode) {
        if (::enableMfaCompletableFuture.isInitialized)
            enableMfaCompletableFuture.completeExceptionally(
                Exception(
                    Web3AuthError.getError(
                        error
                    )
                )
            )
    }

    private fun throwManageMFAError(error: ErrorCode) {
        if (::manageMfaCompletableFuture.isInitialized)
            manageMfaCompletableFuture.completeExceptionally(
                Exception(
                    Web3AuthError.getError(
                        error
                    )
                )
            )
    }

    private fun throwLoginError(error: ErrorCode) {
        if (::loginCompletableFuture.isInitialized) {
            loginCompletableFuture.completeExceptionally(
                Exception(
                    Web3AuthError.getError(
                        error
                    )
                )
            )
        }
    }

    /**
     * Retrieves the private key as a string.
     *
     * @return The private key as a string.
     */
    fun getPrivateKey(): String {
        val privKey: String? = if (web3AuthResponse == null) {
            ""
        } else {
            if (web3AuthOption.useSFAKey == true) {
                web3AuthResponse?.coreKitKey
            } else {
                web3AuthResponse?.privateKey
            }
        }
        return privKey
            ?: throw IllegalStateException("No valid private key found")
    }

    /**
     * Retrieves the Ed25519 private key as a string.
     *
     * @return The Ed25519 private key as a string.
     */
    fun getEd25519PrivateKey(): String {
        val ed25519Key: String? = if (web3AuthResponse == null) {
            null
        } else {
            if (web3AuthOption.useSFAKey == true) {
                web3AuthResponse?.coreKitEd25519PrivKey
            } else {
                web3AuthResponse?.ed25519PrivKey
            }
        }

        return ed25519Key
            ?: throw IllegalStateException("No valid Ed25519 private key found")
    }

    /**
     * Retrieves user information if available.
     *
     * @return The user information if available, or null if not available.
     */
    fun getUserInfo(): UserInfo? {
        return if (web3AuthResponse == null) {
            throw Error(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
        } else {
            web3AuthResponse?.userInfo
        }
    }

    /**
     * Retrieves the Web3AuthResponse if available.
     *
     * @return The Web3AuthResponse if available, or null if not available.
     */
    fun getWeb3AuthResponse(): Web3AuthResponse? {
        return if (web3AuthResponse == null) {
            throw Error(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
        } else {
            web3AuthResponse
        }
    }

    private fun processRequestFailAnalytics(actionType: String, error: ErrorCode? = null) {
        val event = when (actionType) {
            "login" -> AnalyticsEvents.CONNECTION_FAILED
            "enable_mfa" -> AnalyticsEvents.MFA_ENABLEMENT_FAILED
            else -> AnalyticsEvents.MFA_ENABLEMENT_FAILED
        }

        val properties = mapOf(
            "connector" to "auth",
            "auth_connection" to loginParams?.authConnection,
            "auth_connection_id" to loginParams?.authConnectionId.toString(),
            "group_auth_connection_id" to loginParams?.groupedAuthConnectionId.toString(),
            "chain_id" to web3AuthOption.defaultChainId.toString(),
            "dapp_url" to loginParams?.dappUrl.toString(),
            "chain_id" to web3AuthOption.defaultChainId.toString(),
            "chains" to (web3AuthOption.chains?.toString() ?: "[]"),
            "duration" to System.currentTimeMillis() - startTime,
            "error_message" to (error?.name ?: "Unknown Error")
        )

        AnalyticsManager.trackEvent(event, properties)
    }

    private fun processRequestCompleteAnalytics(actionType: String) {
        val event = when (actionType) {
            "login" -> AnalyticsEvents.CONNECTION_COMPLETED
            "enable_mfa" -> AnalyticsEvents.MFA_ENABLEMENT_COMPLETED
            else -> AnalyticsEvents.MFA_MANAGEMENT_COMPLETED
        }

        val analyticsProps = mutableMapOf<String, Any>(
            "connector" to "auth",
            "auth_connection" to loginParams?.authConnection.toString(),
            "auth_connection_id" to loginParams?.authConnectionId.toString(),
            "group_auth_connection_id" to loginParams?.groupedAuthConnectionId.toString(),
            "chain_id" to web3AuthOption.defaultChainId.toString(),
            "dapp_url" to loginParams?.dappUrl.toString(),
            "chain_id" to web3AuthOption.defaultChainId.toString(),
            "chains" to (web3AuthOption.chains?.toString() ?: "[]"),
            "integration_type" to web3AuthOption.getSdkName(),
            "is_mfa_enabled" to (actionType == "enable_mfa"),
            "is_sfa" to false
        )
        val properties =
            analyticsProps + mapOf("duration" to System.currentTimeMillis() - startTime)

        AnalyticsManager.trackEvent(event, properties)
    }

    companion object {
        @JvmStatic
        private var isCustomTabsClosed: Boolean = false

        @JvmStatic
        fun setCustomTabsClosed(_isCustomTabsClosed: Boolean) {
            isCustomTabsClosed = _isCustomTabsClosed
        }

        @JvmStatic
        fun getCustomTabsClosed(): Boolean {
            return isCustomTabsClosed
        }
    }

    override fun onSignResponseReceived(signResponse: SignResponse?) {
        if (signResponse != null) {
            signMsgCF.complete(signResponse)
            AnalyticsManager.trackEvent(
                AnalyticsEvents.REQUEST_FUNCTION_COMPLETED,
                mutableMapOf<String, Any>(
                    "duration" to System.currentTimeMillis() - startTime,
                )
            )
        }
    }

    override fun onWebViewCancelled() {
        signMsgCF.completeExceptionally(Exception("User cancelled the operation."))
        AnalyticsManager.trackEvent(
            AnalyticsEvents.REQUEST_FUNCTION_FAILED,
            mutableMapOf<String, Any>(
                "duration" to System.currentTimeMillis() - startTime,
                "error" to "User cancelled the operation."
            )
        )
    }
}

