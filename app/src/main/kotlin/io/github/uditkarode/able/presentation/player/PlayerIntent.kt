package io.github.uditkarode.able.presentation.player

sealed class PlayerIntent {
    object PlayPause : PlayerIntent()
    object Next : PlayerIntent()
    object Previous : PlayerIntent()
    data class SeekTo(val positionMs: Int) : PlayerIntent()
    object ToggleShuffle : PlayerIntent()
    object ToggleRepeat : PlayerIntent()
    /** Re-fetch album art, optionally forcing a Deezer lookup with a custom search name. */
    data class FetchAlbumArt(
        val customName: String? = null,
        val forceDeezer: Boolean = false,
    ) : PlayerIntent()
    data class EditSongTitle(val newTitle: String) : PlayerIntent()
    data class EditArtistName(val newArtist: String) : PlayerIntent()
}
