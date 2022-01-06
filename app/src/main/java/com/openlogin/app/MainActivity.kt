package com.openlogin.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.openlogin.core.AuthStateChangeListener
import com.openlogin.core.OpenLogin
import com.openlogin.core.isEmailValid
import com.openlogin.core.types.ExtraLoginOptions
import com.openlogin.core.types.LoginParams
import com.openlogin.core.types.OpenLoginOptions

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
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

        openlogin.login(LoginParams(selectedLoginProvider, extraLoginOptions = extraLoginOptions))
    }

    private fun signOut() {
        openlogin.logout()
    }

    private fun reRender() {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val spinner = findViewById<Spinner>(R.id.verifierList)
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)

        val key = openlogin.state.privKey
        val userInfo = openlogin.state.userInfo
        if (key is String && key.isNotEmpty()) {
            contentTextView.text = gson.toJson(openlogin.state)
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
        openlogin.addAuthStateChangeListener(AuthStateChangeListener {
            reRender()
        })

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        val spinner = findViewById<Spinner>(R.id.verifierList)
        val loginVerifierList: List<String> = verifierList.map {
            item -> item.name
        }
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, loginVerifierList)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openlogin.setResultUrl(intent?.data)
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedLoginProvider = verifierList[p2].loginProvider

        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        if (selectedLoginProvider == OpenLogin.Provider.EMAIL_PASSWORDLESS) {
            hintEmailEditText.visibility = View.VISIBLE
        } else {
            hintEmailEditText.visibility = View.GONE
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }
}