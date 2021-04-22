package com.openlogin.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {
    companion object {
        const val SIGN_IN_WITH_GOOGLE = 9000
    }

    private lateinit var googleSignIn: GoogleSignInClient

    private var googleAccount: GoogleSignInAccount? = null

    private fun signInWithGoogle() {
        if (googleAccount != null) return
        startActivityForResult(googleSignIn.signInIntent, SIGN_IN_WITH_GOOGLE)
    }

    private fun onSignedInWithGoogle(task: Task<GoogleSignInAccount>) {
        try {
            googleAccount = task.result
        } catch (e: ApiException) {
            Log.w(localClassName, "signInWithGoogle:failed code=" + e.statusCode);
        }
        reRender()
    }

    private fun signOut() {
        googleSignIn.signOut().addOnCompleteListener {
            googleAccount = null
            reRender()
        }
    }

    private fun reRender() {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInWithGoogleButton = findViewById<SignInButton>(R.id.signInWithGoogleButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        val account = googleAccount
        if (account != null) {
            contentTextView.text = account.displayName
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

        googleSignIn = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        )

        val signInWithGoogleButton = findViewById<SignInButton>(R.id.signInWithGoogleButton)
        val signInWithGoogleTextView = signInWithGoogleButton.getChildAt(0) as TextView
        signInWithGoogleTextView.text = getString(R.string.sign_in_with_google)
        signInWithGoogleButton.setOnClickListener { signInWithGoogle() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        reRender()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_WITH_GOOGLE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            onSignedInWithGoogle(task)
        }
    }
}