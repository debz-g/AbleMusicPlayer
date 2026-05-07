package io.github.uditkarode.able.presentation.songlist

import io.github.uditkarode.able.model.song.Song

data class SongListState(
    val title: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val artistName: String = "",
    val artUrl: String = "",
    val error: String? = null,
)
