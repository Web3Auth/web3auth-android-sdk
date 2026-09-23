package com.web3auth.app

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.web3auth.core.Web3Auth
import com.web3auth.core.isEmailValid
import com.web3auth.core.isPhoneNumberValid
import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.AuthConnectionConfig
import com.web3auth.core.types.BuildEnv
import com.web3auth.core.types.Language
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.ThemeModes
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.WalletServicesConfig
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WhiteLabelData
import org.json.JSONObject
import org.torusresearch.fetchnodedetails.types.Web3AuthNetwork
import org.web3j.crypto.Credentials
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private lateinit var web3Auth: Web3Auth

    private val authConnectionList: List<AuthConnectionLogin> = listOf(
        AuthConnectionLogin("Google", AuthConnection.GOOGLE),
        AuthConnectionLogin("Facebook", AuthConnection.FACEBOOK),
        AuthConnectionLogin("Twitch", AuthConnection.TWITCH),
        AuthConnectionLogin("Discord", AuthConnection.DISCORD),
        AuthConnectionLogin("Reddit", AuthConnection.REDDIT),
        AuthConnectionLogin("Apple", AuthConnection.APPLE),
        AuthConnectionLogin("Github", AuthConnection.GITHUB),
        AuthConnectionLogin("LinkedIn", AuthConnection.LINKEDIN),
        AuthConnectionLogin("Twitter", AuthConnection.TWITTER),
        AuthConnectionLogin("Line", AuthConnection.LINE),
        AuthConnectionLogin("Hosted Email Passwordless", AuthConnection.EMAIL_PASSWORDLESS),
        AuthConnectionLogin("SMS Passwordless", AuthConnection.SMS_PASSWORDLESS),
        AuthConnectionLogin("CUSTOM", AuthConnection.CUSTOM),
        AuthConnectionLogin("Farcaster", AuthConnection.FARCASTER)
    )

    private var selectedLoginProvider: AuthConnection = AuthConnection.GOOGLE

    private val gson = Gson()
    private var TEST_VERIFIER = "torus-test-health"
    private var TORUS_TEST_EMAIL = "devnettestuser@tor.us"
    var TEST_AGGREGRATE_VERIFIER = "torus-aggregate-sapphire-mainnet"

    private fun signIn() {
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        var loginHint: String? = null
        if (selectedLoginProvider == AuthConnection.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, "Please enter a valid Email.", Toast.LENGTH_LONG).show()
                return
            }
            loginHint = hintEmail
        }

        if (selectedLoginProvider == AuthConnection.SMS_PASSWORDLESS) {
            val hintPhNo = hintEmailEditText.text.toString()
            if (hintPhNo.isBlank() || !hintPhNo.isPhoneNumberValid()) {
                Toast.makeText(this, "Please enter a valid Number.", Toast.LENGTH_LONG).show()
                return
            }
            loginHint = hintPhNo
        }

        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.connectTo(
            LoginParams(
                selectedLoginProvider,
                //authConnectionId = "w3ads",
                //groupedAuthConnectionId = "aggregate-mobile",
                loginHint = loginHint,
            )
        )
        loginCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender()
                println("PrivKey: " + web3Auth.getPrivateKey())
                println("ed25519PrivKey: " + web3Auth.getEd25519PrivateKey())
                println("Web3Auth UserInfo" + web3Auth.getUserInfo())
                web3Auth.getUserInfoAsync().whenComplete { info, infoError ->
                    if (infoError == null) {
                        Log.d("MainActivity_Web3Auth", "idToken present=${info.idToken.isNotBlank()}")
                    }
                }
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }

    private fun sfaSignIn() {
        val idToken = "" //JwtUtils.generateIdToken(TORUS_TEST_EMAIL)
        val web3AuthOptions =
            Web3AuthOptions(
                clientId = "YOUR_CLIENT_ID",
                web3AuthNetwork = Web3AuthNetwork.SAPPHIRE_MAINNET,
                redirectUrl = "torusapp://org.torusresearch.web3authexample",
            )
        web3Auth = Web3Auth(
            web3AuthOptions, this
        )
        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.connectTo(
            LoginParams(
                authConnection = selectedLoginProvider,
                authConnectionId = TEST_VERIFIER,
                idToken = idToken,
                groupedAuthConnectionId = TEST_AGGREGRATE_VERIFIER,
            )
        )
        loginCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender()
                println("PrivKey: " + web3Auth.getPrivateKey())
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
        val sfaSignInButton = findViewById<Button>(R.id.sfaSignInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val launchWalletButton = findViewById<Button>(R.id.launchWalletButton)
        val signMsgButton = findViewById<Button>(R.id.signMsgButton)
        val btnSetUpMfa = findViewById<Button>(R.id.btnSetUpMfa)
        val btnManageMfa = findViewById<Button>(R.id.btn_manageMfa)
        val spinner = findViewById<TextInputLayout>(R.id.authConnectionList)
        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)
        var key: String? = null
        var userInfo: UserInfo? = null
        try {
            key = web3Auth.getPrivateKey()
            userInfo = web3Auth.getUserInfo()
        } catch (ex: Exception) {
            print(ex)
        }


        if (userInfo != null) {
            val jsonObject = JSONObject(gson.toJson(web3Auth.getWeb3AuthResponse()))
            contentTextView.text = jsonObject.toString(4) + "\n Private Key: " + key
            contentTextView.movementMethod = ScrollingMovementMethod()
            contentTextView.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
            sfaSignInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
            launchWalletButton.visibility = View.VISIBLE
            signMsgButton.visibility = View.VISIBLE
            btnSetUpMfa.visibility = View.VISIBLE
            btnManageMfa.visibility = View.VISIBLE
            spinner.visibility = View.GONE
            hintEmailEditText.visibility = View.GONE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
            sfaSignInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
            btnSetUpMfa.visibility = View.GONE
            btnManageMfa.visibility = View.GONE
            launchWalletButton.visibility = View.GONE
            signMsgButton.visibility = View.GONE
            spinner.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val authConfig = ArrayList<AuthConnectionConfig>()
        authConfig.add(
            AuthConnectionConfig(
                authConnection = AuthConnection.GOOGLE,
                authConnectionId = "w3ads",
                groupedAuthConnectionId = "aggregate-mobile",
                clientId = "519228911939-snh959gvvmjieoo4j14kkaancbkjp34r.apps.googleusercontent.com"
            )
        )

        authConfig.add(
            AuthConnectionConfig(
                authConnection = AuthConnection.CUSTOM,
                authConnectionId = "auth0-test",
                groupedAuthConnectionId = "aggregate-mobile",
                clientId = "hUVVf4SEsZT7syOiL0gLU9hFEtm2gQ6O"
            )
        )

        val options = Web3AuthOptions(
            clientId = "BPi5PB_UiIZ-cPz1GtV5i1I2iOSOHuimiXBI0e-Oe_u6X3oVAbCiAZOTEBtTXw4tsluTITPqA8zMsfxIKMjiqNQ",
            web3AuthNetwork = Web3AuthNetwork.SAPPHIRE_MAINNET,
            redirectUrl = "torusapp://org.torusresearch.web3authexample",
            // Auth v11: auth/dashboard URLs default to /v11 (TESTING stays unversioned).
            // citadelServerUrl / storageServerUrl default from Web3AuthUrls per build env.
            //sdkUrl = "https://auth.mocaverse.xyz",
            //walletSdkUrl = "https://lrc-mocaverse.web3auth.io",
            walletServicesConfig = WalletServicesConfig(
                whiteLabel = WhiteLabelData(
                    appName = "Web3Auth Sample App",
                    defaultLanguage = Language.EN,
                    mode = ThemeModes.LIGHT,
                    useLogoLoader = true,
                    theme = hashMapOf(
                        "primary" to "#123456",
                        "onPrimary" to "#0000FF"
                    ),
                    consentRequired = false,
                    tncLink = "https://web3auth.io/docs/legal/terms-and-conditions",
                    privacyPolicy = "https://web3auth.io/docs/legal/privacy-policy",
                )
            ),
            //authConnectionConfig = authConfig,
            /*listOf(
                            AuthConnectionConfig(
                                authConnectionId = "web3auth-auth0-email-passwordless-sapphire-devnet",
                                authConnection = AuthConnection.GOOGLE,
                                clientId = "d84f6xvbdV75VTGmHiMWfZLeSPk8M07C"
                            )
                        ),*/
            // Must match the dashboard project: sapphire_mainnet client IDs use PRODUCTION.
            // TESTING hits develop-auth and will fail redirect whitelist checks for prod projects.
            authBuildEnv = BuildEnv.PRODUCTION,
            defaultChainId = "0x1",
            // Leave null to hydrate sessionTime from project config (else 30-day default).
            sessionTime = 86400,
        )

        println("params: $options")

        // Configure Web3Auth (v11 uses AuthSessionManager + citadel for session tokens)
        web3Auth = Web3Auth(
            options, this
        )

        //Set intent result url from Web3Auth redirect
        web3Auth.setResultUrl(intent.data)

        // for session response — rehydrates via citadel (SFA falls back to session-service)
        val sessionResponse: CompletableFuture<Void> = web3Auth.initialize()
        sessionResponse.whenComplete { _, error ->
            if (error == null) {
                reRender()
                println("PrivKey: " + web3Auth.getPrivateKey())
                println("ed25519PrivKey: " + web3Auth.getEd25519PrivateKey())
                println("Web3Auth UserInfo" + web3Auth.getUserInfo())
                web3Auth.getAccessToken().whenComplete { token, tokenError ->
                    if (tokenError == null) {
                        Log.d("MainActivity_Web3Auth", "accessToken present=${!token.isNullOrBlank()}")
                    }
                }
            } else {
                //handle retry login
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val sfaSignInButton = findViewById<Button>(R.id.sfaSignInButton)
        sfaSignInButton.setOnClickListener { sfaSignIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        val launchWalletButton = findViewById<Button>(R.id.launchWalletButton)
        launchWalletButton.setOnClickListener {
            val launchWalletCompletableFuture = web3Auth.showWalletUI()
            launchWalletCompletableFuture.whenComplete { _, error ->
                if (error == null) {
                    Log.d("MainActivity_Web3Auth", "Wallet launched successfully")
                } else {
                    Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                }
            }
        }

        val signMsgButton = findViewById<Button>(R.id.signMsgButton)
        signMsgButton.setOnClickListener {
            val credentials: Credentials = Credentials.create(web3Auth.getPrivateKey())
            val params = JsonArray().apply {
                add("Hello, World!")
                add(credentials.address)
                add("Android")
            }
            val signMsgCompletableFuture = web3Auth.request(
                "personal_sign", requestParams = params, appState = "web3Auth"
            )
            signMsgCompletableFuture.whenComplete { signResult, error ->
                if (error == null) {
                    showAlertDialog("Sign Result", signResult.toString())
                } else {
                    Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                }
            }
        }

        val btnSetUpMfa = findViewById<Button>(R.id.btnSetUpMfa)
        btnSetUpMfa.setOnClickListener {
            val setupMfaCf = web3Auth.enableMFA()
            setupMfaCf.whenComplete { _, error ->
                if (error == null) {
                    Log.d("MainActivity_Web3Auth", "MFA setup successfully")
                } else {
                    Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                }
            }
        }

        val btnManageMfa = findViewById<Button>(R.id.btn_manageMfa)
        btnManageMfa.setOnClickListener {
            val manageMfaCf = web3Auth.manageMFA()
            manageMfaCf.whenComplete { _, error ->
                if (error == null) {
                    Log.d("MainActivity_Web3Auth", "MFA manage successfully")
                } else {
                    Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                }
            }
        }

        val spinner = findViewById<AutoCompleteTextView>(R.id.spinnerTextView)
        val loginVerifierList: List<String> = authConnectionList.map { item ->
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
    }

    override fun onResume() {
        super.onResume()
        if (Web3Auth.getCustomTabsClosed()) {
            Toast.makeText(this, "User closes the browser.", Toast.LENGTH_SHORT).show()
            web3Auth.setResultUrl(null)
            Web3Auth.setCustomTabsClosed(false)
        }
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedLoginProvider = authConnectionList[p2].authConnection

        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)

        if (selectedLoginProvider == AuthConnection.EMAIL_PASSWORDLESS) {
            hintEmailEditText.hint = "Enter Email"
        } else if (selectedLoginProvider == AuthConnection.SMS_PASSWORDLESS) {
            hintEmailEditText.hint = "Enter Phone Number"
        }

        if (selectedLoginProvider == AuthConnection.EMAIL_PASSWORDLESS || selectedLoginProvider == AuthConnection.SMS_PASSWORDLESS) {
            hintEmailEditText.visibility = View.VISIBLE
        } else {
            hintEmailEditText.visibility = View.GONE
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}