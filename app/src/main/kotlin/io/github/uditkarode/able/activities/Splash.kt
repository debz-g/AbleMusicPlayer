package io.github.uditkarode.able.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class Splash : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
