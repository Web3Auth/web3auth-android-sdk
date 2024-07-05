package com.web3auth.core

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.api.ApiService
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.types.AuthOptions
import com.web3auth.core.types.AuthParamsData
import com.web3auth.core.types.AuthenticationOptionsRequest
import com.web3auth.core.types.AuthenticationOptionsResponse
import com.web3auth.core.types.AuthenticatorAttachment
import com.web3auth.core.types.ChainConfig
import com.web3auth.core.types.ChallengeData
import com.web3auth.core.types.ErrorCode
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.ExtraVerifierParams
import com.web3auth.core.types.Flags
import com.web3auth.core.types.LoginConfigItem
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.MetadataInfo
import com.web3auth.core.types.Network
import com.web3auth.core.types.Options
import com.web3auth.core.types.PassKeyLoginParams
import com.web3auth.core.types.REDIRECT_URL
import com.web3auth.core.types.RegistrationOptionsRequest
import com.web3auth.core.types.RegistrationResponse
import com.web3auth.core.types.RegistrationResponseJson
import com.web3auth.core.types.Rp
import com.web3auth.core.types.SessionResponse
import com.web3auth.core.types.SignResponse
import com.web3auth.core.types.UnKnownException
import com.web3auth.core.types.UserCancelledException
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.VerifyAuthenticationRequest
import com.web3auth.core.types.VerifyAuthenticationResponse
import com.web3auth.core.types.VerifyRegistrationResponse
import com.web3auth.core.types.VerifyRequest
import com.web3auth.core.types.WEBVIEW_URL
import com.web3auth.core.types.Web3AuthError
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.session_manager_android.SessionManager
import com.web3auth.session_manager_android.keystore.KeyStoreManager
import com.web3auth.session_manager_android.types.AES256CBC
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.json.JSONObject
import org.torusresearch.fetchnodedetails.FetchNodeDetails
import org.torusresearch.fetchnodedetails.types.NodeDetails
import org.torusresearch.torusutils.TorusUtils
import org.torusresearch.torusutils.types.TorusCtorOptions
import org.torusresearch.torusutils.types.TorusPublicKey
import org.torusresearch.torusutils.types.VerifierArgs
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.concurrent.CompletableFuture

class Web3Auth(web3AuthOptions: Web3AuthOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private var loginCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
    private lateinit var enableMfaCompletableFuture: CompletableFuture<Boolean>

    private var web3AuthResponse: Web3AuthResponse? = null
    private var web3AuthOption = web3AuthOptions
    private var sessionManager: SessionManager = SessionManager(web3AuthOption.context)
    private lateinit var credentialManager: CredentialManager
    private lateinit var trackingId: String
    private lateinit var rpId: String

    /**
     * Initializes the KeyStoreManager.
     */
    private fun initiateKeyStoreManager() {
        KeyStoreManagerUtils.getKeyGenerator()
    }

    private fun getInitOptions(): JSONObject {
        val initOptions = JSONObject()
        initOptions.put("clientId", web3AuthOption.clientId)
        initOptions.put("network", web3AuthOption.network.name.lowercase(Locale.ROOT))
        if (web3AuthOption.redirectUrl != null) initOptions.put(
            "redirectUrl", web3AuthOption.redirectUrl.toString()
        )
        if (web3AuthOption.whiteLabel != null) initOptions.put(
            "whiteLabel", gson.toJson(web3AuthOption.whiteLabel)
        )
        if (web3AuthOption.loginConfig != null) initOptions.put(
            "loginConfig", gson.toJson(web3AuthOption.loginConfig)
        )
        if (web3AuthOption.buildEnv != null) initOptions.put(
            "buildEnv", web3AuthOption.buildEnv?.name?.lowercase(Locale.ROOT)
        )
        if (web3AuthOption.mfaSettings != null) initOptions.put(
            "mfaSettings", gson.toJson(web3AuthOption.mfaSettings)
        )
        if (web3AuthOption.sessionTime != null) initOptions.put(
            "sessionTime", web3AuthOption.sessionTime
        )
        if (web3AuthOption.originData != null) initOptions.put(
            "originData", gson.toJson(web3AuthOption.originData)
        )
        return initOptions
    }

    /**
     * Retrieves the initialization parameters as a JSONObject.
     *
     * @param params The optional login parameters required for initialization. Default is null.
     * @return The initialization parameters as a JSONObject.
     */
    private fun getInitParams(params: LoginParams?): JSONObject {
        val initParams = JSONObject()
        if (params?.loginProvider != null) initParams.put(
            "loginProvider",
            params.loginProvider.name.lowercase(Locale.ROOT)
        )
        if (params?.extraLoginOptions != null) initParams.put(
            "extraLoginOptions",
            gson.toJson(params.extraLoginOptions)
        )
        initParams.put(
            "redirectUrl",
            if (params?.redirectUrl != null) params.redirectUrl.toString() else web3AuthOption.redirectUrl.toString()
        )
        if (params?.mfaLevel != null) initParams.put(
            "mfaLevel",
            params.mfaLevel.name.lowercase(Locale.ROOT)
        )
        if (params?.curve != null) initParams.put("curve", params.curve.name.lowercase(Locale.ROOT))
        if (params?.dappShare != null) initParams.put("dappShare", params.dappShare)
        return initParams
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
        val sdkUrl = Uri.parse(web3AuthOption.sdkUrl)
        val context = web3AuthOption.context
        val initOptions = getInitOptions()
        val initParams = getInitParams(params)

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
                existingExtraLoginOptions = gson.fromJson(extraOptionsString, ExtraLoginOptions::class.java)
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
                val intent = Intent(context, CustomChromeTabsActivity::class.java)
                intent.putExtra(WEBVIEW_URL, url.toString())
                context.startActivity(intent)
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
        KeyStoreManagerUtils.initializePreferences(web3AuthOption.context.applicationContext)

        //initiate keyStore
        initiateKeyStoreManager()

        //authorize session
        if (ApiHelper.isNetworkAvailable(web3AuthOption.context)) {

            //fetch project config
            fetchProjectConfig().whenComplete { _, err ->
                if (err == null) {
                    this.authorizeSession().whenComplete { resp, error ->
                        if (error == null) {
                            web3AuthResponse = resp
                            //initializeCf.complete(null)
                        } else {
                            print(error)
                        }
                        initializeCf.complete(null)
                    }
                } else {
                    initializeCf.completeExceptionally(err)
                }
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
            loginCompletableFuture.completeExceptionally(UserCancelledException())
            return
        }
        val hashUri = Uri.parse(uri.host + "?" + uri.fragment)
        val error = uri.getQueryParameter("error")
        if (error != null) {
            loginCompletableFuture.completeExceptionally(UnKnownException(error))
            if (::enableMfaCompletableFuture.isInitialized) enableMfaCompletableFuture.completeExceptionally(
                UnKnownException(error)
            )
            return
        }

        val b64Params = hashUri.getQueryParameter("b64Params")
        if (b64Params.isNullOrBlank()) {
            loginCompletableFuture.completeExceptionally(UnKnownException("Invalid Login"))
            throwEnableMFAError(ErrorCode.INVALID_LOGIN)
            return
        }
        val b64ParamString = decodeBase64URLString(b64Params).toString(Charsets.UTF_8)
        val sessionResponse = gson.fromJson(b64ParamString, SessionResponse::class.java)
        val sessionId = sessionResponse.sessionId

        if (sessionId.isNotBlank() && sessionId.isNotEmpty()) {
            sessionManager.saveSessionId(sessionId)

            //Rehydrate Session
            if (ApiHelper.isNetworkAvailable(web3AuthOption.context)) {
                this.authorizeSession().whenComplete { resp, error ->
                    if (error == null) {
                        web3AuthResponse = resp
                        if (web3AuthResponse?.error?.isNotBlank() == true) {
                            loginCompletableFuture.completeExceptionally(
                                UnKnownException(
                                    web3AuthResponse?.error
                                        ?: Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)
                                )
                            )
                            throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                        } else if (web3AuthResponse?.privKey.isNullOrBlank() && web3AuthResponse?.factorKey.isNullOrBlank()) {
                            loginCompletableFuture.completeExceptionally(
                                Exception(
                                    Web3AuthError.getError(
                                        ErrorCode.SOMETHING_WENT_WRONG
                                    )
                                )
                            )
                            throwEnableMFAError(ErrorCode.SOMETHING_WENT_WRONG)
                        } else {
                            web3AuthResponse?.sessionId?.let { sessionManager.saveSessionId(it) }

                            if (web3AuthResponse?.userInfo?.dappShare?.isNotEmpty() == true) {
                                KeyStoreManagerUtils.encryptData(
                                    web3AuthResponse?.userInfo?.verifier.plus(" | ")
                                        .plus(web3AuthResponse?.userInfo?.verifierId),
                                    web3AuthResponse?.userInfo?.dappShare!!,
                                )
                            }
                            loginCompletableFuture.complete(web3AuthResponse)
                            enableMfaCompletableFuture.complete(true)
                        }
                    } else {
                        print(error)
                    }
                }
            }
        } else {
            loginCompletableFuture.completeExceptionally(Exception(Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)))
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

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

    /**
     * Logs out the user asynchronously.
     *
     * @return A CompletableFuture<Void> representing the asynchronous operation.
     */
    fun logout(): CompletableFuture<Void> {
        val logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()
        if (ApiHelper.isNetworkAvailable(web3AuthOption.context)) {
            val sessionResponse: CompletableFuture<Boolean> = sessionManager.invalidateSession()
            sessionResponse.whenComplete { _, error ->
                if (error == null) {
                    logoutCompletableFuture.complete(null)
                } else {
                    logoutCompletableFuture.completeExceptionally(Exception(error))
                }
            }
        } else {
            logoutCompletableFuture.completeExceptionally(Exception(Web3AuthError.getError(ErrorCode.RUNTIME_ERROR)))
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
    private fun authorizeSession(): CompletableFuture<Web3AuthResponse> {
        val sessionCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
        val sessionResponse: CompletableFuture<String> = sessionManager.authorizeSession(false)
        sessionResponse.whenComplete { response, error ->
            if (error == null) {
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
            } else {
                sessionCompletableFuture.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.NOUSERFOUND
                        )
                    )
                )
            }
        }
        return sessionCompletableFuture
    }

    private fun fetchProjectConfig(): CompletableFuture<Boolean> {
        val projectConfigCompletableFuture: CompletableFuture<Boolean> = CompletableFuture()
        val web3AuthApi =
            ApiHelper.getInstance(web3AuthOption.network.name).create(ApiService::class.java)
        GlobalScope.launch {
            try {
                val result = web3AuthApi.fetchProjectConfig(
                    web3AuthOption.clientId,
                    web3AuthOption.network.name
                )
                if (result.isSuccessful && result.body() != null) {
                    val response = result.body()
                    web3AuthOption.originData =
                        web3AuthOption.originData.mergeMaps(response?.whitelist?.signed_urls)
                    if (response?.whitelabel != null) {
                        if(web3AuthOption.whiteLabel == null) {
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
        val createSessionCompletableFuture: CompletableFuture<String> = CompletableFuture()
        val sessionResponse: CompletableFuture<String> =
            sessionManager.createSession(jsonObject.toString(), 600, false)
        sessionResponse.whenComplete { response, error ->
            if (error == null) {
                createSessionCompletableFuture.complete(response)
            } else {
                createSessionCompletableFuture.completeExceptionally(error)
            }
        }
        return createSessionCompletableFuture
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
        path: String? = "wallet",
    ): CompletableFuture<Void> {
        val launchWalletServiceCF: CompletableFuture<Void> = CompletableFuture()
        val sessionId = sessionManager.getSessionId()
        if (sessionId.isNotBlank()) {
            val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)
            val context = web3AuthOption.context

            val initOptions = getInitOptions()
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
                    val intent = Intent(context, WebViewActivity::class.java)
                    intent.putExtra(WEBVIEW_URL, url.toString())
                    context.startActivity(intent)
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
        path: String? = "wallet/request"
    ): CompletableFuture<Void> {
        val signMsgCF: CompletableFuture<Void> = CompletableFuture()
        val sessionId = sessionManager.getSessionId()
        if (sessionId.isNotBlank()) {
            val sdkUrl = Uri.parse(web3AuthOption.walletSdkUrl)
            val context = web3AuthOption.context
            val initOptions = getInitOptions()
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
                    val signMessageMap = JsonObject()
                    signMessageMap.addProperty("loginId", loginId)
                    signMessageMap.addProperty("sessionId", sessionId)
                    signMessageMap.addProperty("platform", "android")

                    val requestData = JsonObject().apply {
                        addProperty("method", method)
                        addProperty("params", gson.toJson(requestParams))
                    }

                    signMessageMap.addProperty("request", gson.toJson(requestData))

                    val signMessageHash =
                        "b64Params=" + gson.toJson(signMessageMap).toByteArray(Charsets.UTF_8)
                            .toBase64URLString()

                    val url =
                        Uri.Builder().scheme(sdkUrl.scheme)
                            .encodedAuthority(sdkUrl.encodedAuthority)
                            .encodedPath(sdkUrl.encodedPath).appendEncodedPath(path)
                            .fragment(signMessageHash).build()
                    //print("message signing url: => $url")
                    val intent = Intent(context, WebViewActivity::class.java)
                    intent.putExtra(WEBVIEW_URL, url.toString())
                    intent.putExtra(REDIRECT_URL, web3AuthOption.redirectUrl.toString())
                    context.startActivity(intent)
                    signMsgCF.complete(null)
                }
            }
        } else {
            signMsgCF.completeExceptionally(Exception("Please login first to launch wallet"))
        }
        return signMsgCF
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerPasskey(
        authenticatorAttachment: AuthenticatorAttachment?,
        username: String? = null,
        rp: Rp
    ): Boolean {
        try {
            this.rpId = rp.id
            credentialManager = CredentialManager.create(web3AuthOption.context)
            val sessionId = sessionManager.getSessionId()
            if (sessionId.isBlank()) {
                throw Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
            }
            val registrationResponse =
                getRegistrationOptionsResponse(authenticatorAttachment, username, rp)
            registrationResponse.whenComplete { response, error ->
                if (error == null) {
                    GlobalScope.launch {
                        val data = createPasskey(response.data.options)
                            ?: throw Exception("passkey registration failed.")
                        val passkeyVerifierId = getPasskeyVerifierId(data)
                        val passkeyPublicKey = getPasskeyPublicKey(
                            web3AuthResponse?.userInfo?.verifier.toString(),
                            passkeyVerifierId
                        )
                        if (passkeyPublicKey == null) {
                            throw Exception("Unable to get passkey public key, please try again.")
                        }

                        val encryptedMetadata = getEncryptedMetadata(passkeyPublicKey)

                        val verificationResult = web3AuthResponse?.signatures?.let {
                            verifyRegistration(
                                data,
                                it, web3AuthResponse?.idToken.toString(), encryptedMetadata
                            )
                        }
                    }

                }
            }
            return true
        } catch (e: Exception) {
            throw Exception(Web3AuthError.getError(ErrorCode.ERROR_REGISTERING_USER))
        }
    }

    fun loginWithPasskey(authenticatorId: String?) {
        try {
            val sessionId = sessionManager.getSessionId()
            if (sessionId.isBlank()) {
                throw Exception(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
            }
            val authOptions = getAuthenticationOptions(authenticatorId)
            authOptions.whenComplete { response, error ->
                if (error == null) {
                    GlobalScope.launch {
                        val data = getSavedCredentials(response)
                        val verificationResponse =
                            gson.fromJson(data, RegistrationResponseJson::class.java)
                        val result = verifyAuthentication(verificationResponse)
                        result.whenComplete { response, error ->
                            val passKeyLoginParams = web3AuthResponse?.userInfo?.verifierId?.let {
                                PassKeyLoginParams(
                                    web3AuthResponse?.userInfo?.verifier.toString(),
                                    it, web3AuthResponse?.idToken.toString(), ExtraVerifierParams(
                                        "",
                                        verificationResponse.response.clientDataJson,
                                        verificationResponse.response.attestationObject,
                                        response.data?.publicKey.toString(),
                                        response.data?.challenge.toString(),
                                        "",
                                        rpId,
                                        verificationResponse.id
                                    )
                                )
                            }
                            val passKey = passKeyLoginParams?.let { getPasskeyPostboxKey(it) }

                            //decrypt Data
                            val decryptedData =
                                passKey?.let { decryptData(response.data?.metadata.toString(), it) }
                            if (decryptedData == null) {
                                throw Exception("Unable to decrypt data")
                            }
                            println("Decrypted data => $decryptedData")

                        }
                    }

                }
            }

        } catch (e: Exception) {
            throw Exception(Web3AuthError.getError(ErrorCode.ERROR_SIGNING_USER_IN_WITH_PASSKEYS))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPasskeyVerifierId(verificationResponse: CreatePublicKeyCredentialResponse): String {
        // Parse auth data from the given buffer
        fun parseAuthData(paramBuffer: ByteArray): AuthParamsData {
            val buffer = ByteBuffer.wrap(paramBuffer)

            // Extract rpIdHash
            val rpIdHash = ByteArray(32)
            buffer.get(rpIdHash)

            // Extract flags
            val flagsBuf = ByteArray(1)
            buffer.get(flagsBuf)
            val flagsInt = flagsBuf[0].toInt()
            val flags = Flags(
                up = flagsInt and 0x01 != 0,
                uv = flagsInt and 0x04 != 0,
                at = flagsInt and 0x40 != 0,
                ed = flagsInt and 0x80 != 0,
                flagsInt = flagsInt
            )

            // Extract counter
            val counterBuf = ByteArray(4)
            buffer.get(counterBuf)
            val counter = counterBuf.toLong()

            if (!flags.at) throw Exception("Unable to parse auth data")

            // Extract AAGUID
            val aaguid = ByteArray(16)
            buffer.get(aaguid)

            // Extract credential ID length and ID
            val credIDLenBuf = ByteArray(2)
            buffer.get(credIDLenBuf)
            val credIDLen = credIDLenBuf.toShort()
            val credID = ByteArray(credIDLen.toInt())
            buffer.get(credID)

            // Remaining buffer is the COSEPublicKey
            val COSEPublicKey = ByteArray(buffer.remaining())
            buffer.get(COSEPublicKey)

            return AuthParamsData(
                rpIdHash = rpIdHash,
                flagsBuf = flagsBuf,
                flags = flags,
                counter = counter,
                counterBuf = counterBuf,
                aaguid = aaguid,
                credID = credID,
                COSEPublicKey = COSEPublicKey
            )
        }

        // Convert ArrayBuffer to base64 URL string
        fun b64url(arrayBuffer: ByteArray): String {
            val base64String = Base64.getEncoder().encodeToString(arrayBuffer)
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(base64String.toByteArray())
        }

        val response = verificationResponse.registrationResponseJson as RegistrationResponseJson

        // Decode the attestation object and parse the auth data
        val attestationObject = Base64.getUrlDecoder().decode(response.response.attestationObject)
        val attestationStruct = decode(attestationObject)
        val authDataStruct = parseAuthData(attestationStruct.authData)

        // Convert COSEPublicKey to base64 URL string
        val base64UrlString = b64url(authDataStruct.COSEPublicKey)

        // Generate the verifier ID using Keccak-256 hash
        val keccakDigest = Keccak.Digest256()
        val verifierId = keccakDigest.digest(base64UrlString.toByteArray(Charsets.UTF_8))
        return b64url(verifierId)
    }

    private fun getRegistrationOptionsResponse(
        authenticatorAttachment: AuthenticatorAttachment?,
        username: String? = null,
        rp: Rp
    ): CompletableFuture<RegistrationResponse> {
        val registrationResponseCF: CompletableFuture<RegistrationResponse> = CompletableFuture()
        val web3AuthApi =
            ApiHelper.getPassKeysApiInstance(web3AuthOption.buildEnv?.name.toString())
                .create(ApiService::class.java)

        GlobalScope.launch {
            try {
                val requestBody = web3AuthResponse?.signatures?.let {
                    RegistrationOptionsRequest(
                        web3auth_client_id = web3AuthOption.clientId,
                        verifier_id = web3AuthResponse?.userInfo?.verifierId ?: "",
                        verifier = web3AuthResponse?.userInfo?.verifier ?: "",
                        //authenticator_attachment = authenticatorAttachment?.name ?: "",
                        rp = Rp(name = rp.name, id = rp.id),
                        username = (if (username.isNullOrBlank()) web3AuthResponse?.userInfo?.name else username).toString(),
                        network = web3AuthOption.network.name.toLowerCase(),
                        signatures = it
                    )
                }
                val result = web3AuthApi.getRegistrationOptions(
                    requestBody!!,
                    "Bearer "
                )
                if (result.isSuccessful && result.body() != null) {
                    val response = result.body() as RegistrationResponse
                    trackingId = response.data.trackingId
                    registrationResponseCF.complete(response)
                } else {
                    registrationResponseCF.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.RUNTIME_ERROR
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                registrationResponseCF.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.SOMETHING_WENT_WRONG
                        )
                    )
                )
            }
        }
        return registrationResponseCF
    }

    private suspend fun createPasskey(options: Options): CreatePublicKeyCredentialResponse? {
        val request = CreatePublicKeyCredentialRequest(gson.toJson(options))
        var response: CreatePublicKeyCredentialResponse? = null
        try {
            response = credentialManager.createCredential(
                web3AuthOption.context,
                request
            ) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialException) {
            handlePasskeyFailure(e)
        }
        return response
    }

    private fun handlePasskeyFailure(e: CreateCredentialException) {
        val msg = when (e) {
            is CreatePublicKeyCredentialDomException -> {
                // Handle the passkey DOM errors thrown according to the
                // WebAuthn spec using e.domError
                "An error occurred while creating a passkey, please check logs for additional details."
            }

            is CreateCredentialCancellationException -> {
                // The user intentionally canceled the operation and chose not
                // to register the credential.
                "The user intentionally canceled the operation and chose not to register the credential. Check logs for additional details."
            }

            is CreateCredentialInterruptedException -> {
                // Retry-able error. Consider retrying the call.
                "The operation was interrupted, please retry the call. Check logs for additional details."
            }

            is CreateCredentialProviderConfigurationException -> {
                // Your app is missing the provider configuration dependency.
                // Most likely, you're missing "credentials-play-services-auth".
                "Your app is missing the provider configuration dependency. Check logs for additional details."
            }

            is CreateCredentialUnknownException -> {
                "An unknown error occurred while creating passkey. Check logs for additional details."
            }

            is CreateCredentialCustomException -> {
                // You have encountered an error from a 3rd-party SDK. If you
                // make the API call with a request object that's a subclass of
                // CreateCustomCredentialRequest using a 3rd-party SDK, then you
                // should check for any custom exception type constants within
                // that SDK to match with e.type. Otherwise, drop or log the
                // exception.
                "An unknown error occurred from a 3rd party SDK. Check logs for additional details."
            }

            else -> {
                Log.w("Auth", "Unexpected exception type ${e::class.java.name}")
                "An unknown error occurred."
            }
        }
        Log.e("Auth", "createPasskey failed with exception: " + e.message.toString())
    }

    fun getPasskeyPublicKey(verifier: String, verifierId: String): TorusPublicKey {
        val fetchNodeDetails =
            FetchNodeDetails(TORUS_NETWORK_MAP[Network.valueOf(web3AuthOption.network.name)])
        val opts = TorusCtorOptions(
            "Custom",
            web3AuthOption.clientId,
        )
        opts.network = web3AuthOption.network.name
        opts.isEnableOneKey = true
        val torusUtils = TorusUtils(opts)
        val nodeDetails: NodeDetails = fetchNodeDetails.getNodeDetails(verifier, verifierId).get()
        val publicAddress = torusUtils.getPublicAddress(
            nodeDetails.torusNodeSSSEndpoints, nodeDetails.torusNodePub,
            VerifierArgs(verifier, verifierId, null)
        ).get();
        return publicAddress
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEncryptedMetadata(passkeyPubKey: TorusPublicKey): String {
        val metadata = getUserInfo()?.let {
            MetadataInfo(
                privKey = getPrivkey(),
                userInfo = it
            )
        }
        // Encrypting the metadata
        return encryptData(
            passkeyPubKey.finalKeyData.x,
            passkeyPubKey.finalKeyData.y,
            Json.encodeToString(metadata)
        )
    }

    /*fun encryptData(x: String, y: String, data: String): String {
        // Convert the data to JSON and then to a byte array
        val jsonData = Json.encodeToString(data)
        val dataBytes = jsonData.toByteArray(Charsets.UTF_8)

        // Convert the public key components to ECPoint
        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val ecPoint: ECPoint = ecSpec.curve.createPoint(BigInteger(Hex.decode(x)), BigInteger(Hex.decode(y)))

        // Create the public key from the ECPoint
        val keySpec = ECPublicKeySpec(ecPoint, ecSpec)
        val keyFactory = KeyFactory.getInstance("ECDH", "BC")
        val pubKey: PublicKey = keyFactory.generatePublic(keySpec)

        // Encrypt the data using the public key
        val cipher = Cipher.getInstance("ECIES", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, pubKey)
        val encryptedData = cipher.doFinal(dataBytes)

        // Convert the encrypted data to a hex string
        val encryptedDataHex = Hex.toHexString(encryptedData)

        // Return the encrypted data as a JSON string
        return Json.encodeToString(encryptedDataHex)
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    fun encryptData(x: String, y: String, data: String): String {
        val ephemKey = x.plus(y)
        val privKey = KeyStoreManager.getPrivateKey("")// how to get Privkey
        val ivKey = KeyStoreManager.randomBytes(16)
        val aes256cbc = AES256CBC(
            privKey, ephemKey, KeyStoreManager.convertByteToHexadecimal(ivKey)
        )

        val encryptedData = aes256cbc.encrypt(data.toByteArray(StandardCharsets.UTF_8))
        return KeyStoreManager.convertByteToHexadecimal(encryptedData)
    }

    fun decryptData(data: String, privKey: String): String {
        val ephemKey = KeyStoreManager.getPubKey(privKey)
        val ivKey = KeyStoreManager.randomBytes(16)
        val aes256cbc = AES256CBC(
            privKey, ephemKey, KeyStoreManager.convertByteToHexadecimal(ivKey)
        )
        val encryptedData = aes256cbc.encrypt(data.toByteArray(StandardCharsets.UTF_8))
        val mac = aes256cbc.getMac(encryptedData)
        val decryptedData = aes256cbc.decrypt(data, KeyStoreManager.convertByteToHexadecimal(mac))
        return String(decryptedData)
    }


    private fun verifyRegistration(
        registrationResponse: CreatePublicKeyCredentialResponse,
        signatures: List<String>,
        passkeyToken: String,
        data: String
    ): CompletableFuture<ChallengeData> {
        val registrationResponseCF: CompletableFuture<ChallengeData> = CompletableFuture()
        val web3AuthApi =
            ApiHelper.getPassKeysApiInstance(web3AuthOption.buildEnv?.name.toString())
                .create(ApiService::class.java)

        GlobalScope.launch {
            try {
                val requestBody = VerifyRequest(
                    web3auth_client_id = web3AuthOption.clientId,
                    tracking_id = trackingId,
                    verification_data = registrationResponse,
                    network = web3AuthOption.network.name,
                    signatures = signatures,
                    metadata = data
                )
                val result = web3AuthApi.verifyRegistration(
                    requestBody,
                    "Bearer $passkeyToken"
                )
                if (result.isSuccessful && result.body() != null && result.body()?.verified == true) {
                    val response = result.body() as VerifyRegistrationResponse
                    registrationResponseCF.complete(response.data)
                } else {
                    registrationResponseCF.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.RUNTIME_ERROR
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                registrationResponseCF.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.SOMETHING_WENT_WRONG
                        )
                    )
                )
            }
        }
        return registrationResponseCF
    }

    private fun getAuthenticationOptions(authenticatorId: String?): CompletableFuture<AuthOptions> {
        val authenticationOptionsCF: CompletableFuture<AuthOptions> = CompletableFuture()
        val web3AuthApi =
            ApiHelper.getPassKeysApiInstance(web3AuthOption.buildEnv?.name.toString())
                .create(ApiService::class.java)

        GlobalScope.launch {
            try {
                val requestBody = AuthenticationOptionsRequest(
                    web3auth_client_id = web3AuthOption.clientId,
                    rp_id = "",
                    authenticatorId,
                    network = web3AuthOption.network.name
                )
                val result = web3AuthApi.getAuthenticationOptions(
                    requestBody,
                )
                if (result.isSuccessful && result.body() != null) {
                    val response = result.body() as AuthenticationOptionsResponse
                    trackingId = response.data.trackingId
                    authenticationOptionsCF.complete(response.data.options)
                } else {
                    authenticationOptionsCF.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.RUNTIME_ERROR
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                authenticationOptionsCF.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.SOMETHING_WENT_WRONG
                        )
                    )
                )
            }
        }
        return authenticationOptionsCF
    }

    private suspend fun getSavedCredentials(options: AuthOptions): String? {
        val getPublicKeyCredentialOption =
            GetPublicKeyCredentialOption(options.toString(), null)
        val getPasswordOption = GetPasswordOption()
        val result = try {
            credentialManager.getCredential(
                web3AuthOption.context,
                GetCredentialRequest(
                    listOf(
                        getPublicKeyCredentialOption,
                        getPasswordOption
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("Auth", "getCredential failed with exception: " + e.message.toString())
            return null
        }

        if (result.credential is PublicKeyCredential) {
            val cred = result.credential as PublicKeyCredential
            //DataProvider.setSignedInThroughPasskeys(true)
            return cred.authenticationResponseJson
        }
        if (result.credential is PasswordCredential) {
            val cred = result.credential as PasswordCredential
            //DataProvider.setSignedInThroughPasskeys(false)
            return "Got Password - User:${cred.id} Password: ${cred.password}"
        }
        if (result.credential is CustomCredential) {
            //If you are also using any external sign-in libraries, parse them here with the
            // utility functions provided.
        }
        return null
    }

    private fun verifyAuthentication(publicKeyCredential: RegistrationResponseJson): CompletableFuture<VerifyAuthenticationResponse> {
        val authenticationOptionsCF: CompletableFuture<VerifyAuthenticationResponse> =
            CompletableFuture()
        val web3AuthApi =
            ApiHelper.getPassKeysApiInstance(web3AuthOption.buildEnv?.name.toString())
                .create(ApiService::class.java)

        GlobalScope.launch {
            try {
                val requestBody = VerifyAuthenticationRequest(
                    web3auth_client_id = web3AuthOption.clientId,
                    tracking_id = trackingId,
                    verification_data = publicKeyCredential,
                    network = web3AuthOption.network.name
                )
                val result = web3AuthApi.verifyAuthentication(
                    requestBody,
                )
                if (result.isSuccessful && result.body() != null && result.body()?.verified == true) {
                    val response = result.body() as VerifyAuthenticationResponse
                    authenticationOptionsCF.complete(response)
                } else {
                    authenticationOptionsCF.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.RUNTIME_ERROR
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                authenticationOptionsCF.completeExceptionally(
                    Exception(
                        Web3AuthError.getError(
                            ErrorCode.SOMETHING_WENT_WRONG
                        )
                    )
                )
            }
        }
        return authenticationOptionsCF
    }

    private fun getPasskeyPostboxKey(passKeyLoginParams: PassKeyLoginParams): String {
        val fetchNodeDetails =
            FetchNodeDetails(TORUS_NETWORK_MAP[Network.valueOf(web3AuthOption.network.name)])
        val opts = TorusCtorOptions(
            "Custom",
            web3AuthOption.clientId,
        )
        opts.network = web3AuthOption.network.name
        opts.isEnableOneKey = true
        val torusUtils = TorusUtils(opts)
        val nodeDetails: NodeDetails = fetchNodeDetails.getNodeDetails(
            passKeyLoginParams.verifier,
            passKeyLoginParams.verifierId
        ).get()
        val torusKey = torusUtils.retrieveShares(
            nodeDetails.torusNodeEndpoints, nodeDetails.torusIndexes, passKeyLoginParams.verifier,
            hashMapOf("verifier_id" to passKeyLoginParams.verifierId), passKeyLoginParams.idToken
        ).get()

        if (torusKey.finalKeyData.privKey == null) {
            throw Exception("Unable to get passkey postbox key")
        }
        return torusKey.finalKeyData.privKey.padStart(64, '0')
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
        return privKey ?: ""
    }

    /**
     * Retrieves the Ed25519 private key as a string.
     *
     * @return The Ed25519 private key as a string.
     */
    fun getEd25519PrivKey(): String {
        val ed25519Key: String? = if (web3AuthResponse == null) {
            ""
        } else {
            if (web3AuthOption.useCoreKitKey == true) {
                web3AuthResponse?.coreKitEd25519PrivKey
            } else {
                web3AuthResponse?.ed25519PrivKey
            }
        }
        return ed25519Key ?: ""
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

        private var signResponse: SignResponse? = null
        private var isCustomTabsClosed: Boolean = false
        fun setSignResponse(_response: SignResponse?) {
            signResponse = _response
        }

        fun getSignResponse(): SignResponse? {
            return signResponse
        }

        fun setCustomTabsClosed(_isCustomTabsClosed: Boolean) {
            isCustomTabsClosed = _isCustomTabsClosed
        }

        fun getCustomTabsClosed(): Boolean {
            return isCustomTabsClosed
        }
    }
}

