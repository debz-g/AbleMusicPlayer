package io.github.uditkarode.able.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.presentation.player.PlayerScaffold
import io.github.uditkarode.able.presentation.downloads.DownloadsScreen
import javax.inject.Inject

@AndroidEntryPoint
class Downloads : ComponentActivity() {
    @Inject lateinit var connection: MusicServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connection.bind(this)
        setContent {
            io.github.uditkarode.able.presentation.theme.AbleTheme {
                PlayerScaffold { _ ->
                    DownloadsScreen(onBack = { finish() })
                }
            }
        }
    }
}
