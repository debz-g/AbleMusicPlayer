package io.github.uditkarode.able.presentation.search

import io.github.uditkarode.able.model.song.Song

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data object ExecuteSearch : SearchIntent
    data object CycleMode : SearchIntent
    data class TapResult(val song: Song, val forceMode: String = "") : SearchIntent
    data object DismissError : SearchIntent
}
