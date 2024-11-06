package com.web3auth.core.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.gson.GsonBuilder
import com.web3auth.core.types.Network
import com.web3auth.session_manager_android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiHelper {
    // TODO: Abstract this to a single common source, i.e torus-common, then add these to here and fetch-node-details, etc respectively
    val SIGNER_MAP: Map<Network, String> = mapOf(
        Network.MAINNET to "https://signer.web3auth.io",
        Network.TESTNET to "https://signer.web3auth.io",
        Network.CYAN to "https://signer-polygon.web3auth.io",
        Network.AQUA to "https://signer-polygon.web3auth.io",
        Network.SAPPHIRE_MAINNET to "https://signer.web3auth.io",
        Network.SAPPHIRE_DEVNET to "https://signer.web3auth.io"
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

    fun getInstance(network: String): Retrofit {
        return Retrofit.Builder().baseUrl(SIGNER_MAP[Network.valueOf(network)])
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }

    // TODO: Abstract this to a single common source, i.e torus-common
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
}