package io.github.uditkarode.able.presentation.home

import io.github.uditkarode.able.model.song.Song

data class HomeState(
    val songs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val currentSongPath: String = "",
    val playlists: List<String> = emptyList(),
)
