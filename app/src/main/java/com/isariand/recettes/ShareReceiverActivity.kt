package com.isariand.recettes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On redirige vers MainActivity en réutilisant l'instance si elle existe
        val forward = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            type = intent.type
            putExtras(intent)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        startActivity(forward)
        finish()
    }
}
