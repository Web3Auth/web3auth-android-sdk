package com.openlogin.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.openlogin.core.OpenLogin

class MainActivity : AppCompatActivity() {
    val openlogin = OpenLogin()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        val textView = findViewById<TextView>(R.id.text)
        button.setOnClickListener {
            openlogin.login().thenApply {
                textView.visibility = View.VISIBLE
                textView.text = it.first()
            }
        }
    }
}