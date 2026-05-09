package io.github.uditkarode.able.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.player.PlayerScaffold
import io.github.uditkarode.able.presentation.songlist.LibraryDetailScreen
import javax.inject.Inject

@AndroidEntryPoint
class LibraryDetail : ComponentActivity() {
    companion object {
        @Volatile
        var pendingSongs: ArrayList<Song>? = null
    }

    @Inject lateinit var connection: MusicServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connection.bind(this)
        setContent {
            io.github.uditkarode.able.presentation.theme.AbleTheme {
                PlayerScaffold { _ ->
                    LibraryDetailScreen(onBack = { finish() })
                }
            }
        }
    }
}
