package com.web3auth.core.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.gson.GsonBuilder
import com.web3auth.core.types.AuthenticateEndpoints
import com.web3auth.core.types.BuildEnv
import com.web3auth.core.types.CrudEndpoints
import com.web3auth.core.types.Network
import com.web3auth.core.types.PasskeyServiceEndpoints
import com.web3auth.core.types.RegisterEndpoints
import com.web3auth.session_manager_android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiHelper {

    val SIGNER_MAP: Map<Network, String> = mapOf(
        Network.MAINNET to "https://signer.web3auth.io",
        Network.TESTNET to "https://signer.web3auth.io",
        Network.CYAN to "https://signer-polygon.web3auth.io",
        Network.AQUA to "https://signer-polygon.web3auth.io",
        Network.SAPPHIRE_MAINNET to "https://signer.web3auth.io",
        Network.SAPPHIRE_DEVNET to "https://signer.web3auth.io"
    )

    val PASSKEY_SVC_URL: Map<BuildEnv, String> = mapOf(
        BuildEnv.TESTING to "https://api-develop-passwordless.web3auth.io",
        BuildEnv.STAGING to "https://api-passwordless.web3auth.io",
        BuildEnv.PRODUCTION to "https://api-passwordless.web3auth.io"
    )

    private const val sessionBaseUrl = "https://session.web3auth.io"

    private val okHttpClient = OkHttpClient().newBuilder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
            }
        })
        .build()

    private val builder = GsonBuilder().disableHtmlEscaping().create()

    fun getPassKeysApiInstance(buildEnv: String): Retrofit {
        return Retrofit.Builder().baseUrl(PASSKEY_SVC_URL[BuildEnv.valueOf(buildEnv)])
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }

    fun getInstance(network: String): Retrofit {
        return Retrofit.Builder().baseUrl(SIGNER_MAP[Network.valueOf(network)])
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }

    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    fun getPasskeyEndpoints(buildEnv: BuildEnv): PasskeyServiceEndpoints {
        val baseUrl = PASSKEY_SVC_URL[buildEnv]
            ?: throw IllegalArgumentException("Unknown build environment: $buildEnv")

        return PasskeyServiceEndpoints(
            register = RegisterEndpoints(
                options = "$baseUrl/api/v3/auth/passkey/fast/register/options",
                verify = "$baseUrl/api/v3/auth/passkey/fast/register/verify"
            ),
            authenticate = AuthenticateEndpoints(
                options = "$baseUrl/api/v3/auth/passkey/fast/authenticate/options",
                verify = "$baseUrl/api/v3/auth/passkey/fast/authenticate/verify"
            ),
            crud = CrudEndpoints(
                list = "$baseUrl/api/v3/passkey/fast/list"
            )
        )
    }
}