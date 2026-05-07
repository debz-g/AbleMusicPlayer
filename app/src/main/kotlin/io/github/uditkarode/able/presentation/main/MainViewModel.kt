package io.github.uditkarode.able.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.SongState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MiniPlayerState(
    val isVisible: Boolean = false,
    val songName: String = "",
    val artistName: String = "",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val connection: MusicServiceConnection,
) : ViewModel() {

    val miniPlayerState = connection.state
        .map { playback ->
            MiniPlayerState(
                isVisible  = playback.currentSong != null,
                songName   = playback.currentSong?.name ?: "",
                artistName = playback.currentSong?.artist ?: "",
                isPlaying  = playback.songState == SongState.playing,
                isLoading  = playback.isLoading,
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, MiniPlayerState())

    fun playPause() {
        val playback = connection.state.value
        connection.boundService?.setPlayPause(
            if (playback.songState == SongState.playing) SongState.paused else SongState.playing
        )
    }
}
