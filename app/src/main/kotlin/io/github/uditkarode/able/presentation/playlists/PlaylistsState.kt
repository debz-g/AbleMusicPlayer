package io.github.uditkarode.able.presentation.playlists

import io.github.uditkarode.able.model.Playlist

data class PlaylistsState(
    val playlists: List<Playlist> = emptyList(),
    val isImporting: Boolean = false,
)
