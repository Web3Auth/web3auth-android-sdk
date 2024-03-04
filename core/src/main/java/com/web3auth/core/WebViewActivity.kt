package com.web3auth.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.webkit.WebChromeClient
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
            webSettings.userAgentString = null
            webSettings.setSupportMultipleWindows(true)

            if (walletUrl != null) {
                webView.loadUrl(walletUrl)
            }
        }

        swipeRefreshLayout?.setOnRefreshListener {
            webView.reload()
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                dialog: Boolean,
                userGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val result = view.hitTestResult
                val data = result.extra
                val context: Context = view.context
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
                context.startActivity(browserIntent)
                return false
            }
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

    override fun onBackPressed() {
        when {
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }
}