package com.openlogin.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.openlogin.core.OpenLogin
import com.openlogin.core.isEmailValid
import com.openlogin.core.types.ExtraLoginOptions
import com.openlogin.core.types.LoginParams
import com.openlogin.core.types.OpenLoginOptions
import com.openlogin.core.types.OpenLoginResponse
import java8.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private lateinit var openlogin: OpenLogin

    private val verifierList : List<LoginVerifier> = listOf(
        LoginVerifier("Google", OpenLogin.Provider.GOOGLE),
        LoginVerifier("Facebook", OpenLogin.Provider.FACEBOOK),
        LoginVerifier("Twitch", OpenLogin.Provider.TWITCH),
        LoginVerifier("Discord", OpenLogin.Provider.DISCORD),
        LoginVerifier("Reddit", OpenLogin.Provider.REDDIT),
        LoginVerifier("Apple", OpenLogin.Provider.APPLE),
        LoginVerifier("Github", OpenLogin.Provider.GITHUB),
        LoginVerifier("LinkedIn", OpenLogin.Provider.LINKEDIN),
        LoginVerifier("Twitter", OpenLogin.Provider.TWITTER),
        LoginVerifier("Line", OpenLogin.Provider.LINE),
        LoginVerifier("Hosted Email Passwordless", OpenLogin.Provider.EMAIL_PASSWORDLESS)
    )

    private var selectedLoginProvider: OpenLogin.Provider = OpenLogin.Provider.GOOGLE

    private val gson = Gson()

    private fun signIn() {
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        var extraLoginOptions : ExtraLoginOptions? = null
        if (selectedLoginProvider == OpenLogin.Provider.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, "Please enter a valid Email.", Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintEmail)
        }

        val loginCompletableFuture: CompletableFuture<OpenLoginResponse> = openlogin.login(LoginParams(selectedLoginProvider, extraLoginOptions = extraLoginOptions))
        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                reRender(loginResponse)
            } else {
                Log.d("MainActivity_OpenLogin", error.message ?: "Something went wrong" )
            }

        }
    }

    private fun signOut() {
        val logoutCompletableFuture =  openlogin.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender(OpenLoginResponse())
            } else {
                Log.d("MainActivity_OpenLogin", error.message ?: "Something went wrong" )
            }
        }
    }

    private fun reRender(openLoginResponse: OpenLoginResponse) {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val spinner = findViewById<TextInputLayout>(R.id.verifierList)
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)

        val key = openLoginResponse.privKey
        val userInfo = openLoginResponse.userInfo
        if (key is String && key.isNotEmpty()) {
            contentTextView.text = gson.toJson(openLoginResponse)
            contentTextView.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
            spinner.visibility = View.GONE
            hintEmailEditText.visibility = View.GONE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
            spinner.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configure OpenLogin
        openlogin = OpenLogin(OpenLoginOptions(context = this,
            clientId = getString(R.string.openlogin_project_id),
            network = OpenLogin.Network.MAINNET,
            redirectUrl = Uri.parse("torusapp://org.torusresearch.openloginexample/redirect")))

        openlogin.setResultUrl(intent.data)

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        val spinner = findViewById<AutoCompleteTextView>(R.id.spinnerTextView)
        val loginVerifierList: List<String> = verifierList.map {
            item -> item.name
        }
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(this, R.layout.item_dropdown, loginVerifierList)
        spinner.setAdapter(adapter)
        spinner.onItemClickListener = this
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openlogin.setResultUrl(intent?.data)
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedLoginProvider = verifierList[p2].loginProvider

        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        if (selectedLoginProvider == OpenLogin.Provider.EMAIL_PASSWORDLESS) {
            hintEmailEditText.visibility = View.VISIBLE
        } else {
            hintEmailEditText.visibility = View.GONE
        }
    }
}