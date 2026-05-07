package io.github.uditkarode.able.presentation.player

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.model.song.SongState

data class PlayerState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val songState: SongState = SongState.paused,
    val isLoading: Boolean = false,
    val isShuffling: Boolean = false,
    val isRepeating: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    // Album art bitmap; null → show note placeholder
    val albumArt: Bitmap? = null,
    // Accent colors derived from album art palette (defaults to app indigo)
    val seekbarColor: Color = Color(0xFF5E92F3),
    val controlsColor: Color = Color.White,
    // Transient overrides set after FFmpeg metadata edits (Song.name is val)
    val editedSongName: String? = null,
    val editedArtistName: String? = null,
) {
    val displaySongName: String get() = editedSongName ?: currentSong?.name ?: ""
    val displayArtistName: String get() = editedArtistName ?: currentSong?.artist ?: ""
    val isLocalSong: Boolean get() = currentSong?.filePath?.contains("emulated/0/") == true
}
