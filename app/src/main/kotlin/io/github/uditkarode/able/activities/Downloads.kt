package io.github.uditkarode.able.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.presentation.downloads.DownloadsScreen

@AndroidEntryPoint
class Downloads : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownloadsScreen(onBack = { finish() })
        }
    }
}
