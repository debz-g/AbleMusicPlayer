package io.github.uditkarode.able.data.player

import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.model.song.SongState

data class PlaybackState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val songState: SongState = SongState.paused,
    val isLoading: Boolean = false,
    val isShuffling: Boolean = false,
    val isRepeating: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val isSpotifyImporting: Boolean = false,
) {
    val currentSong: Song? get() = queue.getOrNull(currentIndex)
}
