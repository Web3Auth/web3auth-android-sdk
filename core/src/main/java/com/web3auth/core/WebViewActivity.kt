package com.web3auth.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.web3auth.core.types.WALLET_URL

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        if (extras != null) {
            val walletUrl = extras.getString(WALLET_URL)

            webView = WebView(this)

            val webSettings: WebSettings = webView.settings
            webSettings.javaScriptEnabled = true

            if (walletUrl != null) {
                webView.loadUrl(walletUrl)
            }
        }
        setContentView(webView)
    }
}