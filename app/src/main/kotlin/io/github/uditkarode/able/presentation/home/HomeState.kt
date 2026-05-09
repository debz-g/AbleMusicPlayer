package io.github.uditkarode.able.presentation.home

import io.github.uditkarode.able.model.song.Song

data class HomeState(
    /** Full sorted song list — used for queue when tapping a song. */
    val songs: List<Song> = emptyList(),
    /** Paginated window shown in the LazyColumn. */
    val displayedSongs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val currentSongPath: String = "",
    val playlists: List<String> = emptyList(),
    val hasMoreSongs: Boolean = false,
)
