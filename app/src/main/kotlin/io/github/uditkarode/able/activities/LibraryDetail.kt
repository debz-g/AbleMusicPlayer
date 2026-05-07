package io.github.uditkarode.able.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.songlist.LibraryDetailScreen

@AndroidEntryPoint
class LibraryDetail : ComponentActivity() {
    companion object {
        @Volatile
        var pendingSongs: ArrayList<Song>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibraryDetailScreen(onBack = { finish() })
        }
    }
}
