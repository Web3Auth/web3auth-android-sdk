package com.web3auth.core

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.api.ApiService
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.types.ChainConfig
import com.web3auth.core.types.ErrorCode
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.InitOptions
import com.web3auth.core.types.InitParams
import com.web3auth.core.types.LoginConfigItem
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.REDIRECT_URL
import com.web3auth.core.types.RequestData
import com.web3auth.core.types.SessionResponse
import com.web3auth.core.types.SignMessage
import com.web3auth.core.types.SignResponse
import com.web3auth.core.types.UnKnownException
import com.web3auth.core.types.UserCancelledException
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.WEBVIEW_URL
import com.web3auth.core.types.Web3AuthError
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WebViewResultCallback
import com.web3auth.session_manager_android.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.CompletableFuture

class Web3Auth(web3AuthOptions: Web3AuthOptions, context: Context) : WebViewResultCallback,
    ContextWrapper(context) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private lateinit var loginCompletableFuture: CompletableFuture<Web3AuthResponse>
    private lateinit var enableMfaCompletableFuture: CompletableFuture<Boolean>
    private lateinit var signMsgCF: CompletableFuture<SignResponse>

    private var web3AuthResponse: Web3AuthResponse? = null
    private var web3AuthOption = web3AuthOptions
    private var context = context
    private var sessionManager: SessionManager = SessionManager(
        context,
        web3AuthOptions.sessionTime ?: 600,
        web3AuthOptions.redirectUrl.toString()
    )

    /**
     * Initializes the KeyStoreManager.
     */
    private fun initiateKeyStoreManager() {
        KeyStoreManagerUtils.getKeyGenerator()
    }

    private fun getInitOptions(): InitOptions {
        return InitOptions(
            clientId = web3AuthOption.clientId,
            network = web3AuthOption.network.name.lowercase(Locale.ROOT),
            redirectUrl = web3AuthOption.redirectUrl.toString(),
            whiteLabel = web3AuthOption.whiteLabel?.let { gson.toJson(it) },
            loginConfig = web3AuthOption.loginConfig?.let { gson.toJson(it) },
            buildEnv = web3AuthOption.buildEnv?.name?.lowercase(Locale.ROOT),
            mfaSettings = web3AuthOption.mfaSettings?.let { gson.toJson(it) },
            sessionTime = web3AuthOption.sessionTime,
            originData = web3AuthOption.originData?.let { gson.toJson(it) }
        )
    }

    /**
     * Retrieves the initialization parameters as a JSONObject.
     *
     * @param params The optional login parameters required for initialization. Default is null.
     * @return The initialization parameters as a JSONObject.
     */
    private fun getInitParams(params: LoginParams?): InitParams {
        return InitParams(
            loginProvider = params?.loginProvider?.name?.lowercase(Locale.ROOT),
            extraLoginOptions = params?.extraLoginOptions?.let { gson.toJson(it) },
            redirectUrl = params?.redirectUrl?.toString() ?: web3AuthOption.redirectUrl.toString(),
            mfaLevel = params?.mfaLevel?.name?.lowercase(Locale.ROOT),
            curve = params?.curve?.name?.lowercase(Locale.ROOT),
            dappShare = params?.dappShare,
            appState = params?.appState,
            dappUrl = params?.dappUrl
        )
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
        CustomChromeTabsActivity.webViewResultCallback = this
        val sdkUrl = Uri.parse(web3AuthOption.sdkUrl)
        val initOptions = JSONObject(gson.toJson(getInitOptions()))
        val initParams = JSONObject(gson.toJson(getInitParams(params)))

        val paramMap = JSONObject()
        paramMap.put(
            "options", initOptions
        )
        paramMap.put("actionType", actionType)

        if (actionType == "enable_mfa") {
            val userInfo = web3AuthResponse?.userInfo
            initParams.put("loginProvider", userInfo?.typeOfLogin)
            var extraOptionsString = ""
            var existingExtraLoginOptions = ExtraLoginOptions()
            if (initParams.has("extraLoginOptions")) {
                extraOptionsString = initParams.getString("extraLoginOptions")
                existingExtraLoginOptions =
                    gson.fromJson(extraOptionsString, ExtraLoginOptions::class.java)
            }
            existingExtraLoginOptions.login_hint = userInfo?.verifierId
            initParams.put("extraLoginOptions", gson.toJson(existingExtraLoginOptions))
            initParams.put("mfaLevel", MFALevel.MANDATORY.name.lowercase(Locale.ROOT))
            paramMap.put("sessionId", sessionManager.getSessionId())
        }
        paramMap.put("params", initParams)

        val loginIdCf = getLoginId(paramMap)
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
                intent.putExtra(REDIRECT_URL, web3AuthOption.redirectUrl.toString())
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
                //authorize session
                sessionManager.setSessionId(SessionManager.getSessionIdFromStorage())
                this.authorizeSession(web3AuthOption.redirectUrl.toString(), baseContext)
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
            loginCompletableFuture.completeExceptionally(UnKnownException(error))
            if (::enableMfaCompletableFuture.isInitialized) enableMfaCompletableFuture.completeExceptionally(
                UnKnownException(error)
            )
            return
        }

        val b64Params = hashUri.getQueryParameter("b64Params")
        if (b64Params.isNullOrBlank()) {
            throwLoginError(ErrorCode.INVALID_LOGIN)
            throwEnableMFAError(ErrorCode.INVALID_LOGIN)
            return
        }
        val b64ParamString = decodeBase64URLString(b64Params).toString(Charsets.UTF_8)
        val sessionResponse = gson.fromJson(b64ParamString, SessionResponse::class.java)
        val sessionId = sessionResponse.sessionId

        if (sessionId.isNotBlank() && sessionId.isNotEmpty()) {
            SessionManager.saveSessionIdToStorage(sessionId)
            sessionManager.setSessionId(sessionId)

            //Rehydrate Session
            this.authorizeSession(web3AuthOption.redirectUrl.toString(), baseContext)
                .whenComplete { resp, error ->
                    runOnUIThread {
                        if (error == null) {
                            web3AuthResponse = resp
                            if (web3AuthResponse?.error?.isNotBlank() == true) {
                                throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                            } else if (web3AuthResponse?.privKey.isNullOrBlank() && web3AuthResponse?.factorKey.isNullOrBlank()) {
                                throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                            } else {
                                web3AuthResponse?.sessionId?.let {
                                    SessionManager.saveSessionIdToStorage(it)
                                    sessionManager.setSessionId(it)
                                }

                                if (web3AuthResponse?.userInfo?.dappShare?.isNotEmpty() == true) {
                                    KeyStoreManagerUtils.encryptData(
                                        web3AuthResponse?.userInfo?.verifier.plus(" | ")
                                            .plus(web3AuthResponse?.userInfo?.verifierId),
                                        web3AuthResponse?.userInfo?.dappShare!!,
                                    )
                                }
                                loginCompletableFuture.complete(web3AuthResponse)
                                if (::enableMfaCompletableFuture.isInitialized)
                                    enableMfaCompletableFuture.complete(true)
                            }
                        } else {
                            print(error)
                        }
                    }
                }
        } else {
            throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
            throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
        }
    }

    /**
     * Performs a login operation asynchronously.
     *
     * @param loginParams The login parameters required for authentication.
     * @return A CompletableFuture<Web3AuthResponse> representing the asynchronous operation, containing the Web3AuthResponse upon successful login.
     */
    fun login(loginParams: LoginParams): CompletableFuture<Web3AuthResponse> {
        //check for share
        if (web3AuthOption.loginConfig != null) {
            val loginConfigItem: LoginConfigItem? = web3AuthOption.loginConfig?.values?.first()
            val share: String? =
                KeyStoreManagerUtils.decryptData(loginConfigItem?.verifier.toString())
            if (share?.isNotEmpty() == true) {
                loginParams.dappShare = share
            }
        }

        //login
        processRequest("login", loginParams)

        loginCompletableFuture = CompletableFuture<Web3AuthResponse>()
        return loginCompletableFuture
    }

    /**
     * Logs out the user asynchronously.
     *
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun logout(): CompletableFuture<Void> {
        val logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()
        val sessionResponse: CompletableFuture<Boolean>? =
            sessionManager.invalidateSession(baseContext)
        sessionResponse?.whenComplete { _, error ->
            SessionManager.deleteSessionIdFromStorage()
            runOnUIThread {
                if (error == null) {
                    logoutCompletableFuture.complete(null)
                } else {
                    logoutCompletableFuture.completeExceptionally(Exception(error))
                }
            }
        }
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
                } else if (web3AuthResponse?.privKey.isNullOrBlank() && web3AuthResponse?.factorKey.isNullOrBlank()) {
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
            ApiHelper.getInstance(web3AuthOption.network.name).create(ApiService::class.java)
        if (!ApiHelper.isNetworkAvailable(baseContext)) {
            throw Exception(
                Web3AuthError.getError(ErrorCode.RUNTIME_ERROR)
            )
        }
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val result = web3AuthApi.fetchProjectConfig(
                    web3AuthOption.clientId,
                    web3AuthOption.network.name.lowercase()
                )
                if (result.isSuccessful && result.body() != null) {
                    val response = result.body()
                    web3AuthOption.originData =
                        web3AuthOption.originData.mergeMaps(response?.whitelist?.signed_urls)
                    if (response?.whitelabel != null) {
                        if (web3AuthOption.whiteLabel == null) {
                            web3AuthOption.whiteLabel = response.whitelabel
                        } else {
                            web3AuthOption.whiteLabel =
                                web3AuthOption.whiteLabel!!.merge(response.whitelabel)
                        }
                    }
                    projectConfigCompletableFuture.complete(true)
                } else {
                    projectConfigCompletableFuture.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.RUNTIME_ERROR
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
    private fun getLoginId(jsonObject: JSONObject): CompletableFuture<String> {
        val sessionId = SessionManager.generateRandomSessionKey()
        sessionManager.setSessionId(sessionId)
        return sessionManager.createSession(
            jsonObject.toString(),
            baseContext,
        )
    }

    /**
     * Launches the wallet services asynchronously.
     *
     * @param chainConfig The configuration details of the blockchain network.
     * @param path The path where the wallet services will be launched. Default value is "wallet".
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun launchWalletServices(
        chainConfig: ChainConfig,
        path: String? = "wallet"
    ): CompletableFuture<Void> {
        val launchWalletServiceCF: CompletableFuture<Void> = CompletableFuture()
        val sessionId = SessionManager.getSessionIdFromStorage()
        if (sessionId.isNotBlank()) {
            val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)

            val initOptions = JSONObject(gson.toJson(getInitOptions()))
            initOptions.put(
                "chainConfig", gson.toJson(chainConfig)
            )

            val paramMap = JSONObject()
            paramMap.put(
                "options", initOptions
            )

            val loginIdCf = getLoginId(paramMap)

            loginIdCf.whenComplete { loginId, error ->
                if (error == null) {
                    val walletMap = JsonObject()
                    walletMap.addProperty(
                        "loginId", loginId
                    )
                    walletMap.addProperty("sessionId", sessionId)
                    walletMap.addProperty("platform", "android")

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
            launchWalletServiceCF.completeExceptionally(Exception("Please login first to launch wallet"))
        }
        return launchWalletServiceCF
    }

    /**
     * Signs a message asynchronously.
     *
     * @param chainConfig The configuration details of the blockchain network.
     * @param method The method name of the request.
     * @param requestParams The parameters of the request in JSON array format.
     * @param path The path where the signing service is located. Default value is "wallet/request".
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun request(
        chainConfig: ChainConfig,
        method: String,
        requestParams: JsonArray,
        path: String? = "wallet/request",
        appState: String? = null
    ): CompletableFuture<SignResponse> {
        signMsgCF = CompletableFuture()
        WebViewActivity.webViewResultCallback = this

        val sessionId = SessionManager.getSessionIdFromStorage()
        if (sessionId.isNotBlank()) {
            val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)
            val initOptions = JSONObject(gson.toJson(getInitOptions()))
            initOptions.put(
                "chainConfig", gson.toJson(chainConfig)
            )
            val paramMap = JSONObject()
            paramMap.put(
                "options", initOptions
            )

            val loginIdCf = getLoginId(paramMap)

            loginIdCf.whenComplete { loginId, error ->
                if (error == null) {
                    val signMessageMap = SignMessage(
                        loginId = loginId,
                        sessionId = sessionId,
                        request = RequestData(
                            method = method,
                            params = gson.toJson(requestParams)
                        ),
                        appState = appState.let { it }
                    )

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
                    intent.putExtra(REDIRECT_URL, web3AuthOption.redirectUrl.toString())
                    baseContext.startActivity(intent)
                }
            }
        } else {
            runOnUIThread {
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
    fun getPrivkey(): String {
        val privKey: String? = if (web3AuthResponse == null) {
            ""
        } else {
            if (web3AuthOption.useCoreKitKey == true) {
                web3AuthResponse?.coreKitKey
            } else {
                web3AuthResponse?.privKey
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
    fun getEd25519PrivKey(): String {
        val ed25519Key: String? = if (web3AuthResponse == null) {
            null
        } else {
            if (web3AuthOption.useCoreKitKey == true) {
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

    companion object {
        private var isCustomTabsClosed: Boolean = false

        fun setCustomTabsClosed(_isCustomTabsClosed: Boolean) {
            isCustomTabsClosed = _isCustomTabsClosed
        }

        fun getCustomTabsClosed(): Boolean {
            return isCustomTabsClosed
        }
    }

    override fun onSignResponseReceived(signResponse: SignResponse?) {
        if (signResponse != null) {
            signMsgCF.complete(signResponse)
        }
    }

    override fun onSessionResponseReceived(sessionResponse: SessionResponse?) {
        val sessionId = sessionResponse?.sessionId
        if (sessionId?.isNotBlank() == true && sessionId.isNotEmpty()) {
            SessionManager.saveSessionIdToStorage(sessionId)
            sessionManager.setSessionId(sessionId)

            //Rehydrate Session
            this.authorizeSession(web3AuthOption.redirectUrl.toString(), this.context)
                .whenComplete { resp, error ->
                    runOnUIThread {
                        if (error == null) {
                            web3AuthResponse = resp
                            if (web3AuthResponse?.error?.isNotBlank() == true) {
                                throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                            } else if (web3AuthResponse?.privKey.isNullOrBlank() && web3AuthResponse?.factorKey.isNullOrBlank()) {
                                throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
                                throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                            } else {
                                web3AuthResponse?.sessionId?.let {
                                    SessionManager.saveSessionIdToStorage(it)
                                    sessionManager.setSessionId(it)
                                }

                                if (web3AuthResponse?.userInfo?.dappShare?.isNotEmpty() == true) {
                                    KeyStoreManagerUtils.encryptData(
                                        web3AuthResponse?.userInfo?.verifier.plus(" | ")
                                            .plus(web3AuthResponse?.userInfo?.verifierId),
                                        web3AuthResponse?.userInfo?.dappShare!!,
                                    )
                                }
                                loginCompletableFuture.complete(web3AuthResponse)
                                if (::enableMfaCompletableFuture.isInitialized)
                                    enableMfaCompletableFuture.complete(true)
                            }
                        } else {
                            print(error)
                        }
                    }
                }
        } else {
            throwLoginError(ErrorCode.SOMETHING_WENT_WRONG)
            throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
        }
    }

    override fun onWebViewCancelled() {
        signMsgCF.completeExceptionally(Exception("User cancelled the operation."))
    }
}

