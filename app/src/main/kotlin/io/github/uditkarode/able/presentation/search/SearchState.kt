package io.github.uditkarode.able.presentation.search

import io.github.uditkarode.able.model.song.Song

enum class SearchMode { Music, Album, Playlists }

data class SearchState(
    val query: String = "",
    val mode: SearchMode = SearchMode.Music,
    val results: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val useYouTubeMusic: Boolean = true,
)
