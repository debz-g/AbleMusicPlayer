package io.github.uditkarode.able.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.BuildConfig
import io.github.uditkarode.able.presentation.about.AboutScreen

@AndroidEntryPoint
class About : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            io.github.uditkarode.able.presentation.theme.AbleTheme {
                AboutScreen(
                    buildType = BuildConfig.BUILD_TYPE,
                    onBack = { finish() },
                    onOpenTelegram = {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AbleApp")))
                    },
                )
            }
        }
    }
}
