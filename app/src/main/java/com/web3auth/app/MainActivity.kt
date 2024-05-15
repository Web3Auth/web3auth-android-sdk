package com.web3auth.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.web3auth.core.Web3Auth
import com.web3auth.core.isEmailValid
import com.web3auth.core.types.*
import org.json.JSONObject
import org.web3j.crypto.Credentials
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
            spinner.visibility = View.GONE
            hintEmailEditText.visibility = View.GONE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
            btnSetUpMfa.visibility = View.GONE
            launchWalletButton.visibility = View.GONE
            signMsgButton.visibility = View.GONE
            spinner.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val options = Web3AuthOptions(
            context = this,
            clientId = "BHgArYmWwSeq21czpcarYh0EVq2WWOzflX-NTK-tY1-1pauPzHKRRLgpABkmYiIV_og9jAvoIxQ8L3Smrwe04Lw",
            network = Network.SAPPHIRE_DEVNET,
            redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample"),
//            sdkUrl = "https://auth.mocaverse.xyz",
//            walletSdkUrl = "https://lrc-mocaverse.web3auth.io",
            whiteLabel = WhiteLabelData(
                "Web3Auth Sample App", null, null, null,
                Language.EN, ThemeModes.LIGHT, true,
                hashMapOf(
                    "primary" to "#123456"
                )
            ),
            loginConfig = hashMapOf(
                "loginConfig" to LoginConfigItem(
                    "web3auth-auth0-email-passwordless-sapphire-devnet",
                    typeOfLogin = TypeOfLogin.JWT,
                    clientId = "d84f6xvbdV75VTGmHiMWfZLeSPk8M07C"
                )
            ),
            buildEnv = BuildEnv.TESTING,
            sessionTime = 86400,
        )

        println("params: $options")

        // Configure Web3Auth
        web3Auth = Web3Auth(
            options
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

        val signResultButton = findViewById<Button>(R.id.signResultButton)
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
                ), "personal_sign", requestParams = params
            )
            signMsgCompletableFuture.whenComplete { _, error ->
                if (error == null) {
                    Log.d("MainActivity_Web3Auth", "Message signed successfully")
                    signResultButton.visibility = View.VISIBLE
                } else {
                    Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                    signResultButton.visibility = View.GONE
                }
            }
        }

        signResultButton.setOnClickListener {
            val signResult = Web3Auth.getSignResponse()
            showAlertDialog("Sign Result", signResult.toString())
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
                if (Web3Auth.getSignResponse() != null) {
                    return
                }
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