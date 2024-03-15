package com.web3auth.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.GsonBuilder
import com.web3auth.core.types.REDIRECT_URL
import com.web3auth.core.types.SignResponse
import com.web3auth.core.types.WEBVIEW_URL

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var mOnScrollChangedListener: OnScrollChangedListener? = null
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val extras = intent.extras
        if (extras != null) {
            val webViewUrl = extras.getString(WEBVIEW_URL)
            val redirectUrl = extras.getString(REDIRECT_URL)

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (redirectUrl?.isNotEmpty() == true) {
                        if (url?.contains(redirectUrl) == true) {
                            val uri = Uri.parse(url)
                            val hashUri = Uri.parse(uri.host + "?" + uri.fragment)
                            val b64Params = hashUri.getQueryParameter("b64Params")
                            val b64ParamString =
                                decodeBase64URLString(b64Params!!).toString(Charsets.UTF_8)
                            val signResponse =
                                gson.fromJson(b64ParamString, SignResponse::class.java)
                            Web3Auth.setSignResponse(signResponse)
                            finish()
                        }
                    }
                    if (webViewUrl != null) {
                        view?.loadUrl(webViewUrl)
                    }
                    return false
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

            if (webViewUrl != null) {
                webView.loadUrl(webViewUrl)
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

        webView.addJavascriptInterface(this, "AndroidBridge");
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

    @JavascriptInterface
    fun enablePullToRefresh() {
        swipeRefreshLayout?.isEnabled = true
    }

    @JavascriptInterface
    fun disablePullToRefresh() {
        swipeRefreshLayout?.isEnabled = false
    }
}