package com.web3auth.app

import android.content.Intent
import android.net.Uri
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
import com.web3auth.core.types.BuildEnv
import com.web3auth.core.types.ChainConfig
import com.web3auth.core.types.ChainNamespace
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.Language
import com.web3auth.core.types.LoginConfigItem
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.Network
import com.web3auth.core.types.Provider
import com.web3auth.core.types.ThemeModes
import com.web3auth.core.types.TypeOfLogin
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WhiteLabelData
import org.json.JSONObject
import org.web3j.crypto.Credentials
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private lateinit var web3Auth: Web3Auth

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
        LoginVerifier("Hosted Email Passwordless", Provider.EMAIL_PASSWORDLESS),
        LoginVerifier("SMS Passwordless", Provider.SMS_PASSWORDLESS),
        LoginVerifier("JWT", Provider.JWT),
        LoginVerifier("Farcaster", Provider.FARCASTER)
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

        if (selectedLoginProvider == Provider.SMS_PASSWORDLESS) {
            val hintPhNo = hintEmailEditText.text.toString()
            if (hintPhNo.isBlank() || !hintPhNo.isPhoneNumberValid()) {
                Toast.makeText(this, "Please enter a valid Number.", Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintPhNo)
        }

        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(
            LoginParams(
                selectedLoginProvider,
                extraLoginOptions = extraLoginOptions,
                mfaLevel = MFALevel.OPTIONAL
            )
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
        val launchWalletButton = findViewById<Button>(R.id.launchWalletButton)
        val signMsgButton = findViewById<Button>(R.id.signMsgButton)
        val btnSetUpMfa = findViewById<Button>(R.id.btnSetUpMfa)
        val btnManageMfa = findViewById<Button>(R.id.btn_manageMfa)
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


        if (userInfo != null) {
            val jsonObject = JSONObject(gson.toJson(web3Auth.getWeb3AuthResponse()))
            contentTextView.text = jsonObject.toString(4) + "\n Private Key: " + key
            contentTextView.movementMethod = ScrollingMovementMethod()
            contentTextView.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
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

        val options = Web3AuthOptions(
            clientId = "BFuUqebV5I8Pz5F7a5A2ihW7YVmbv_OHXnHYDv6OltAD5NGr6e-ViNvde3U4BHdn6HvwfkgobhVu4VwC-OSJkik",
            network = Network.SAPPHIRE_DEVNET,
            redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample"),
//            sdkUrl = "https://auth.mocaverse.xyz",
//            walletSdkUrl = "https://lrc-mocaverse.web3auth.io",
            whiteLabel = WhiteLabelData(
                "Web3Auth Sample App", null, null, null,
                Language.EN, ThemeModes.LIGHT, true,
                hashMapOf(
                    "primary" to "#123456",
                    "onPrimary" to "#0000FF"
                )
            ),
            loginConfig = hashMapOf(
                "loginConfig" to LoginConfigItem(
                    "web3auth-auth0-email-passwordless-sapphire-devnet",
                    typeOfLogin = TypeOfLogin.JWT,
                    clientId = "d84f6xvbdV75VTGmHiMWfZLeSPk8M07C",
                )
            ),
            buildEnv = BuildEnv.TESTING,
            sessionTime = 86400,
        )

        println("params: $options")

        // Configure Web3Auth
        web3Auth = Web3Auth(
            options, this
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
                //handle retry login
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        val launchWalletButton = findViewById<Button>(R.id.launchWalletButton)
        launchWalletButton.setOnClickListener {
            val launchWalletCompletableFuture = web3Auth.launchWalletServices(
                chainConfig = ChainConfig(
                    chainId = "0x89",
                    rpcTarget = "https://1rpc.io/matic",
                    chainNamespace = ChainNamespace.EIP155
                )
            )
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
            val credentials: Credentials = Credentials.create(web3Auth.getPrivkey())
            val params = JsonArray().apply {
                add("Hello, World!")
                add(credentials.address)
                add("Android")
            }
            val signMsgCompletableFuture = web3Auth.request(
                chainConfig = ChainConfig(
                    chainId = "0x89",
                    rpcTarget = "https://polygon-rpc.com/",
                    chainNamespace = ChainNamespace.EIP155
                ), "personal_sign", requestParams = params, appState = "web3Auth"
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
    }

    override fun onResume() {
        super.onResume()
        if (Web3Auth.getCustomTabsClosed()) {
            Toast.makeText(this, "User closed the browser.", Toast.LENGTH_SHORT).show()
            web3Auth.setResultUrl(null)
            Web3Auth.setCustomTabsClosed(false)
        }
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedLoginProvider = verifierList[p2].loginProvider

        val hintEmailEditText = findViewById<EditText>(R.id.etEmailHint)

        if (selectedLoginProvider == Provider.EMAIL_PASSWORDLESS) {
            hintEmailEditText.hint = "Enter Email"
        } else if (selectedLoginProvider == Provider.SMS_PASSWORDLESS) {
            hintEmailEditText.hint = "Enter Phone Number"
        }

        if (selectedLoginProvider == Provider.EMAIL_PASSWORDLESS || selectedLoginProvider == Provider.SMS_PASSWORDLESS) {
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