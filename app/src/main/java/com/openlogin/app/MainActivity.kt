package com.openlogin.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.openlogin.core.OpenLogin

const val APP_LINK_BASE_URL = "http://localhost/app-links"
const val APP_LINK_LOGGED_IN_URL = "${APP_LINK_BASE_URL}/logged-in"
const val APP_LINK_LOGGED_OUT_URL = "${APP_LINK_BASE_URL}/logged-out"

class MainActivity : AppCompatActivity() {
    private lateinit var openlogin: OpenLogin

    private fun signIn() {
        openlogin.login(
            mapOf(
                "redirectUrl" to APP_LINK_LOGGED_IN_URL,
                "loginProvider" to "discord"
            )
        )
    }

    private fun signOut() {
        openlogin.logout(
            mapOf(
                "redirectUrl" to APP_LINK_LOGGED_OUT_URL
            )
        )
    }

    private fun reRender() {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInWithGoogleButton = findViewById<Button>(R.id.signInWithGoogleButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        val key = openlogin.state["privKey"]
        if (key is String) {
            contentTextView.text = key
            contentTextView.visibility = View.VISIBLE
            signInWithGoogleButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInWithGoogleButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configure OpenLogin
        openlogin = OpenLogin(
            this,
            sdkUrl = "http://10.0.2.2:3000",
            params = mapOf(
                "clientId" to getString(R.string.openlogin_project_id),
                "network" to "mainnet",
                "redirectUrl" to APP_LINK_LOGGED_IN_URL
            )
        )

        val redirectUrl = intent.data
        if (redirectUrl != null && redirectUrl.path == Uri.parse(APP_LINK_LOGGED_IN_URL).path)
            openlogin.onLoggedIn(redirectUrl)

        // Setup UI and event handlers
        val signInWithGoogleButton = findViewById<Button>(R.id.signInWithGoogleButton)
        signInWithGoogleButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        reRender()
    }
}