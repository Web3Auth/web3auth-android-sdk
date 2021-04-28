package com.openlogin.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.openlogin.core.utils.installBouncyCastle
import com.openlogin.core.utils.toDER
import com.openlogin.core.utils.toHexString
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import java.math.BigInteger

class TestActivity : AppCompatActivity() {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val key = "7b7afb9be90646859c2fd51e4e79d724753ad7984f18932be0362f31434e9713"

    private fun setOnCreate() {
        installBouncyCastle()

        val keyPair = ECKeyPair.create(BigInteger(key, 16))
        val privHex = keyPair.privateKey.toHexString()
        val pubHex = "04${keyPair.publicKey.toHexString()}"

        consoleLog(
            mapOf(
                "privKey" to privHex,
                "pubKey" to pubHex,
                "valid" to (key == privHex && "04ddd0fd09f6375c2bad3cee123283347d0e28a21cde10bb946c2d552515778046d1bbc6fdf7790805903444a7993533e868d9b2039a50f2b570ba10e30289d920" == pubHex)
            )
        )

        val msg = Hash.sha256("This is a message.".toByteArray(Charsets.UTF_8))
        val sign = keyPair.sign(msg).toDER()

        consoleLog(
            mapOf(
                "msg" to msg.toHexString(),
                "sign" to sign.toHexString(),
                "valid" to ("a3964890912366008dee9864a4dfddf88446f354b989e340f826e21b2e83bd9c" == msg.toHexString() && "304402207cbbd6ad3ac06fb1eb9d6555ad324e84df708dc1e72071c664df75f13a194d39022003c18b858c154a147427c8407e1372c57b4498bfa4211491b2be2d1f81e15872" == sign.toHexString())
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