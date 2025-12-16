package com.isariand.recettes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ShareReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            type = intent.type
            putExtras(intent)
            // FLAG_ACTIVITY_NEW_TASK est important ici car on vient d'un contexte externe (TikTok)
            // REORDER_TO_FRONT ramène l'instance existante au premier plan
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

        startActivity(forward)
        finish()
    }
}
