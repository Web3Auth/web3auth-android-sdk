package com.openlogin.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.openlogin.app.utils.installBouncyCastle
import org.bitcoinj.core.ECKey
import org.web3j.crypto.ECKeyPair
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.MessageDigest


class TestActivity : AppCompatActivity() {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val key = "7b7afb9be90646859c2fd51e4e79d724753ad7984f18932be0362f31434e9713"

    private fun setOnCreate() {
        installBouncyCastle()

        val keyPair = ECKeyPair.create(BigInteger(key, 16))
        val privHex = keyPair.privateKey.toString(16)
        val pubHex = "04${keyPair.publicKey.toString(16)}"

        consoleLog(
            mapOf(
                "privKey" to privHex,
                "pubKey" to pubHex,
            )
        )

        val sha256 = MessageDigest.getInstance("SHA-256")
        val msg = sha256.digest("This is a message.".toByteArray(Charsets.UTF_8))
        val sign = keyPair.sign(msg)
        val der = ECKey.ECDSASignature(sign.r, sign.s).encodeToDER()

        consoleLog(
            mapOf(
                "msg" to Numeric.toHexStringNoPrefix(msg),
                "sign" to Numeric.toHexStringNoPrefix(der),
            )
        )
    }

    private fun consoleLog(msg: Any) {
        val view = findViewById<TextView>(R.id.textView)

        var text = view.text.toString()
        if (text.isNotEmpty()) text += "\n"

        text += if (msg is String) msg else gson.toJson(msg)
        view.text = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setOnCreate()
    }
}