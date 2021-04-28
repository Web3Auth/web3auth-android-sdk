package com.openlogin.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.openlogin.core.utils.installBouncyCastle
import com.openlogin.core.utils.toBase64URLString
import com.openlogin.core.utils.toDER
import com.openlogin.core.utils.toHexString
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import java.math.BigInteger

class TestActivity : AppCompatActivity() {
    private val prettyGson = GsonBuilder().setPrettyPrinting().create()
    private val defaultGson = Gson()
    private val key = "<POSTBOX KEY>"

    private fun setOnCreate() {
        installBouncyCastle()

        val keyPair = ECKeyPair.create(BigInteger(key.padStart(64, '0'), 16))
        val userData = mapOf(
            "clientId" to getString(R.string.openlogin_project_id),
            "timestamp" to "1619606866322"
        )

        consoleLog(
            mapOf(
                "userData" to defaultGson.toJson(userData),
                "valid" to (defaultGson.toJson(userData) == "{\"clientId\":\"BEKbgRFZnqnMQFOQYcDdYFq0mOxZGdbVkIxzr-YoRpWWFQD5g04aAMc2xF1sf-qZ0StRkOOHqSkqQozdpwBXAz8\",\"timestamp\":\"1619606866322\"}")
            )
        )

        val hash =
            Hash.sha3(defaultGson.toJson(userData).toByteArray(Charsets.UTF_8))
        consoleLog(
            mapOf(
                "hash" to hash.toHexString(),
                "valid" to (hash.toHexString() == "a9de071c223fda7c19e6589a4617dc7eb5e39da7b12711968423e5ccc68de7d9")
            )
        )

        val user = "04${keyPair.publicKey.toString(16)}"
        consoleLog(
            mapOf(
                "user" to user,
                "valid" to (user == "04b99258b5f4c0267a9b932ddc6e08245368a322b253b54d971320c0ab65e47f446f46f843e319fd9e25d85400618e9a3d502a1222d5a273efdc25ddf0e2b9de2f")
            )
        )

        val sig = keyPair.sign(hash).toDER()
        consoleLog(
            mapOf(
                "sig" to sig.toBase64URLString(),
                "valid" to (sig.toBase64URLString() ==
                        "MEQCIDT6s_pGtF_YvQbWOvPZov0puElsrYOXZY_igBM1ZZTvAiAmHP45Kvwf3WPmFCbxvrgV4FSA-CBV1G2pcWNHOdiifg")
            )
        )
    }

    private fun consoleLog(msg: Any) {
        val view = findViewById<TextView>(R.id.textView)

        var text = view.text.toString()
        if (text.isNotEmpty()) text += "\n"

        text += if (msg is String) msg else prettyGson.toJson(msg)
        view.text = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setOnCreate()
    }
}