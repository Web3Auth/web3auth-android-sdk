package com.web3auth.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.web3auth.core.Web3Auth
import com.web3auth.core.isEmailValid
import com.web3auth.core.types.*
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private lateinit var web3Auth: Web3Auth
    private val isLoginCompleted = AtomicBoolean(false)
    private var count = 0

    private val verifierList: List<LoginVerifier> = listOf(
        LoginVerifier("Google", Provider.GOOGLE),
        LoginVerifier("Facebook", Provider.FACEBOOK),
        LoginVerifier("Twitch", Provider.TWITCH),
        LoginVerifier("Discord", Provider.DISCORD),
        LoginVerifier("Reddit", Provider.REDDIT),
        LoginVerifier("Apple", Provider.APPLE),
        LoginVerifier("Github", Provider.GITHUB),
        LoginVerifier("LinkedIn", Provider.LINKEDIN),
        LoginVerifier("Twitter", Provider.TWITTER),
        LoginVerifier("Line", Provider.LINE),
        LoginVerifier("Hosted Email Passwordless", Provider.EMAIL_PASSWORDLESS)
    )

    private var selectedLoginProvider: Provider = Provider.GOOGLE

    private val gson = Gson()

    private fun signIn() {
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        var extraLoginOptions: ExtraLoginOptions? = null
        if (selectedLoginProvider == Provider.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, "Please enter a valid Email.", Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintEmail)
        }
        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(
            LoginParams(selectedLoginProvider, extraLoginOptions = extraLoginOptions)
        )
        loginCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender()
                println("PrivKey: " + web3Auth.getPrivkey())
                println("ed25519PrivKey: " + web3Auth.getEd25519PrivKey())
                println("Web3Auth UserInfo" + web3Auth.getUserInfo())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }

    private fun signOut() {
        val logoutCompletableFuture = web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender()
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }

    private fun reRender() {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val spinner = findViewById<TextInputLayout>(R.id.verifierList)
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        var key: String? = null
        var userInfo: UserInfo? = null
        try {
            key = web3Auth.getPrivkey()
            userInfo = web3Auth.getUserInfo()
        } catch (ex: Exception) {
            print(ex)
        }

        if (key != null && userInfo != null && key.isNotEmpty()) {
            val jsonObject = JSONObject(gson.toJson(userInfo))
            contentTextView.text = jsonObject.toString(4) + "\n Private Key: " + key
            contentTextView.movementMethod = ScrollingMovementMethod()
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

        // Configure Web3Auth
        web3Auth = Web3Auth(
            Web3AuthOptions(
                context = this,
                clientId = getString(R.string.web3auth_project_id),
                network = Network.MAINNET,
                redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample/redirect"),
                whiteLabel = WhiteLabelData(
                    "Web3Auth Sample App", null, null, "en", true,
                    hashMapOf(
                        "primary" to "#123456"
                    )
                ),
                loginConfig = hashMapOf(
                    "loginConfig" to LoginConfigItem(
                        "torus",
                        typeOfLogin = TypeOfLogin.GOOGLE,
                        name = ""
                    )
                )
            )
        )

        web3Auth.setResultUrl(intent.data)

        // for session response
        val sessionResponse: CompletableFuture<Void> = web3Auth.initialize()
        sessionResponse.whenComplete { _, error ->
            if (error == null) {
                reRender()
                println("PrivKey: " + web3Auth.getPrivkey())
                println("ed25519PrivKey: " + web3Auth.getEd25519PrivKey())
                println("Web3Auth UserInfo" + web3Auth.getUserInfo())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        val spinner = findViewById<AutoCompleteTextView>(R.id.spinnerTextView)
        val loginVerifierList: List<String> = verifierList.map { item ->
            item.name
        }
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(this, R.layout.item_dropdown, loginVerifierList)
        spinner.setAdapter(adapter)
        spinner.onItemClickListener = this
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web3Auth.setResultUrl(intent?.data)
        isLoginCompleted.set(true)
    }

    override fun onResume() {
        super.onResume()
        if (isLoginCompleted.get()) {
            isLoginCompleted.set(false)
            count = 0
        } else {
            if (count > 0) {
                Toast.makeText(this, "User closed the browser.", Toast.LENGTH_SHORT).show()
                web3Auth.setResultUrl(null)
            }
            count++
        }
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedLoginProvider = verifierList[p2].loginProvider

        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        if (selectedLoginProvider == Provider.EMAIL_PASSWORDLESS) {
            hintEmailEditText.visibility = View.VISIBLE
        } else {
            hintEmailEditText.visibility = View.GONE
        }
    }
}