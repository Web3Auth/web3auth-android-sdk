package com.web3auth.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.postDelayed
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.web3auth.core.api.ApiHelper
import com.web3auth.core.api.Web3AuthApi
import com.web3auth.core.api.models.LogoutApiRequest
import com.web3auth.core.api.models.RefreshSessionRequest
import com.web3auth.core.keystore.KeyStoreManagerUtils
import com.web3auth.core.types.*
import com.web3auth.core.types.Base64
import com.web3auth.core.utils.*
import java8.util.concurrent.CompletableFuture
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*

class Web3Auth(web3AuthOptions: Web3AuthOptions) {
    enum class Network {
        @SerializedName("mainnet")
        MAINNET,

        @SerializedName("testnet")
        TESTNET,

        @SerializedName("cyan")
        CYAN
    }

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private val sdkUrl = Uri.parse(web3AuthOptions.sdkUrl)
    private val initParams: Map<String, Any>
    private val context: Context

    private var loginCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()
    private var logoutCompletableFuture: CompletableFuture<Void> = CompletableFuture()
    private var sessionCompletableFuture: CompletableFuture<Web3AuthResponse> = CompletableFuture()

    private var web3AuthResponse = Web3AuthResponse()
    private var shareMetadata = ShareMetadata()
    private val web3AuthApi = ApiHelper.getInstance().create(Web3AuthApi::class.java)
    private var sessionId: String? = null
    private var web3AuthOption = web3AuthOptions

    init {
        //initiate keyStore
        initiateKeyStoreManager()

        // Build init params
        val initParams = mutableMapOf(
            "clientId" to web3AuthOptions.clientId,
            "network" to web3AuthOptions.network.name.lowercase(Locale.ROOT)
        )
        if (web3AuthOptions.redirectUrl != null) initParams["redirectUrl"] =
            web3AuthOptions.redirectUrl.toString()
        if (web3AuthOptions.whiteLabel != null) initParams["whiteLabel"] =
            gson.toJson(web3AuthOptions.whiteLabel)
        if (web3AuthOptions.loginConfig != null) initParams["loginConfig"] =
            gson.toJson(web3AuthOptions.loginConfig)

        this.initParams = initParams
        this.context = web3AuthOptions.context

        //authorize session
        authorizeSession()
    }

    private fun initiateKeyStoreManager() {
        KeyStoreManagerUtils.getKeyGenerator()
    }

    private fun request(
        path: String,
        params: LoginParams? = null,
        extraParams: Map<String, Any>? = null
    ) {
        val paramMap = mapOf(
            "init" to initParams,
            "params" to params
        )
        extraParams?.let { paramMap.plus("params" to extraParams) }
        val validParams = paramMap.filterValues { it != null }

        val hash = gson.toJson(validParams).toByteArray(Charsets.UTF_8).toBase64URLString()

        val url = Uri.Builder().scheme(sdkUrl.scheme)
            .encodedAuthority(sdkUrl.encodedAuthority)
            .encodedPath(sdkUrl.encodedPath)
            .appendPath(path)
            .fragment(hash)
            .build()

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
            decodeBase64URLString(hash).toString(Charsets.UTF_8),
            Web3AuthResponse::class.java
        )
        if (web3AuthResponse.error?.isNotBlank() == true) {
            loginCompletableFuture.completeExceptionally(
                UnKnownException(
                    web3AuthResponse.error ?: "Something went wrong"
                )
            )
        }

        if (web3AuthResponse.privKey.isNullOrBlank()) {
            logoutCompletableFuture.complete(null)
        }

        web3AuthResponse.sessionId?.let {
            KeyStoreManagerUtils.encryptData(
                KeyStoreManagerUtils.SESSION_ID,
                it
            )
        }
        web3AuthResponse.userInfo?.idToken?.let {
            KeyStoreManagerUtils.encryptData(
                KeyStoreManagerUtils.ID_TOKEN,
                it
            )
        }
        web3AuthResponse.appRefreshToken?.let {
            KeyStoreManagerUtils.encryptData(
                KeyStoreManagerUtils.REFRESH_TOKEN,
                it
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

    fun logout(params: Map<String, Any>? = null): CompletableFuture<Void> {
        sessionTimeOutAPI()
        request("logout", extraParams = params)

        logoutCompletableFuture = CompletableFuture()
        return logoutCompletableFuture
    }

    /**
     * Authorize User session in order to avoid re-login
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun authorizeSession() {
        sessionCompletableFuture = CompletableFuture()
        sessionId = KeyStoreManagerUtils.decryptData(KeyStoreManagerUtils.SESSION_ID)
        val idToken =
            KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.ID_TOKEN)
        if (idToken?.isNotEmpty() == true) {
            JwtUtils.isTokenExpired(idToken)
            println("isTokenExpired" + JwtUtils.isTokenExpired(idToken))
        }
        if (sessionId != null && sessionId?.isNotEmpty() == true) {
            val pubKey = "04".plus(KeyStoreManagerUtils.getPubKey(sessionId.toString()))
            GlobalScope.launch {
                val result = web3AuthApi.authorizeSession(pubKey)
                if (result.isSuccessful && result.body() != null) {
                    val messageObj = result.body()?.message?.let { JSONObject(it).toString() }
                    this@Web3Auth.shareMetadata = gson.fromJson(
                        messageObj,
                        ShareMetadata::class.java
                    )
                    println("shareMetadata$shareMetadata")

                    KeyStoreManagerUtils.savePreferenceData(
                        KeyStoreManagerUtils.EPHEM_PUBLIC_Key,
                        shareMetadata.ephemPublicKey.toString()
                    )
                    KeyStoreManagerUtils.savePreferenceData(
                        KeyStoreManagerUtils.IV_KEY,
                        shareMetadata.iv.toString()
                    )
                    KeyStoreManagerUtils.savePreferenceData(
                        KeyStoreManagerUtils.MAC,
                        shareMetadata.mac.toString()
                    )

                    val aes256cbc = AES256CBC(
                        sessionId,
                        shareMetadata.ephemPublicKey,
                        shareMetadata.iv.toString()
                    )

                    // Implementation specific oddity - hex string actually gets passed as a base64 string
                    try {
                        val encryptedShareBytes =
                            AES256CBC.toByteArray(shareMetadata.ciphertext?.let { BigInteger(it, 16) })
                        val share = aes256cbc.decrypt(Base64.encodeBytes(encryptedShareBytes))
                        val tempJson = JSONObject(share.toString())
                        tempJson.put("userInfo", tempJson.get("store"))
                        tempJson.remove("store")
                        web3AuthResponse =
                            gson.fromJson(tempJson.toString(), Web3AuthResponse::class.java)
                        if (web3AuthResponse != null) {
                            if (web3AuthResponse.error?.isNotBlank() == true) {
                                Handler(Looper.getMainLooper()).postDelayed(10) {
                                    sessionCompletableFuture.completeExceptionally(
                                        UnKnownException(
                                            web3AuthResponse.error ?: "Something went wrong"
                                        )
                                    )
                                }
                            }

                            if (web3AuthResponse.privKey.isNullOrBlank()) {
                                Handler(Looper.getMainLooper()).postDelayed(10) {
                                    sessionCompletableFuture.complete(null)
                                }
                            }

                            Handler(Looper.getMainLooper()).postDelayed(10) {
                                if(JwtUtils.isTokenExpired(idToken.toString())) {
                                    refreshSession(web3AuthResponse)
                                } else {
                                    sessionCompletableFuture.complete(web3AuthResponse)
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    fun sessionResponse(): CompletableFuture<Web3AuthResponse> {
        return sessionCompletableFuture
    }

    /**
     * Session TimeOut API for logout
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun sessionTimeOutAPI() {
        val ephemKey =
            KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.EPHEM_PUBLIC_Key)
        val ivKey = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.IV_KEY)
        val mac = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.MAC)

        if (ephemKey?.isEmpty() == true && ivKey?.isEmpty() == true) return

        val aes256cbc = AES256CBC(
            sessionId,
            ephemKey,
            ivKey.toString()
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
                        sessionId?.let { BigInteger(it, 16) },
                        gsonData
                    ),
                    timeout = 1
                )
            )
            if (result.isSuccessful) {
                //Delete local storage
                val loginConfigItem: LoginConfigItem? = web3AuthOption.loginConfig?.values?.first()
                KeyStoreManagerUtils.deletePreferencesData(loginConfigItem?.verifier.toString())
            }
        }
    }

    private fun refreshSession(web3AuthResponse: Web3AuthResponse) {
        val refreshToken = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.REFRESH_TOKEN)
        val ephemKey = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.EPHEM_PUBLIC_Key)
        val ivKey = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.IV_KEY)
        val mac = KeyStoreManagerUtils.getPreferencesData(KeyStoreManagerUtils.MAC)
        val mewKeyPair = KeyStoreManagerUtils.generateKeyPair().toString()

        if (ephemKey?.isEmpty() == true && ivKey?.isEmpty() == true) return
        val aes256cbc = AES256CBC(
            mewKeyPair,
            ephemKey,
            ivKey.toString()
        )
        val encryptedData = aes256cbc.encrypt(web3AuthResponse.userInfo.toString().toByteArray(StandardCharsets.UTF_8))
        val encryptedMetadata = ShareMetadata(ivKey, ephemKey, encryptedData, mac)
        val gsonData = gson.toJson(encryptedMetadata)
        GlobalScope.launch {
            val result = web3AuthApi.refreshSession(
                RefreshSessionRequest(
                    refresh_token = refreshToken,
                    old_session_key = sessionId,
                    key = "04".plus(KeyStoreManagerUtils.getPubKey(sessionId = mewKeyPair)),
                    data = gsonData,
                    signature =  KeyStoreManagerUtils.getECDSASignature(
                        BigInteger(mewKeyPair, 16),
                        gsonData
                    ),
                    namespace = web3AuthResponse.userInfo?._sessionNamespace
                )
            )
            if (result.isSuccessful) {
                web3AuthResponse.sessionId?.let {
                    KeyStoreManagerUtils.encryptData(
                        KeyStoreManagerUtils.SESSION_ID,
                        it
                    )
                }
                web3AuthResponse.userInfo?.idToken?.let {
                    KeyStoreManagerUtils.encryptData(
                        KeyStoreManagerUtils.ID_TOKEN,
                        it
                    )
                }
                web3AuthResponse.appRefreshToken?.let {
                    KeyStoreManagerUtils.encryptData(
                        KeyStoreManagerUtils.REFRESH_TOKEN,
                        it
                    )
                }
            }
        }
    }

    fun logout(
        redirectUrl: Uri? = null,
        appState: String? = null
    ) {
        val params = mutableMapOf<String, Any>()
        if (redirectUrl != null) params["redirectUrl"] = redirectUrl.toString()
        if (appState != null) params["appState"] = appState
        logout(params)
    }
}
