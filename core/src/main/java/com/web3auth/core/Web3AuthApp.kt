package com.web3auth.core

import android.app.Application
import android.content.Context

open class Web3AuthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }

    companion object {
        lateinit var mInstance: Web3AuthApp
        fun getContext(): Context? {
            return mInstance.applicationContext
        }
    }
}