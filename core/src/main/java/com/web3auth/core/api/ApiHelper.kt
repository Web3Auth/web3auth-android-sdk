package com.web3auth.core.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.gson.GsonBuilder
import com.web3auth.core.types.BuildEnv
import com.web3auth.core.types.Web3AuthUrls
import com.web3auth.session_manager_android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiHelper {

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

    /**
     * Project config client. Auth v11 uses [DASHBOARD_PUBLIC_API_MAP] (signer-service)
     * keyed by build env, not the legacy network-specific signer host.
     */
    fun getInstance(buildEnv: BuildEnv): Retrofit {
        val baseUrl = Web3AuthUrls.dashboardPublicApiUrl(buildEnv).trimEnd('/') + "/"
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }

    @Deprecated(
        message = "Use getInstance(BuildEnv) — project config is keyed by build env in Auth v11.",
        replaceWith = ReplaceWith("getInstance(buildEnv)")
    )
    fun getInstance(network: String): Retrofit {
        return getInstance(BuildEnv.PRODUCTION)
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
}