package com.openlogin.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.SignInButton
import com.openlogin.core.OpenLogin


class MainActivity : AppCompatActivity() {
    private lateinit var openlogin: OpenLogin

    private fun signIn() {
        openlogin.login("google")
    }

    private fun signOut() {
        openlogin.logout()
    }

    private fun reRender() {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInWithGoogleButton = findViewById<SignInButton>(R.id.signInWithGoogleButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        val key = openlogin.privKey
        if (key != null) {
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
            clientId = getString(R.string.openlogin_project_id),
            network = OpenLogin.Network.MAINNET,
            redirectUrl = "http://localhost/app-links/auth"
        )

        // Setup UI and event handlers
        val signInWithGoogleButton = findViewById<SignInButton>(R.id.signInWithGoogleButton)
        val signInWithGoogleTextView = signInWithGoogleButton.getChildAt(0) as TextView
        signInWithGoogleTextView.text = getString(R.string.sign_in_with_google)
        signInWithGoogleButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        reRender()
    }
}