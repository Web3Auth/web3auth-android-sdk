package com.web3auth.core

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.web3auth.core.types.WEBVIEW_URL

class CustomChromeTabsActivity : AppCompatActivity() {

    private lateinit var customTabLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cct)

        customTabLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_CANCELED) {
                    Web3Auth.setCustomTabsClosed(true)
                    finish()
                }
            }

        val extras = intent.extras
        if (extras != null) {
            val webViewUrl = extras.getString(WEBVIEW_URL)
            if (webViewUrl != null) {
                launchCustomTabs(webViewUrl)
            }
        }
    }

    private fun launchCustomTabs(url: String) {
        val customTabsBrowsers = this.getCustomTabsBrowsers()
        if (customTabsBrowsers.isNotEmpty()) {
            val intent = CustomTabsIntent.Builder().build().intent
            intent.data = Uri.parse(url)
            customTabLauncher.launch(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}