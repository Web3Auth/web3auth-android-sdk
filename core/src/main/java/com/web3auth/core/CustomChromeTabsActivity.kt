package com.web3auth.core

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.web3auth.core.types.REDIRECT_URL
import com.web3auth.core.types.SessionResponse
import com.web3auth.core.types.WEBVIEW_URL
import com.web3auth.core.types.WebViewResultCallback

class CustomChromeTabsActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    companion object {
        var webViewResultCallback: WebViewResultCallback? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_cct)
        webView = findViewById(R.id.webView)

        // Handle loading URL from intent extras
        val extras = intent.extras
        if (extras != null) {
            val webViewUrl = extras.getString(WEBVIEW_URL)
            val redirectUrl = extras.getString(REDIRECT_URL)
            if (webViewUrl != null) {
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (redirectUrl?.isNotEmpty() == true) {
                            if (url?.contains(redirectUrl) == true) {
                                val uri = Uri.parse(url)
                                val hashUri = Uri.parse(uri.host + "?" + uri.fragment)
                                val b64Params = hashUri.getQueryParameter("b64Params")
                                val b64ParamString =
                                    decodeBase64URLString(b64Params!!).toString(Charsets.UTF_8)
                                val sessionResponse =
                                    gson.fromJson(b64ParamString, SessionResponse::class.java)
                                println("Session Response: $sessionResponse")
                                webViewResultCallback?.onSessionResponseReceived(sessionResponse)
                                //WebViewActivity.webViewResultCallback?.onSignResponseReceived(signResponse)
                                finish()
                                return true
                            }
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {

                    }
                }
            }

            if (webViewUrl != null) {
                webView.loadUrl(webViewUrl)
            }
        }

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setSupportMultipleWindows(true)
        webView.settings.userAgentString = null

    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}