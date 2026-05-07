package io.github.uditkarode.able.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.presentation.welcome.WelcomeScreen

@AndroidEntryPoint
class Welcome : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WelcomeScreen(
                onContinue = {
                    getSharedPreferences("able_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("welcome_shown", true)
                        .apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
            )
        }
    }
}
