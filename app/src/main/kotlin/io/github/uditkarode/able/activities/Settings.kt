package io.github.uditkarode.able.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.presentation.settings.SettingsScreen

@AndroidEntryPoint
class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(
                onBack = { finish() },
                onOpenDownloads = {
                    startActivity(Intent(this, Downloads::class.java))
                },
                onOpenAbout = {
                    startActivity(Intent(this, About::class.java))
                },
            )
        }
    }
}
