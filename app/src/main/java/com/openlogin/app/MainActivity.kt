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
import com.web3auth.core.Web3Auth
import com.web3auth.core.isEmailValid
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import java8.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private lateinit var openlogin: Web3Auth

    private val verifierList : List<LoginVerifier> = listOf(
        LoginVerifier("Google", Web3Auth.Provider.GOOGLE),
        LoginVerifier("Facebook", Web3Auth.Provider.FACEBOOK),
        LoginVerifier("Twitch", Web3Auth.Provider.TWITCH),
        LoginVerifier("Discord", Web3Auth.Provider.DISCORD),
        LoginVerifier("Reddit", Web3Auth.Provider.REDDIT),
        LoginVerifier("Apple", Web3Auth.Provider.APPLE),
        LoginVerifier("Github", Web3Auth.Provider.GITHUB),
        LoginVerifier("LinkedIn", Web3Auth.Provider.LINKEDIN),
        LoginVerifier("Twitter", Web3Auth.Provider.TWITTER),
        LoginVerifier("Line", Web3Auth.Provider.LINE),
        LoginVerifier("Hosted Email Passwordless", Web3Auth.Provider.EMAIL_PASSWORDLESS)
    )

    private var selectedLoginProvider: Web3Auth.Provider = Web3Auth.Provider.GOOGLE

    private val gson = Gson()

    private fun signIn() {
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        var extraLoginOptions : ExtraLoginOptions? = null
        if (selectedLoginProvider == Web3Auth.Provider.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, "Please enter a valid Email.", Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintEmail)
        }

        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = openlogin.login(
            LoginParams(selectedLoginProvider, extraLoginOptions = extraLoginOptions)
        )
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
                reRender(Web3AuthResponse())
            } else {
                Log.d("MainActivity_OpenLogin", error.message ?: "Something went wrong" )
            }
        }
    }

    private fun reRender(web3AuthResponse: Web3AuthResponse) {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val spinner = findViewById<TextInputLayout>(R.id.verifierList)
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)

        val key = web3AuthResponse.privKey
        val userInfo = web3AuthResponse.userInfo
        if (key is String && key.isNotEmpty()) {
            contentTextView.text = gson.toJson(web3AuthResponse)
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
        openlogin = Web3Auth(
            Web3AuthOptions(context = this,
            clientId = getString(R.string.openlogin_project_id),
            network = Web3Auth.Network.MAINNET,
            redirectUrl = Uri.parse("torusapp://org.torusresearch.openloginexample/redirect"))
        )

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
        if (selectedLoginProvider == Web3Auth.Provider.EMAIL_PASSWORDLESS) {
            hintEmailEditText.visibility = View.VISIBLE
        } else {
            hintEmailEditText.visibility = View.GONE
        }
    }
}