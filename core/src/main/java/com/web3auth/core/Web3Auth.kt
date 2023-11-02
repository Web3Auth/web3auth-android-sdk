package com.web3auth.core

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.GsonBuilder
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.types.*
import com.web3auth.session_manager_android.SessionManager
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CompletableFuture

class Web3Auth(web3AuthOptions: Web3AuthOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private var loginCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()

    private var web3AuthResponse: Web3AuthResponse? = null
    private var web3AuthOption = web3AuthOptions
    private var sessionManager: SessionManager = SessionManager(web3AuthOption.context)

    private fun initiateKeyStoreManager() {
        KeyStoreManagerUtils.getKeyGenerator()
    }

    private fun request(
        path: String, params: LoginParams? = null, extraParams: Map<String, Any>? = null
    ) {
        val sdkUrl = Uri.parse(web3AuthOption.sdkUrl)
        val context = web3AuthOption.context

        val initOptions = mutableMapOf(
            "clientId" to web3AuthOption.clientId,
            "network" to web3AuthOption.network.name.lowercase(Locale.ROOT)
        )
        if (web3AuthOption.redirectUrl != null) initOptions["redirectUrl"] =
            web3AuthOption.redirectUrl.toString()
        if (web3AuthOption.whiteLabel != null) initOptions["whiteLabel"] =
            gson.toJson(web3AuthOption.whiteLabel)
        if (web3AuthOption.loginConfig != null) initOptions["loginConfig"] =
            gson.toJson(web3AuthOption.loginConfig)
        if (web3AuthOption.buildEnv != null) initOptions["buildEnv"] =
            web3AuthOption.buildEnv.toString().lowercase()
        if (web3AuthOption.mfaSettings != null) initOptions["mfaSettings"] =
            gson.toJson(web3AuthOption.mfaSettings)

        val initParams = mutableMapOf(
            "loginProvider" to params?.loginProvider,
            "extraLoginOptions" to params?.extraLoginOptions,
            "redirectUrl" to if (params?.redirectUrl != null) params.redirectUrl.toString() else initOptions["redirectUrl"].toString(),
            "mfaLevel" to params?.mfaLevel
        )

        val paramMap = mapOf(
            "options" to initOptions, "params" to initParams, "actionType" to path
        )

        extraParams?.let { paramMap.plus("params" to extraParams) }
        val validParams = paramMap.filterValues { it != null }

        val loginIdCf = getLoginId(validParams)

        loginIdCf.whenComplete { loginId, error ->
            if (error == null) {
                val jsonObject = mapOf(
                    "loginId" to loginId
                )
                val hash = "b64Params=" + gson.toJson(jsonObject).toByteArray(Charsets.UTF_8)
                    .toBase64URLString()

                val url =
                    Uri.Builder().scheme(sdkUrl.scheme).encodedAuthority(sdkUrl.encodedAuthority)
                        .encodedPath(sdkUrl.encodedPath).appendPath("start").fragment(hash).build()

                val defaultBrowser = context.getDefaultBrowser()
                val customTabsBrowsers = context.getCustomTabsBrowsers()

                if (customTabsBrowsers.contains(defaultBrowser)) {
                    val customTabs = CustomTabsIntent.Builder().build()
                    customTabs.intent.setPackage(defaultBrowser)
                    customTabs.launchUrl(context, url)
                } else if (customTabsBrowsers.isNotEmpty()) {
                    val customTabs = CustomTabsIntent.Builder().build()
                    customTabs.intent.setPackage(customTabsBrowsers[0])
                    customTabs.launchUrl(context, url)
                } else {
                    // Open in browser externally
                    context.startActivity(Intent(Intent.ACTION_VIEW, url))
                }
            }
        }

    }

    fun initialize(): CompletableFuture<Void> {
        val initializeCf = CompletableFuture<Void>()
        KeyStoreManagerUtils.initializePreferences(web3AuthOption.context.applicationContext)

        //initiate keyStore
        initiateKeyStoreManager()

        //authorize session
        if (ApiHelper.isNetworkAvailable(web3AuthOption.context)) {
            this.authorizeSession().whenComplete { resp, error ->
                if (error == null) {
                    web3AuthResponse = resp
                } else {
                    print(error)
                }
                initializeCf.complete(null)
            }
        }
        return initializeCf
    }

    fun setResultUrl(uri: Uri?) {
        val hash = uri?.fragment
        if (hash == null) {
            loginCompletableFuture.completeExceptionally(UserCancelledException())
            return
        }
        val error = uri.getQueryParameter("error")
        if (error != null) {
            loginCompletableFuture.completeExceptionally(UnKnownException(error))
        }

        val sessionId = hash.split("&")[0].split("=")[1]

        if (sessionId != null) {
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
                        } else if (web3AuthResponse?.privKey.isNullOrBlank()) {
                            loginCompletableFuture.completeExceptionally(
                                Exception(
                                    Web3AuthError.getError(
                                        ErrorCode.SOMETHING_WENT_WRONG
                                    )
                                )
                            )
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
                        }
                    } else {
                        print(error)
                    }
                }
            }
        } else {
            loginCompletableFuture.completeExceptionally(Exception(Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)))
        }
    }

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
        request("login", loginParams)

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

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
     * Authorize User session in order to avoid re-login
     */
    private fun authorizeSession(): CompletableFuture<Web3AuthResponse> {
        val sessionCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
        val sessionResponse: CompletableFuture<String> = sessionManager.authorizeSession(false)
        sessionResponse.whenComplete { response, error ->
            if (error == null) {
                val tempJson = JSONObject(response)
                web3AuthResponse =
                    gson.fromJson(tempJson.toString(), Web3AuthResponse::class.java)
                if (web3AuthResponse?.error?.isNotBlank() == true) {
                    sessionCompletableFuture.completeExceptionally(
                        UnKnownException(
                            web3AuthResponse?.error ?: Web3AuthError.getError(
                                ErrorCode.SOMETHING_WENT_WRONG
                            )
                        )
                    )
                } else if (web3AuthResponse?.privKey.isNullOrBlank()) {
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

    private fun getLoginId(jsonObject: Map<String, Any?>): CompletableFuture<String> {
        val createSessionCompletableFuture: CompletableFuture<String> = CompletableFuture()
        if (this.sessionManager == null) {
            createSessionCompletableFuture.completeExceptionally(Exception("Session Manager is not initialized"))
        }
        val sessionResponse: CompletableFuture<String> =
            sessionManager.createSession(gson.toJson(jsonObject), 600)
        sessionResponse.whenComplete { response, error ->
            if (error == null) {
                createSessionCompletableFuture.complete(response)
            } else {
                createSessionCompletableFuture.completeExceptionally(error)
            }
        }
        return createSessionCompletableFuture
    }

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

    fun getUserInfo(): UserInfo? {
        return if (web3AuthResponse == null) {
            throw Error(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
        } else {
            web3AuthResponse?.userInfo
        }
    }
}
