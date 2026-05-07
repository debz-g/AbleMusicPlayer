package io.github.uditkarode.able.presentation.home

import io.github.uditkarode.able.model.song.Song

sealed interface HomeIntent {
    data object Load : HomeIntent
    data class TapSong(val index: Int) : HomeIntent
    data class AddToQueue(val song: Song) : HomeIntent
    data class AddToPlaylist(val song: Song, val playlistName: String) : HomeIntent
    data class CreatePlaylist(val name: String, val song: Song) : HomeIntent
    data class DeleteSong(val song: Song) : HomeIntent
}
