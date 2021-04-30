package com.openlogin.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.openlogin.core.OpenLogin

class MainActivity : AppCompatActivity() {
    private lateinit var openlogin: OpenLogin

    private fun signIn() {
        openlogin.login()
    }

    private fun signOut() {
        openlogin.logout()
    }

    private fun reRender() {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        val key = openlogin.state["privKey"]
        if (key is String && key.isNotEmpty()) {
            contentTextView.text = key
            contentTextView.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
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
                "redirectUrl" to "http://localhost/app-links/auth"
            ),
            resultUrl = intent.data
        )

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        reRender()
    }
}