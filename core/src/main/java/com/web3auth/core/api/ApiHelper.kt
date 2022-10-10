package com.web3auth.core.api

import com.google.gson.GsonBuilder
import com.web3auth.core.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiHelper {

    private const val baseUrl = "https://broadcast-server.tor.us"

    private val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            if(BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
            }
        })
        .build()

    private val builder = GsonBuilder().disableHtmlEscaping().create()

    fun getInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }
}