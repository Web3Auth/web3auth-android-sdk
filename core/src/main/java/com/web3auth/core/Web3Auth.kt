package com.web3auth.core

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.postDelayed
import com.google.gson.GsonBuilder
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.api.Web3AuthApi
import com.web3auth.core.api.models.LogoutApiRequest
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.types.*
import com.web3auth.core.types.Base64
import java8.util.concurrent.CompletableFuture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*

class Web3Auth(web3AuthOptions: Web3AuthOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private var loginCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()

    private var web3AuthResponse = Web3AuthResponse()
    private val web3AuthApi = ApiHelper.getInstance().create(Web3AuthApi::class.java)
    private var sessionId: String? = null
    private var web3AuthOption = web3AuthOptions

    private fun initiateKeyStoreManager() {
        KeyStoreManagerUtils.getKeyGenerator()
    }

    private fun request(
        path: String, params: LoginParams? = null, extraParams: Map<String, Any>? = null
    ) {
        val sdkUrl = Uri.parse(web3AuthOption.sdkUrl)
        val context = web3AuthOption.context

        val initParams = mutableMapOf(
            "clientId" to web3AuthOption.clientId,
            "network" to web3AuthOption.network.name.lowercase(Locale.ROOT)
        )
        if (web3AuthOption.redirectUrl != null) initParams["redirectUrl"] =
            web3AuthOption.redirectUrl.toString()
        if (web3AuthOption.whiteLabel != null) initParams["whiteLabel"] =
            gson.toJson(web3AuthOption.whiteLabel)
        if (web3AuthOption.loginConfig != null) initParams["loginConfig"] =
            gson.toJson(web3AuthOption.loginConfig)


        val paramMap = mapOf(
            "init" to initParams, "params" to params
        )
        extraParams?.let { paramMap.plus("params" to extraParams) }
        val validParams = paramMap.filterValues { it != null }

        val hash = gson.toJson(validParams).toByteArray(Charsets.UTF_8).toBase64URLString()

        val url = Uri.Builder().scheme(sdkUrl.scheme).encodedAuthority(sdkUrl.encodedAuthority)
            .encodedPath(sdkUrl.encodedPath).appendPath(path).fragment(hash).build()

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

        web3AuthResponse = gson.fromJson(
            decodeBase64URLString(hash).toString(Charsets.UTF_8), Web3AuthResponse::class.java
        )

        if (web3AuthResponse.error?.isNotBlank() == true) {
            loginCompletableFuture.completeExceptionally(
                UnKnownException(
                    web3AuthResponse.error ?: Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)
                )
            )
        } else if (web3AuthResponse.privKey.isNullOrBlank()) {
            loginCompletableFuture.completeExceptionally(Exception(Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)))
        } else {
            web3AuthResponse.sessionId?.let {
                KeyStoreManagerUtils.encryptData(
                    KeyStoreManagerUtils.SESSION_ID, it
                )
            }
            if (web3AuthResponse.userInfo?.dappShare?.isNotEmpty() == true) {
                KeyStoreManagerUtils.encryptData(
                    web3AuthResponse.userInfo?.verifier.plus(" | ")
                        .plus(web3AuthResponse.userInfo?.verifierId),
                    web3AuthResponse.userInfo?.dappShare!!,
                )
            }
            loginCompletableFuture.complete(web3AuthResponse)
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
            try {
                val ephemKey =
                    KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.EPHEM_PUBLIC_Key)
                val ivKey = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.IV_KEY)
                val mac = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.MAC)
                sessionId = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.SESSION_ID)

                if (ephemKey.isNullOrEmpty() || ivKey.isNullOrEmpty() || mac.isNullOrEmpty() || sessionId.isNullOrEmpty()) {
                    logoutCompletableFuture.complete(null)
                } else {
                    val aes256cbc = AES256CBC(
                        sessionId, ephemKey, ivKey.toString()
                    )
                    val encryptedData = aes256cbc.encrypt("".toByteArray(StandardCharsets.UTF_8))
                    val encryptedMetadata = ShareMetadata(ivKey, ephemKey, encryptedData, mac)
                    val gsonData = gson.toJson(encryptedMetadata)

                    GlobalScope.launch {
                        val result = web3AuthApi.logout(
                            LogoutApiRequest(
                                key = "04".plus(KeyStoreManagerUtils.getPubKey(sessionId = sessionId.toString())),
                                data = gsonData,
                                signature = KeyStoreManagerUtils.getECDSASignature(
                                    sessionId?.let { BigInteger(it, 16) }, gsonData
                                ),
                                timeout = 1
                            )
                        )
                        if (result.isSuccessful) {
                            //Delete local storage
                            val loginConfigItem: LoginConfigItem? =
                                web3AuthOption.loginConfig?.values?.first()
                            KeyStoreManagerUtils.deletePreferencesData(loginConfigItem?.verifier.toString())
                            Handler(Looper.getMainLooper()).postDelayed(10) {
                                logoutCompletableFuture.complete(null)
                            }
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed(10) {
                                logoutCompletableFuture.completeExceptionally(
                                    Exception(
                                        Web3AuthError.getError(
                                            ErrorCode.SOMETHING_WENT_WRONG
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                Handler(Looper.getMainLooper()).postDelayed(10) {
                    logoutCompletableFuture.completeExceptionally(ex)
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
        sessionId = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.SESSION_ID)
        if (sessionId != null && sessionId?.isNotEmpty() == true) {
            val pubKey = "04".plus(KeyStoreManagerUtils.getPubKey(sessionId.toString()))
            GlobalScope.launch {
                try {
                    val result = web3AuthApi.authorizeSession(pubKey)
                    if (result.isSuccessful && result.body() != null) {
                        val messageObj = JSONObject(result.body()?.message).toString()
                        val shareMetadata = gson.fromJson(
                            messageObj, ShareMetadata::class.java
                        )

                        KeyStoreManagerUtils.savePreferenceData(
                            KeyStoreManagerUtils.EPHEM_PUBLIC_Key,
                            shareMetadata.ephemPublicKey.toString()
                        )
                        KeyStoreManagerUtils.savePreferenceData(
                            KeyStoreManagerUtils.IV_KEY, shareMetadata.iv.toString()
                        )
                        KeyStoreManagerUtils.savePreferenceData(
                            KeyStoreManagerUtils.MAC, shareMetadata.mac.toString()
                        )

                        val aes256cbc = AES256CBC(
                            sessionId, shareMetadata.ephemPublicKey, shareMetadata.iv.toString()
                        )

                        // Implementation specific oddity - hex string actually gets passed as a base64 string
                        val encryptedShareBytes =
                            AES256CBC.toByteArray(BigInteger(shareMetadata.ciphertext, 16))
                        val share = aes256cbc.decrypt(Base64.encodeBytes(encryptedShareBytes))
                        val tempJson = JSONObject(share.toString())
                        tempJson.put("userInfo", tempJson.get("store"))
                        tempJson.remove("store")
                        web3AuthResponse =
                            gson.fromJson(tempJson.toString(), Web3AuthResponse::class.java)
                        if (web3AuthResponse.error?.isNotBlank() == true) {
                            Handler(Looper.getMainLooper()).postDelayed(10) {
                                sessionCompletableFuture.completeExceptionally(
                                    UnKnownException(
                                        web3AuthResponse.error ?: Web3AuthError.getError(
                                            ErrorCode.SOMETHING_WENT_WRONG
                                        )
                                    )
                                )
                            }
                        } else if (web3AuthResponse.privKey.isNullOrBlank()) {
                            Handler(Looper.getMainLooper()).postDelayed(10) {
                                sessionCompletableFuture.completeExceptionally(
                                    Exception(
                                        Web3AuthError.getError(ErrorCode.SOMETHING_WENT_WRONG)
                                    )
                                )
                            }
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed(10) {
                                sessionCompletableFuture.complete(web3AuthResponse)
                            }
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
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    sessionCompletableFuture.completeExceptionally(
                        Exception(
                            Web3AuthError.getError(
                                ErrorCode.NOUSERFOUND
                            )
                        )
                    )
                }
            }
        }

        return sessionCompletableFuture
    }

    fun getPrivkey(): String? {
        val privKey: String? = if (web3AuthResponse == null) {
            throw Error(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
        } else {
            if (web3AuthOption.useCoreKitKey == true) {
                web3AuthResponse.coreKitEd25519PrivKey
            } else {
                web3AuthResponse.ed25519PrivKey
            }
        }
        return privKey
    }

    fun getEd25519PrivKey(): String? {
        val ed25519Key: String? = if (web3AuthResponse == null) {
            throw Error(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
        } else {
            if (web3AuthOption.useCoreKitKey == true) {
                web3AuthResponse.coreKitEd25519PrivKey
            } else {
                web3AuthResponse.ed25519PrivKey
            }
        }
        return ed25519Key
    }

    fun getUserInfo(): UserInfo? {
        return if (web3AuthResponse == null) {
            throw Error(Web3AuthError.getError(ErrorCode.NOUSERFOUND))
        } else {
            web3AuthResponse.userInfo
        }
    }
}
