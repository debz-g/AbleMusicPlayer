package io.github.uditkarode.able.presentation.library

import io.github.uditkarode.able.model.song.Song

enum class LibraryMode { ARTISTS, ALBUMS }

data class LibraryGroup(val label: String, val count: Int)

data class LibraryState(
    val groups: List<LibraryGroup> = emptyList(),
    val songs: List<Song> = emptyList(),
    val mode: LibraryMode = LibraryMode.ARTISTS,
    val isLoading: Boolean = false,
)
