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
import com.web3auth.core.analytics.AnalyticsIntegrationType
import com.web3auth.core.analytics.AnalyticsManager
import com.web3auth.core.analytics.AnalyticsSdkType
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.api.ApiService
import com.web3auth.core.keystore.IS_SFA
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.keystore.SharedPrefsHelper
import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.ConfirmationStrategy
import com.web3auth.core.types.DEFAULT_SESSION_TIME
import com.web3auth.core.types.ErrorCode
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.LOGIN_SOURCE_ANDROID
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.ProjectConfigResponse
import com.web3auth.core.types.REDIRECT_URL
import com.web3auth.core.types.RedirectResponse
import com.web3auth.core.types.SessionResponse
import com.web3auth.core.types.SignResponse
import com.web3auth.core.types.SmartAccountWalletScope
import com.web3auth.core.types.UnKnownException
import com.web3auth.core.types.UserCancelledException
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.WEBVIEW_URL
import com.web3auth.core.types.WalletServicesConfig
import com.web3auth.core.types.Web3AuthError
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.Web3AuthSubVerifierInfo
import com.web3auth.core.types.WebViewResultCallback
import com.web3auth.session_manager_android.StorageManager
import com.web3auth.session_manager_android.auth.ApiClientConfig
import com.web3auth.session_manager_android.auth.AuthSessionManager
import com.web3auth.session_manager_android.auth.AuthTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.torusresearch.fetchnodedetails.FetchNodeDetails
import org.torusresearch.fetchnodedetails.types.NodeDetails
import org.torusresearch.torusutils.TorusUtils
import org.torusresearch.torusutils.types.RetrieveSharesParams
import org.torusresearch.torusutils.types.VerifierParams
import org.torusresearch.torusutils.types.VerifyParams
import org.torusresearch.torusutils.types.common.SessionToken
import org.torusresearch.torusutils.types.common.TorusKey
import org.torusresearch.torusutils.types.common.TorusKeyType
import org.torusresearch.torusutils.types.common.TorusOptions
import org.torusresearch.fetchnodedetails.types.BuildEnv as FndBuildEnv
import com.web3auth.core.types.BuildEnv as AuthBuildEnv
import org.web3j.crypto.Hash
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture

class Web3Auth(web3AuthOptions: Web3AuthOptions, context: Context) : WebViewResultCallback,
    ContextWrapper(context) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private lateinit var loginCompletableFuture: CompletableFuture<Web3AuthResponse>
    private lateinit var enableMfaCompletableFuture: CompletableFuture<Boolean>
    private lateinit var manageMfaCompletableFuture: CompletableFuture<Boolean>
    private lateinit var signMsgCF: CompletableFuture<SignResponse>

    private var nodeDetailManager: FetchNodeDetails
    private val torusUtils: TorusUtils
    private var web3AuthResponse: Web3AuthResponse? = null
    private var web3AuthOption = web3AuthOptions
    /** Session-service storage for ephemeral login payloads (`/start` loginId). */
    private var storageManager: StorageManager
    /** Citadel token session manager. */
    private var authSessionManager: AuthSessionManager
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
                "sdk_name" to AnalyticsSdkType.ANDROID,
                "sdk_version" to AnalyticsEvents.SDK_VERSION,
                "web3auth_client_id" to web3AuthOptions.clientId,
                "web3auth_network" to web3AuthOptions.web3AuthNetwork,
                "integration_type" to AnalyticsIntegrationType.NATIVE_SDK,
            )
        )

        val fndBuildEnv = toFndBuildEnv(web3AuthOptions.authBuildEnv)
        nodeDetailManager = FetchNodeDetails(
            web3AuthOptions.web3AuthNetwork,
            fndBuildEnv
        )
        val torusOptions = TorusOptions(
            web3AuthOptions.clientId,
            web3AuthOptions.web3AuthNetwork,
            fndBuildEnv,
            null,
            0,
            true,
            TorusKeyType.secp256k1,
            LOGIN_SOURCE_ANDROID
        )
        torusUtils = TorusUtils(torusOptions)
        SharedPrefsHelper.init(context.applicationContext)
        val isSFAValue = SharedPrefsHelper.getBoolean(IS_SFA)
        val sessionNamespace =
            web3AuthOptions.sessionNamespace?.takeIf { it.isNotBlank() }
                ?: if (isSFAValue) "sfa" else null

        storageManager = createStorageManager(
            context = context,
            sessionNamespace = sessionNamespace
        )
        authSessionManager = createAuthSessionManager(context)
    }

    private fun toFndBuildEnv(buildEnv: AuthBuildEnv): FndBuildEnv = when (buildEnv) {
        AuthBuildEnv.STAGING -> FndBuildEnv.STAGING
        AuthBuildEnv.TESTING -> FndBuildEnv.TESTING
        AuthBuildEnv.PRODUCTION -> FndBuildEnv.PRODUCTION
    }

    /**
     * Wallet v6 (Auth v11 / ws-embed 6) rehydrates via citadel using
     * `sessionId` + `accessToken` from the mobile hash — not a session-service
     * mirror. SFA falls back to the stored session-service session id when
     * citadel tokens are absent.
     */
    private data class WalletLaunchCreds(
        val sessionId: String,
        val accessToken: String?,
        val idToken: String?,
        val refreshToken: String?,
    )

    private fun resolveWalletLaunchCreds(): CompletableFuture<WalletLaunchCreds> {
        return authSessionManager.getSessionIdAsync()
            .thenCombine(authSessionManager.getAccessTokenAsync()) { session, access ->
                session to access
            }
            .thenCombine(authSessionManager.getIdTokenAsync()) { pair, idToken ->
                Triple(pair.first, pair.second, idToken)
            }
            .thenCombine(authSessionManager.getRefreshTokenAsync()) { triple, refresh ->
                val sessionId = triple.first?.takeIf { it.isNotBlank() }
                    ?: StorageManager.getSessionIdFromStorage().takeIf { it.isNotBlank() }
                    ?: throw Exception("Please login first to launch wallet")
                // Wallet v6 APP_SCOPED requires accessToken for api-wallet Bearer auth.
                // SFA (session-service only) may omit it.
                val isSfa = SharedPrefsHelper.getBoolean(IS_SFA)
                if (!isSfa && triple.second.isNullOrBlank()) {
                    throw Exception("Missing accessToken for wallet services. Please login again.")
                }
                WalletLaunchCreds(
                    sessionId = sessionId,
                    accessToken = triple.second?.takeIf { it.isNotBlank() },
                    idToken = triple.third?.takeIf { it.isNotBlank() },
                    refreshToken = refresh?.takeIf { it.isNotBlank() },
                )
            }
    }

    private fun JsonObject.addWalletAuthCreds(creds: WalletLaunchCreds) {
        addProperty("sessionId", creds.sessionId.strip0xForWalletSession())
        creds.accessToken?.let { addProperty("accessToken", it) }
        creds.idToken?.let { addProperty("idToken", it) }
        creds.refreshToken?.let { addProperty("refreshToken", it) }
    }

    private fun createStorageManager(
        context: Context,
        sessionNamespace: String?,
        sessionId: String? = null,
        allowedOrigin: String? = null,
    ): StorageManager {
        return StorageManager(
            context = context,
            sessionServerBaseUrl = web3AuthOption.storageServerUrl
                ?: throw IllegalStateException("storageServerUrl is required"),
            sessionTime = web3AuthOption.sessionTime ?: DEFAULT_SESSION_TIME,
            allowedOrigin = allowedOrigin ?: web3AuthOption.redirectUrl,
            sessionId = sessionId,
            sessionNamespace = sessionNamespace,
        )
    }

    private fun createAuthSessionManager(context: Context): AuthSessionManager {
        val citadelUrl = web3AuthOption.citadelServerUrl
            ?: throw IllegalStateException("citadelServerUrl is required")
        return AuthSessionManager(
            context = context.applicationContext,
            apiClientConfig = ApiClientConfig(baseURL = citadelUrl),
        )
    }

    private fun generateRecordId(): String = UUID.randomUUID().toString()

    private fun resolveSessionNamespace(): String? {
        val isSFAValue = SharedPrefsHelper.getBoolean(IS_SFA)
        return web3AuthOption.sessionNamespace?.takeIf { it.isNotBlank() }
            ?: if (isSFAValue) "sfa" else null
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

        if (actionType == "enable_mfa" || actionType == "manage_mfa") {
            refreshSession().whenComplete { _, error ->
                if (error != null) {
                    if (actionType == "enable_mfa") throwEnableMFAError(ErrorCode.NOUSERFOUND)
                    else throwManageMFAError(ErrorCode.NOUSERFOUND)
                    return@whenComplete
                }
                launchAuthRequest(actionType, params)
            }
        } else {
            launchAuthRequest(actionType, params)
        }
    }

    private fun launchAuthRequest(actionType: String, params: LoginParams?) {
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

        val sessionId = StorageManager.generateRandomSessionKey()
        val recordId = params?.recordId?.takeIf { it.isNotBlank() } ?: generateRecordId()
        val loginSource =
            params?.loginSource?.takeIf { it.isNotBlank() } ?: LOGIN_SOURCE_ANDROID

        val paramMap = JSONObject()
        paramMap.put("options", initOptionsJson)
        paramMap.put("actionType", actionType)

        if (actionType == "enable_mfa" || actionType == "manage_mfa") {
            val userInfo = web3AuthResponse?.userInfo
            initParamsJson.put("authConnection", userInfo?.authConnection)
            initParamsJson.put("authConnectionId", userInfo?.authConnectionId)
            initParamsJson.put("groupedAuthConnectionId", userInfo?.groupedAuthConnectionId)
            var existingExtraLoginOptions = ExtraLoginOptions()
            if (initParamsJson.has("extraLoginOptions")) {
                val extraOptionsString = initParamsJson.getString("extraLoginOptions")
                existingExtraLoginOptions =
                    gson.fromJson(extraOptionsString, ExtraLoginOptions::class.java)
            }
            existingExtraLoginOptions.login_hint = userInfo?.userId
            initParamsJson.put("extraLoginOptions", gson.toJson(existingExtraLoginOptions))
            initParamsJson.put("mfaLevel", MFALevel.MANDATORY.name.lowercase(Locale.ROOT))
            if (actionType == "manage_mfa") {
                val loginIdObject = mapOf("loginId" to sessionId, "recordId" to recordId)
                initParamsJson.put(
                    "appState",
                    gson.toJson(loginIdObject).toByteArray(Charsets.UTF_8).toBase64URLString()
                )
            } else {
                val loginIdObject = mapOf("loginId" to sessionId, "platform" to "android")
                initParamsJson.put(
                    "appState",
                    gson.toJson(loginIdObject).toByteArray(Charsets.UTF_8).toBase64URLString()
                )
            }
            authSessionManager.getSessionIdAsync().whenComplete { storedSessionId, _ ->
                if (!storedSessionId.isNullOrBlank()) {
                    paramMap.put("sessionId", storedSessionId)
                }
                authSessionManager.getAccessTokenAsync().whenComplete { accessToken, _ ->
                    if (!accessToken.isNullOrBlank()) {
                        paramMap.put("accessToken", accessToken)
                    }
                    paramMap.put("params", initParamsJson)
                    storeAndOpenStartUrl(
                        sessionId = sessionId,
                        recordId = recordId,
                        loginSource = loginSource,
                        paramMap = paramMap,
                        sdkUrl = sdkUrl,
                    )
                }
            }
            return
        }

        paramMap.put("params", initParamsJson)
        storeAndOpenStartUrl(
            sessionId = sessionId,
            recordId = recordId,
            loginSource = loginSource,
            paramMap = paramMap,
            sdkUrl = sdkUrl,
        )
    }

    private fun storeAndOpenStartUrl(
        sessionId: String,
        recordId: String,
        loginSource: String,
        paramMap: JSONObject,
        sdkUrl: Uri,
    ) {
        var paramsString = paramMap.toString().replace("\\/", "/")
        val loginIdCf = getLoginId(sessionId, paramsString)
        loginIdCf.whenComplete { loginId, error ->
            if (error == null) {
                if (web3AuthOption.whiteLabel?.consentRequired == true) {
                    AnalyticsManager.trackEvent(AnalyticsEvents.USER_CONSENT_STARTED)
                }
                val configParams = mutableMapOf(
                    "loginId" to loginId,
                    "recordId" to recordId,
                    "loginSource" to loginSource,
                )
                resolveSessionNamespace()?.let { configParams["sessionNamespace"] = it }
                web3AuthOption.storageServerUrl?.let { configParams["storageServerUrl"] = it }

                val hash = "b64Params=" + gson.toJson(configParams).toByteArray(Charsets.UTF_8)
                    .toBase64URLString()

                val url =
                    Uri.Builder().scheme(sdkUrl.scheme).encodedAuthority(sdkUrl.encodedAuthority)
                        .encodedPath(sdkUrl.encodedPath).appendPath("start").fragment(hash).build()
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
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.SDK_INITIALIZATION_COMPLETED,
                    buildInitializationAnalyticsProperties()
                )
                // Rehydrate session via citadel with session-service fallback for SFA.
                this.authorizeSession(web3AuthOption.redirectUrl, baseContext)
                    .whenComplete { resp, error ->
                        runOnUIThread {
                            if (error == null && resp != null) {
                                web3AuthResponse = resp
                                initializeCf.complete(null)
                            } else {
                                authSessionManager.clearSessionDataAsync()
                                StorageManager.deleteSessionIdFromStorage()
                                initializeCf.completeExceptionally(
                                    error ?: Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
                                )
                            }
                        }
                    }
            } else {
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.SDK_INITIALIZATION_FAILED,
                    mutableMapOf<String, Any>(
                        "integration_type" to AnalyticsIntegrationType.NATIVE_SDK,
                        "dapp_url" to this.loginParams?.dappUrl.toString(),
                        "duration" to System.currentTimeMillis() - startTime,
                        "error_code" to ErrorCode.PROJECT_CONFIG_NOT_FOUND_ERROR.name,
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
                trackConsentIfNeeded(AnalyticsEvents.USER_CONSENT_DECLINED)
                loginCompletableFuture.completeExceptionally(UserCancelledException())
                return
            }
        }
        val hashUri = Uri.parse(uri?.host + "?" + uri?.fragment)
        val error = uri?.getQueryParameter("error")
        if (error != null) {
            trackConsentIfNeeded(AnalyticsEvents.USER_CONSENT_ERRORED)
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
                        "integration_type" to "android",
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
                // manage_mfa redirect may include a refreshed token set
                if (!response.sessionId.isNullOrBlank()) {
                    persistAuthTokens(
                        SessionResponse(
                            sessionId = response.sessionId,
                            accessToken = response.accessToken,
                            refreshToken = response.refreshToken,
                            idToken = response.idToken,
                        )
                    ).whenComplete { _, _ ->
                        actionType?.let { processRequestCompleteAnalytics(it) }
                        if (::manageMfaCompletableFuture.isInitialized)
                            manageMfaCompletableFuture.complete(true)
                    }
                    return
                }
                actionType?.let { processRequestCompleteAnalytics(it) }
                if (::manageMfaCompletableFuture.isInitialized)
                    manageMfaCompletableFuture.complete(true)
                return
            }
        }

        val sessionResponse = gson.fromJson(b64ParamString, SessionResponse::class.java)
        val sessionId = sessionResponse.sessionId

        if (sessionId.isNotBlank() && sessionId.isNotEmpty()) {
            persistAuthTokens(sessionResponse).whenComplete { _, tokenError ->
                if (tokenError != null) {
                    actionType?.let { processRequestFailAnalytics(it, ErrorCode.SOMETHING_WENT_WRONG) }
                    throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                    throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                    throwManageMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                    return@whenComplete
                }
                // Rehydrate Session via citadel
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
                                        StorageManager.saveSessionIdToStorage(it)
                                    }

                                    if (web3AuthResponse?.userInfo?.dappShare?.isNotEmpty() == true) {
                                        KeyStoreManagerUtils.encryptData(
                                            web3AuthResponse?.userInfo?.authConnectionId.plus(" | ")
                                                .plus(web3AuthResponse?.userInfo?.userId),
                                            web3AuthResponse?.userInfo?.dappShare!!,
                                        )
                                    }

                                    val completeLoginUi = {
                                        actionType?.let { processRequestCompleteAnalytics(it) }
                                        if (actionType == "login") {
                                            trackConsentIfNeeded(AnalyticsEvents.USER_CONSENT_ACCEPTED)
                                        }

                                        if (::loginCompletableFuture.isInitialized)
                                            loginCompletableFuture.complete(web3AuthResponse)

                                        if (::enableMfaCompletableFuture.isInitialized)
                                            enableMfaCompletableFuture.complete(true)

                                        if (::manageMfaCompletableFuture.isInitialized)
                                            manageMfaCompletableFuture.complete(true)
                                    }

                                    // Wallet v6 uses citadel sessionId + accessToken at launch time.
                                    completeLoginUi()
                                }
                            } else {
                                print(error)
                                actionType?.let { processRequestFailAnalytics(it) }
                            }
                        }
                    }
            }
        } else {
            throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
            throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
            throwManageMFAError(ErrorCode.SOMETHING_WENT_WRONG)
        }
    }

    private fun persistAuthTokens(sessionResponse: SessionResponse): CompletableFuture<Void> {
        StorageManager.saveSessionIdToStorage(sessionResponse.sessionId)
        return authSessionManager.setTokensAsync(
            AuthTokens(
                sessionId = sessionResponse.sessionId,
                accessToken = sessionResponse.accessToken,
                refreshToken = sessionResponse.refreshToken,
                idToken = sessionResponse.idToken,
            )
        )
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
        storageManager = createStorageManager(
            context = baseContext,
            sessionNamespace = if (!loginParams.idToken.isNullOrEmpty()) "sfa" else resolveSessionNamespace()
        )
        authSessionManager = createAuthSessionManager(baseContext)

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
        // Drop any prior PnP citadel tokens so initialize()/refreshSession() cannot
        // restore the previous user over this new SFA session-service session.
        authSessionManager.clearSessionDataAsync().whenComplete { _, _ ->
            completeSfaConnect(loginParams, subVerifierInfoArray)
        }
    }

    private fun completeSfaConnect(
        loginParams: LoginParams,
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

        val sessionId = StorageManager.generateRandomSessionKey()
        storageManager.setSessionId(sessionId)
        storageManager.createSession(gson.toJson(response))
            .whenComplete { result, err ->
                runOnUIThread {
                    if (err == null) {
                        web3AuthResponse = response
                        StorageManager.saveSessionIdToStorage(result)
                        storageManager.setSessionId(result)
                        val analyticsProps = mutableMapOf<String, Any>(
                            "connector" to "auth",
                            "auth_connection" to loginParams.authConnection.toString(),
                            "auth_connection_id" to loginParams.authConnectionId.toString(),
                            "group_auth_connection_id" to loginParams.groupedAuthConnectionId.toString(),
                            "chain_id" to web3AuthOption.defaultChainId.toString(),
                            "dapp_url" to loginParams.dappUrl.toString(),
                            "chain_id" to web3AuthOption.defaultChainId.toString(),
                            "chains" to (web3AuthOption.chains?.toString() ?: "[]"),
                            "integration_type" to "android",
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

        val endpoints = nodeDetails.torusNodeEndpoints
        val indexes = nodeDetails.torusIndexes ?: emptyArray()
        val nodePubkeys = nodeDetails.torusNodePub ?: emptyArray()
        val recordId = loginParams.recordId?.takeIf { it.isNotBlank() } ?: generateRecordId()
        val authConnection = loginParams.authConnection.name.lowercase(Locale.ROOT)

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
                RetrieveSharesParams(
                    endpoints,
                    indexes,
                    nodePubkeys,
                    loginParams.authConnectionId.toString(),
                    verifierParams,
                    aggregateIdToken,
                    null,
                    null,
                    null,
                    recordId,
                    authConnection
                )
            )
        } ?: run {
            val verifierParams = VerifierParams(userId.toString(), null, null, null)
            retrieveSharesResponse = torusUtils.retrieveShares(
                RetrieveSharesParams(
                    endpoints,
                    indexes,
                    nodePubkeys,
                    loginParams.authConnectionId.toString(),
                    verifierParams,
                    loginParams.idToken.toString(),
                    null,
                    null,
                    null,
                    recordId,
                    authConnection
                )
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
     * Invalidates the session-service row (SFA / ephemeral store) and clears
     * citadel tokens. Local state is always cleared; the returned future completes
     * successfully after local cleanup so SFA callers are not left hanging when
     * citadel has no tokens.
     *
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun logout(): CompletableFuture<Void> {
        AnalyticsManager.trackEvent(
            AnalyticsEvents.LOGOUT_STARTED
        )
        val logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()
        val storedSessionId = StorageManager.getSessionIdFromStorage()

        val invalidateStore: CompletableFuture<*> = if (storedSessionId.isNotBlank()) {
            storageManager.setSessionId(storedSessionId)
            storageManager.invalidateSession()
        } else {
            CompletableFuture.completedFuture(true)
        }

        authSessionManager.getAccessTokenAsync().whenComplete { accessToken, _ ->
            val clearCitadel: CompletableFuture<*> =
                if (!accessToken.isNullOrBlank()) {
                    authSessionManager.logoutAsync()
                } else {
                    authSessionManager.clearSessionDataAsync()
                }

            CompletableFuture.allOf(invalidateStore, clearCitadel).whenComplete { _, error ->
                StorageManager.deleteSessionIdFromStorage()
                SharedPrefsHelper.clear()
                web3AuthResponse = Web3AuthResponse()
                runOnUIThread {
                    if (error != null) {
                        AnalyticsManager.trackEvent(
                            AnalyticsEvents.LOGOUT_FAILED,
                            mutableMapOf<String, Any>(
                                "error_message" to "Logout Failed: ${error.message}"
                            )
                        )
                    } else {
                        AnalyticsManager.trackEvent(
                            AnalyticsEvents.LOGOUT_COMPLETED
                        )
                    }
                    // Always complete after local clear so SFA / partial failures
                    // do not leave callers waiting after state is already wiped.
                    logoutCompletableFuture.complete(null)
                    AnalyticsManager.reset()
                }
            }
        }
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
                "integration_type" to "android",
                "dapp_url" to this.loginParams?.dappUrl.toString(),
                "connector" to "auth",
                "duration" to System.currentTimeMillis() - startTime,
            )
        )
        enableMfaCompletableFuture = CompletableFuture()
        if (SharedPrefsHelper.getBoolean(IS_SFA) || !loginParams?.idToken.isNullOrEmpty()) {
            throwEnableMFAError(ErrorCode.ENABLE_MFA_NOT_ALLOWED)
            return enableMfaCompletableFuture
        }
        if (web3AuthResponse?.userInfo?.isMfaEnabled == true) {
            throwEnableMFAError(ErrorCode.MFA_ALREADY_ENABLED)
            return enableMfaCompletableFuture
        }
        hasActiveSession { hasSession ->
            if (!hasSession) {
                throwEnableMFAError(ErrorCode.NOUSERFOUND)
                return@hasActiveSession
            }
            processRequest("enable_mfa", loginParams)
        }
        return enableMfaCompletableFuture
    }


    fun manageMFA(loginParams: LoginParams? = null): CompletableFuture<Boolean> {
        actionType = "manage_mfa"
        AnalyticsManager.trackEvent(
            AnalyticsEvents.MFA_MANAGEMENT_STARTED,
            mutableMapOf<String, Any>(
                "integration_type" to "android",
                "dapp_url" to this.loginParams?.dappUrl.toString(),
                "connector" to "auth"
            )
        )
        AnalyticsManager.trackEvent(AnalyticsEvents.MFA_MANAGEMENT_SELECTED)
        manageMfaCompletableFuture = CompletableFuture()
        if (SharedPrefsHelper.getBoolean(IS_SFA) || !loginParams?.idToken.isNullOrEmpty()) {
            throwManageMFAError(ErrorCode.ENABLE_MFA_NOT_ALLOWED)
            return manageMfaCompletableFuture
        }
        if (web3AuthResponse?.userInfo?.isMfaEnabled == false) {
            throwManageMFAError(ErrorCode.MFA_NOT_ENABLED)
            return manageMfaCompletableFuture
        }
        hasActiveSession { hasSession ->
            if (!hasSession) {
                throwManageMFAError(ErrorCode.NOUSERFOUND)
                return@hasActiveSession
            }
            processRequest("manage_mfa", loginParams)
        }
        return manageMfaCompletableFuture
    }

    /**
     * True when citadel or session-service (SFA) has a session, or in-memory keys exist.
     */
    private fun hasActiveSession(onResult: (Boolean) -> Unit) {
        authSessionManager.getSessionIdAsync().whenComplete { citadelSessionId, _ ->
            if (!citadelSessionId.isNullOrBlank()) {
                onResult(true)
                return@whenComplete
            }
            val storageSessionId = StorageManager.getSessionIdFromStorage()
            val hasKeys = !web3AuthResponse?.privateKey.isNullOrBlank() ||
                !web3AuthResponse?.factorKey.isNullOrBlank()
            onResult(storageSessionId.isNotBlank() || hasKeys)
        }
    }

    /**
     * Authorize User session in order to avoid re-login.
     * Prefers citadel AuthSessionManager; falls back to session-service StorageManager (SFA).
     */
    private fun authorizeSession(
        origin: String,
        context: Context
    ): CompletableFuture<Web3AuthResponse> {
        val sessionCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
        authSessionManager.authorizeAsync().whenComplete { response, error ->
            if (error == null && !response.isNullOrBlank()) {
                completeAuthorizeFromPayload(response, sessionCompletableFuture)
                return@whenComplete
            }
            // Fallback: legacy/SFA session-service authorize
            val savedSessionId = StorageManager.getSessionIdFromStorage()
            if (savedSessionId.isBlank()) {
                sessionCompletableFuture.completeExceptionally(
                    Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
                )
                return@whenComplete
            }
            storageManager.setSessionId(savedSessionId)
            storageManager.authorizeSession().whenComplete { storageResponse, storageError ->
                if (storageError != null || storageResponse.isNullOrBlank()) {
                    sessionCompletableFuture.completeExceptionally(
                        Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
                    )
                } else {
                    completeAuthorizeFromPayload(storageResponse, sessionCompletableFuture)
                }
            }
        }
        return sessionCompletableFuture
    }

    private fun completeAuthorizeFromPayload(
        response: String,
        sessionCompletableFuture: CompletableFuture<Web3AuthResponse>,
    ) {
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

    /**
     * Re-authorizes the current citadel session. Clears tokens on failure.
     */
    fun refreshSession(): CompletableFuture<Web3AuthResponse> {
        val future = CompletableFuture<Web3AuthResponse>()
        authorizeSession(web3AuthOption.redirectUrl, baseContext).whenComplete { resp, error ->
            if (error != null || resp == null) {
                authSessionManager.logoutAsync().whenComplete { _, _ ->
                    web3AuthResponse = Web3AuthResponse()
                    future.completeExceptionally(
                        error ?: Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
                    )
                }
            } else {
                web3AuthResponse = resp
                future.complete(resp)
            }
        }
        return future
    }

    fun getAccessToken(): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        authSessionManager.getAccessTokenAsync().whenComplete { token, error ->
            if (error != null || token.isNullOrBlank()) {
                future.completeExceptionally(
                    error ?: Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
                )
            } else {
                future.complete(token)
            }
        }
        return future
    }

    fun getIdentityToken(): CompletableFuture<String> {
        AnalyticsManager.trackEvent(AnalyticsEvents.IDENTITY_TOKEN_STARTED)
        val future = CompletableFuture<String>()
        authSessionManager.getIdTokenAsync().whenComplete { token, error ->
            if (error != null || token.isNullOrBlank()) {
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.IDENTITY_TOKEN_FAILED,
                    mapOf("error_message" to (error?.message ?: "missing idToken"))
                )
                future.completeExceptionally(
                    error ?: Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
                )
            } else {
                AnalyticsManager.trackEvent(AnalyticsEvents.IDENTITY_TOKEN_COMPLETED)
                future.complete(token)
            }
        }
        return future
    }

    private fun fetchProjectConfig(): CompletableFuture<Boolean> {
        val projectConfigCompletableFuture: CompletableFuture<Boolean> = CompletableFuture()
        val web3AuthApi =
            ApiHelper.getInstance(web3AuthOption.authBuildEnv)
                .create(ApiService::class.java)
        if (!ApiHelper.isNetworkAvailable(baseContext)) {
            throw Exception(
                Web3AuthError.getError(ErrorCode.RUNTIME_ERROR)
            )
        }
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val aaProvider = resolveAaProvider()
                val result = web3AuthApi.fetchProjectConfig(
                    project_id = web3AuthOption.clientId,
                    network = web3AuthOption.web3AuthNetwork.name.lowercase(),
                    build_env = web3AuthOption.authBuildEnv.name.lowercase(),
                    aa_provider = aaProvider,
                )
                if (result.isSuccessful && result.body() != null) {
                    projectConfigResponse = result.body()
                    // Set global properties for analytics after fetching project config
                    AnalyticsManager.setGlobalProperties(
                        mapOf(
                            "sdk_name" to AnalyticsSdkType.ANDROID,
                            "sdk_version" to AnalyticsEvents.SDK_VERSION,
                            "web3auth_client_id" to web3AuthOption.clientId,
                            "web3auth_network" to web3AuthOption.web3AuthNetwork,
                            "team_id" to projectConfigResponse?.teamId.toString(),
                            "integration_type" to AnalyticsIntegrationType.NATIVE_SDK,
                        )
                    )
                    val response = result.body()
                    applySessionTimeFromProjectConfig(response)
                    applySmartAccountFlagsFromProjectConfig(response)
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
                    response?.chains?.let { projectChains ->
                        if (web3AuthOption.chains == null) {
                            web3AuthOption.chains = projectChains.firstOrNull()
                        }
                    }
                    mergeWalletServicesFromProjectConfig(response)
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
     * Resolves AA provider for project-config query from options only
     * (project config is not available yet on first fetch).
     */
    private fun resolveAaProvider(): String? {
        val raw = web3AuthOption.accountAbstractionConfig ?: return null
        return runCatching {
            val json = JSONObject(raw)
            sequenceOf("smartAccountType", "smart_account_type", "aaProvider", "aa_provider")
                .mapNotNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                .firstOrNull()
                ?.lowercase(Locale.ROOT)
        }.getOrNull()
    }

    /**
     * Auth v11: when options.sessionTime is unset, take project config (else 30-day default).
     * Recreates [storageManager] so subsequent loginId /store calls use the resolved timeout.
     */
    private fun applySessionTimeFromProjectConfig(response: ProjectConfigResponse?) {
        if (web3AuthOption.sessionTime == null) {
            val fromProject = response?.sessionTime?.takeIf { it > 0 }
            web3AuthOption.sessionTime = fromProject ?: DEFAULT_SESSION_TIME
        }
        storageManager = createStorageManager(
            context = baseContext,
            sessionNamespace = resolveSessionNamespace(),
        )
    }

    private fun applySmartAccountFlagsFromProjectConfig(response: ProjectConfigResponse?) {
        val smartAccounts = response?.smartAccounts ?: return
        if (web3AuthOption.useAAWithExternalWallet == null) {
            web3AuthOption.useAAWithExternalWallet =
                smartAccounts.walletScope == SmartAccountWalletScope.ALL
        }
        if (web3AuthOption.accountAbstractionConfig.isNullOrBlank()) {
            web3AuthOption.accountAbstractionConfig = gson.toJson(smartAccounts)
        }
    }

    private fun trackConsentIfNeeded(event: String) {
        if (web3AuthOption.whiteLabel?.consentRequired == true) {
            AnalyticsManager.trackEvent(event)
        }
    }

    private fun buildInitializationAnalyticsProperties(): MutableMap<String, Any?> {
        val projectChains = projectConfigResponse?.chains
        val optionChain = web3AuthOption.chains
        val chainIds = projectChains?.map { it.chainId }
            ?: listOfNotNull(optionChain?.chainId)
        val defaultChainId = web3AuthOption.defaultChainId
            ?: projectChains?.firstOrNull()?.chainId
            ?: optionChain?.chainId
            ?: "0x1"
        val defaultChainName = projectChains?.firstOrNull { it.chainId == defaultChainId }?.displayName
            ?: optionChain?.displayName
        val wl = web3AuthOption.whiteLabel
        val ws = web3AuthOption.walletServicesConfig
        val wsTheme = ws?.whiteLabel?.theme
        val sa = projectConfigResponse?.smartAccounts

        return mutableMapOf(
            "chain_ids" to chainIds,
            "chain_names" to (projectChains?.mapNotNull { it.displayName } ?: listOfNotNull(optionChain?.displayName)),
            "chain_rpc_targets" to (projectChains?.map { it.rpcTarget }
                ?: listOfNotNull(optionChain?.rpcTarget)),
            "default_chain_id" to defaultChainId,
            "default_chain_name" to defaultChainName,
            "chain_nameSpaces" to listOf("eip155", "solana", "other"),
            "session_time" to (web3AuthOption.sessionTime ?: DEFAULT_SESSION_TIME),
            "sfa_key_enabled" to (web3AuthOption.useSFAKey == true),
            "custom_storage" to false,
            "logging_enabled" to web3AuthOption.enableLogging,
            "auth_build_env" to web3AuthOption.authBuildEnv,
            "auth_mfa_settings" to web3AuthOption.mfaSettings,
            "whitelabel_logo_light_enabled" to (wl?.logoLight != null),
            "whitelabel_logo_dark_enabled" to (wl?.logoDark != null),
            "whitelabel_theme_mode" to wl?.theme,
            "whitelabel_app_name" to wl?.appName,
            "whitelabel_tnc_link_enabled" to !wl?.tncLink.isNullOrBlank(),
            "whitelabel_privacy_policy_enabled" to !wl?.privacyPolicy.isNullOrBlank(),
            "whitelabel_consent_required" to (wl?.consentRequired == true),
            "aa_smart_account_type" to sa?.smartAccountType?.name?.lowercase(Locale.ROOT),
            "aa_chain_ids" to sa?.chains?.map { it.chainId },
            "aa_bundler_urls" to sa?.chains?.map { it.bundlerConfig.url },
            "aa_paymaster_urls" to sa?.chains?.mapNotNull { it.paymasterConfig?.url },
            "aa_paymaster_enabled" to (sa?.chains?.any { it.paymasterConfig != null } == true),
            "aa_eip_standard" to sa?.eipStandard,
            "aa_wallet_scope" to sa?.walletScope?.name?.lowercase(Locale.ROOT),
            "aa_use_with_external_wallet" to web3AuthOption.useAAWithExternalWallet,
            "ws_confirmation_strategy" to ws?.confirmationStrategy?.name?.lowercase(Locale.ROOT),
            "ws_enable_key_export" to ws?.enableKeyExport,
            "ws_show_widget_button" to wsTheme?.get("showWidgetButton"),
            "ws_hide_defi_positions_display" to wsTheme?.get("hideDefiPositionsDisplay"),
            "ws_default_portfolio" to wsTheme?.get("defaultPortfolio"),
            "duration" to System.currentTimeMillis() - startTime,
            "integration_type" to AnalyticsIntegrationType.NATIVE_SDK,
            "dapp_url" to this.loginParams?.dappUrl,
        )
    }

    /**
     * Maps dashboard `walletUi` toggles into [WalletServicesConfig] (ws-embed v5 props).
     * Developer overrides on [Web3AuthOptions.walletServicesConfig] take precedence.
     */
    private fun mergeWalletServicesFromProjectConfig(response: ProjectConfigResponse?) {
        val walletUi = response?.walletUiConfig ?: return
        val existing = web3AuthOption.walletServicesConfig
        val whiteLabelMap = HashMap<String, String?>()
        existing?.whiteLabel?.theme?.let { whiteLabelMap.putAll(it) }

        fun putBool(key: String, invertedEnable: Boolean?) {
            if (invertedEnable != null) {
                whiteLabelMap[key] = (!invertedEnable).toString()
            }
        }

        // Prefer existing whiteLabel branding fields; inject hide* flags into theme map for wallet payload.
        putBool("hideTokenDisplay", walletUi.enableTokenDisplay)
        putBool("hideNftDisplay", walletUi.enableNftDisplay)
        putBool("hideTransfers", walletUi.enableSendButton)
        putBool("hideTopup", walletUi.enableBuyButton)
        putBool("hideReceive", walletUi.enableReceiveButton)
        putBool("hideSwap", walletUi.enableSwapButton)
        putBool("hideShowAllTokens", walletUi.enableShowAllTokensButton)
        putBool("hideWalletConnect", walletUi.enableWalletConnect)
        putBool("hideDefiPositionsDisplay", walletUi.enableDefiPositionsDisplay)
        if (walletUi.enablePortfolioWidget != null) {
            whiteLabelMap["showWidgetButton"] = walletUi.enablePortfolioWidget.toString()
        }
        walletUi.portfolioWidgetPosition?.let {
            whiteLabelMap["buttonPosition"] = it.name.lowercase(Locale.ROOT).replace('_', '-')
        }
        walletUi.defaultPortfolio?.let {
            whiteLabelMap["defaultPortfolio"] = it.name.lowercase(Locale.ROOT)
        }

        val confirmation = when (walletUi.enableConfirmationModal) {
            true -> ConfirmationStrategy.MODAL
            false -> ConfirmationStrategy.AUTO_APPROVE
            null -> existing?.confirmationStrategy ?: ConfirmationStrategy.DEFAULT
        }

        val mergedWhiteLabel = (existing?.whiteLabel ?: web3AuthOption.whiteLabel)?.copy(
            theme = whiteLabelMap.ifEmpty { existing?.whiteLabel?.theme }
        ) ?: com.web3auth.core.types.WhiteLabelData(theme = whiteLabelMap.ifEmpty { null })

        web3AuthOption.walletServicesConfig = WalletServicesConfig(
            confirmationStrategy = existing?.confirmationStrategy ?: confirmation,
            whiteLabel = existing?.whiteLabel?.merge(mergedWhiteLabel) ?: mergedWhiteLabel,
            enableKeyExport = existing?.enableKeyExport ?: response.enableKeyExport,
        )
    }


    /**
     * Retrieves the login ID from the provided JSONObject asynchronously.
     *
     * @param jsonObject The JSONObject from which to retrieve the login ID.
     * @return A CompletableFuture<String> representing the asynchronous operation, containing the login ID.
     */
    private fun getLoginId(sessionId: String, jsonObject: String): CompletableFuture<String> {
        storageManager.setSessionId(sessionId)
        return storageManager.createSession(jsonObject)
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
                "integration_type" to "android",
                "dapp_url" to this.loginParams?.dappUrl.toString(),
            )
        )
        val launchWalletServiceCF: CompletableFuture<Void> = CompletableFuture()
        val activeResponse = web3AuthResponse
        if (activeResponse == null ||
            (activeResponse.privateKey.isNullOrBlank() && activeResponse.factorKey.isNullOrBlank())
        ) {
            AnalyticsManager.trackEvent(
                AnalyticsEvents.WALLET_SERVICES_FAILED,
                mutableMapOf<String, Any>(
                    "integration_type" to "android",
                    "dapp_url" to this.loginParams?.dappUrl.toString(),
                    "duration" to System.currentTimeMillis() - startTime,
                    "error" to "Wallet Services Error: Session ID is not found. Please login first."
                )
            )
            launchWalletServiceCF.completeExceptionally(Exception("Please login first to launch wallet"))
            return launchWalletServiceCF
        }

        // Wallet v6 rehydrates via citadel sessionId + accessToken (Auth v11 parity).
        resolveWalletLaunchCreds().whenComplete { creds, credsError ->
            if (credsError != null || creds == null) {
                AnalyticsManager.trackEvent(
                    AnalyticsEvents.WALLET_SERVICES_FAILED,
                    mutableMapOf<String, Any>(
                        "integration_type" to "android",
                        "dapp_url" to this.loginParams?.dappUrl.toString(),
                        "duration" to System.currentTimeMillis() - startTime,
                        "error" to (credsError?.message ?: "Wallet launch credentials missing")
                    )
                )
                launchWalletServiceCF.completeExceptionally(
                    credsError ?: Exception("Wallet launch credentials missing")
                )
                return@whenComplete
            }
            openWalletUi(path, launchWalletServiceCF, creds)
        }
        return launchWalletServiceCF
    }

    private fun openWalletUi(
        path: String?,
        launchWalletServiceCF: CompletableFuture<Void>,
        creds: WalletLaunchCreds,
    ) {
        if (creds.sessionId.isBlank()) {
            launchWalletServiceCF.completeExceptionally(Exception("Please login first to launch wallet"))
            return
        }
        val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)

        // If chains are not present in project config, throw an error
        if (projectConfigResponse?.chains == null) {
            launchWalletServiceCF.completeExceptionally(
                Exception(Web3AuthError.getError(ErrorCode.PROJECT_CONFIG_NOT_FOUND_ERROR))
            )
            return
        }
        val initOptions = JSONObject(gson.toJson(web3AuthOption)).apply {
            put("network", web3AuthOption.web3AuthNetwork.toString().lowercase(Locale.ROOT))
            projectConfigResponse?.chains?.let {
                put("chains", JSONArray(gson.toJson(it)))
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
            web3AuthOption.walletServicesConfig?.let {
                put("walletServicesConfig", JSONObject(gson.toJson(it)))
            }
            projectConfigResponse?.walletConnectProjectId?.takeIf { it.isNotBlank() }?.let {
                put("walletConnectProjectId", it)
            }
        }

        val paramMap = JSONObject()
        paramMap.put(
            "options", initOptions
        )
        val sessionId = StorageManager.generateRandomSessionKey()
        val loginIdCf = getLoginId(sessionId, paramMap.toString())

        loginIdCf.whenComplete { loginId, error ->
            if (error == null && !loginId.isNullOrBlank()) {
                val walletMap = JsonObject()
                // Wallet F0 does not strip 0x — must pass unprefixed hex session keys.
                walletMap.addProperty("loginId", loginId.strip0xForWalletSession())
                walletMap.addWalletAuthCreds(creds)
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
                val intent = Intent(baseContext, WebViewActivity::class.java)
                intent.putExtra(WEBVIEW_URL, url.toString())
                baseContext.startActivity(intent)
                launchWalletServiceCF.complete(null)
            } else {
                launchWalletServiceCF.completeExceptionally(
                    error ?: Exception("Failed to create wallet loginId")
                )
            }
        }
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

        val activeResponse = web3AuthResponse
        if (activeResponse == null ||
            (activeResponse.privateKey.isNullOrBlank() && activeResponse.factorKey.isNullOrBlank())
        ) {
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
            return signMsgCF
        }

        resolveWalletLaunchCreds().whenComplete { creds, credsError ->
            if (credsError != null || creds == null) {
                runOnUIThread {
                    AnalyticsManager.trackEvent(
                        AnalyticsEvents.REQUEST_FUNCTION_FAILED,
                        mutableMapOf<String, Any>(
                            "duration" to System.currentTimeMillis() - startTime,
                            "error" to (credsError?.message ?: "Wallet launch credentials missing")
                        )
                    )
                    signMsgCF.completeExceptionally(
                        credsError ?: Exception("Wallet launch credentials missing")
                    )
                }
                return@whenComplete
            }
            openWalletRequest(method, requestParams, path, appState, creds)
        }
        return signMsgCF
    }

    private fun openWalletRequest(
        method: String,
        requestParams: JsonArray,
        path: String?,
        appState: String?,
        creds: WalletLaunchCreds,
    ) {
        val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)

        // If chains are not present in project config, throw an error
        if (projectConfigResponse?.chains == null) {
            signMsgCF.completeExceptionally(
                Exception(Web3AuthError.getError(ErrorCode.PROJECT_CONFIG_NOT_FOUND_ERROR))
            )
            return
        }

        val initOptions = JSONObject(gson.toJson(web3AuthOption))
        initOptions.apply {
            put("network", web3AuthOption.web3AuthNetwork.toString().lowercase(Locale.ROOT))
            projectConfigResponse?.chains?.let {
                put("chains", JSONArray(gson.toJson(it)))
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
            web3AuthOption.walletServicesConfig?.let {
                put("walletServicesConfig", JSONObject(gson.toJson(it)))
            }
            projectConfigResponse?.walletConnectProjectId?.takeIf { it.isNotBlank() }?.let {
                put("walletConnectProjectId", it)
            }
        }

        val paramMap = JSONObject()
        paramMap.put(
            "options", initOptions
        )

        val loginId = StorageManager.generateRandomSessionKey()
        val loginIdCf = getLoginId(loginId, paramMap.toString())

        loginIdCf.whenComplete { loginIdResult, error ->
            if (error == null && !loginIdResult.isNullOrBlank()) {
                // Match iOS: `request` is a JSON string whose `params` is a real array.
                val requestObj = JsonObject().apply {
                    addProperty("method", method)
                    add("params", requestParams)
                }
                val signMessageObj = JsonObject().apply {
                    addProperty("loginId", loginIdResult.strip0xForWalletSession())
                    addWalletAuthCreds(creds)
                    addProperty("platform", "android")
                    addProperty("request", gson.toJson(requestObj))
                    if (!appState.isNullOrBlank()) {
                        addProperty("appState", appState)
                    }
                    if (SharedPrefsHelper.getBoolean(IS_SFA)) {
                        addProperty("sessionNamespace", "sfa")
                    }
                }

                val signMessageHash =
                    "b64Params=" + gson.toJson(signMessageObj).toByteArray(Charsets.UTF_8)
                        .toBase64URLString()

                val url =
                    Uri.Builder().scheme(sdkUrl.scheme)
                        .encodedAuthority(sdkUrl.encodedAuthority)
                        .encodedPath(sdkUrl.encodedPath).appendEncodedPath(path)
                        .fragment(signMessageHash).build()
                val intent = Intent(baseContext, WebViewActivity::class.java)
                intent.putExtra(WEBVIEW_URL, url.toString())
                intent.putExtra(REDIRECT_URL, web3AuthOption.redirectUrl)
                baseContext.startActivity(intent)
            } else {
                signMsgCF.completeExceptionally(
                    error ?: Exception("Failed to create wallet loginId")
                )
            }
        }
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
     * Auth v11 parity: returns user info and backfills [UserInfo.idToken] from
     * citadel token storage when the in-memory profile lacks it.
     */
    fun getUserInfoAsync(): CompletableFuture<UserInfo> {
        val future = CompletableFuture<UserInfo>()
        val existing = web3AuthResponse?.userInfo
        if (existing == null) {
            future.completeExceptionally(Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND)))
            return future
        }
        authSessionManager.getIdTokenAsync().whenComplete { idToken, _ ->
            if (!idToken.isNullOrBlank() && existing.idToken.isBlank()) {
                existing.idToken = idToken
            }
            future.complete(existing)
        }
        return future
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
            "chains" to (web3AuthOption.chains?.toString() ?: "[]"),
            "duration" to System.currentTimeMillis() - startTime,
            "error_code" to (error?.name ?: "UNKNOWN"),
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
            "integration_type" to "android",
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

