package com.web3auth.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.web3auth.core.types.WALLET_URL

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var mOnScrollChangedListener: OnScrollChangedListener? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val extras = intent.extras
        if (extras != null) {
            val walletUrl = extras.getString(WALLET_URL)

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (walletUrl != null) {
                        view?.loadUrl(walletUrl)
                    }
                    return false;
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    swipeRefreshLayout?.isRefreshing = false
                }
            }

            val webSettings: WebSettings = webView.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.userAgentString = "Web3Auth"

            if (walletUrl != null) {
                webView.loadUrl(walletUrl)
            }
        }

        swipeRefreshLayout?.setOnRefreshListener {
            webView.reload()
        }
    }

    override fun onStart() {
        super.onStart()
        swipeRefreshLayout?.viewTreeObserver?.addOnScrollChangedListener(
            OnScrollChangedListener {
                swipeRefreshLayout?.isEnabled = webView.scrollY == 0;
            }.also { mOnScrollChangedListener = it })
    }

    override fun onStop() {
        super.onStop()
        swipeRefreshLayout?.viewTreeObserver?.removeOnScrollChangedListener(mOnScrollChangedListener);
    }
}