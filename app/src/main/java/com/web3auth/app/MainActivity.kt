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
import java8.util.concurrent.CompletableFuture
import org.json.JSONObject

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
        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                reRender(loginResponse)
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong" )
            }
        }
    }

    private fun signOut() {
        val logoutCompletableFuture =  web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender(Web3AuthResponse())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong" )
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
            var string = "{\"privKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf\",\"sessionId\":\"62c9a25df9b54759eece08bdf8f43a3c0548e64d2a82757cff3d52cbe1739306\",\"ed25519PrivKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf22c725e309c9c6692e9664f7a606a2590fa03feb07e7f0f00525dd8a4f7983cd\",\"privKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf\",\"sessionId\":\"62c9a25df9b54759eece08bdf8f43a3c0548e64d2a82757cff3d52cbe1739306\",\"ed25519PrivKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf22c725e309c9c6692e9664f7a606a2590fa03feb07e7f0f00525dd8a4f7983cd\",\"privKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf\",\"sessionId\":\"62c9a25df9b54759eece08bdf8f43a3c0548e64d2a82757cff3d52cbe1739306\",\"ed25519PrivKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf22c725e309c9c6692e9664f7a606a2590fa03feb07e7f0f00525dd8a4f7983cd\",\"privKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf\",\"sessionId\":\"62c9a25df9b54759eece08bdf8f43a3c0548e64d2a82757cff3d52cbe1739306\",\"ed25519PrivKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf22c725e309c9c6692e9664f7a606a2590fa03feb07e7f0f00525dd8a4f7983cd\",\"privKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf\",\"sessionId\":\"62c9a25df9b54759eece08bdf8f43a3c0548e64d2a82757cff3d52cbe1739306\",\"ed25519PrivKey\":\"0ed0dc5df01aa12fe086ed79d191753349aa30179eeca1c133aac100624d1adf22c725e309c9c6692e9664f7a606a2590fa03feb07e7f0f00525dd8a4f7983cd\",\"userInfo\":{\"email\":\"gaurav@tor.us\",\"name\":\"GauravGoel\",\"profileImage\":\"https://lh3.googleusercontent.com/a/ALm5wu1Dje-Y60iGsfYH6vLA_4Q5etTazFWs7lW7tJoW=s96-c\",\"aggregateVerifier\":\"tkey-google\",\"verifier\":\"torus\",\"verifierId\":\"gaurav@tor.us\",\"typeOfLogin\":\"google\",\"dappShare\":\"\",\"idToken\":\"eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlRZT2dnXy01RU9FYmxhWS1WVlJZcVZhREFncHRuZktWNDUzNU1aUEMwdzAifQ.eyJpYXQiOjE2NjU2NTMzOTAsImF1ZCI6IkJBU2RaeHA1dFdxUEdremJPVTRWTUpzbno4UU9wSjJweXhmNmtaY3V4SThBSjFnc093NU1nYlc5NlJwQUF4X0trLTVwNHJoT29wUGhDYTlrUl85bUtwRSIsIm5vbmNlIjoiMDIzMGQ0OTYwNTc3YWM4OTQyODE0N2QxMzFhODY1ZjliOGMzMGMyZWVlMGZmMzQ3N2Y0OWJlMzVjOTY4NjU0NDk2IiwiaXNzIjoiaHR0cHM6Ly9hcGkub3BlbmxvZ2luLmNvbSIsIndhbGxldHMiOlt7InB1YmxpY19rZXkiOiIwM2E3YzQ1MDhhYTU0MTZiMzA3YzA0YmM1MTQwZWQzOTE4YmM2YzllM2I4YzI2MjE0MTBkMzAyNDQ4ZWE4Mzg5MGMiLCJ0eXBlIjoid2ViM2F1dGhfYXBwX2tleSIsImN1cnZlIjoic2VjcDI1NmsxIn1dLCJlbWFpbCI6ImdhdXJhdkB0b3IudXMiLCJuYW1lIjoiR2F1cmF2IEdvZWwiLCJwcm9maWxlSW1hZ2UiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BTG01d3UxRGplLVk2MGlHc2ZZSDZ2TEFfNFE1ZXRUYXpGV3M3bFc3dEpvVz1zOTYtYyIsInZlcmlmaWVyIjoidG9ydXMiLCJ2ZXJpZmllcklkIjoiZ2F1cmF2QHRvci51cyIsImFnZ3JlZ2F0ZVZlcmlmaWVyIjoidGtleS1nb29nbGUiLCJleHAiOjE2NjU3Mzk3OTB9.ef3RbLjWOLuOvV22Mue5ggWtU-je1FYuOAULHFO4UoFy7AC2mT53aPBcop81Mk0bkYd_201jlIlZDFlNdFyipw\",\"oAuthIdToken\":\"\"}}"
            val jsonObject = JSONObject(gson.toJson(web3AuthResponse))
            contentTextView.text = jsonObject.toString(4)
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
            Web3AuthOptions(context = this,
            clientId = getString(R.string.web3auth_project_id),
            network = Web3Auth.Network.MAINNET,
            redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample/redirect"),
                whiteLabel = WhiteLabelData(
                    "Web3Auth Sample App", null, null, "en", true,
                    hashMapOf(
                        "primary" to "#123456"
                    )
                )
            )
        )

        web3Auth.setResultUrl(intent.data)

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