package io.github.uditkarode.able.presentation.downloads

data class DownloadItem(
    val name: String,
    val artist: String,
    val status: String,
    val isActive: Boolean,
)

data class SpotifyProgress(
    val trackName: String,
    val trackIndex: Int,
    val totalTracks: Int,
    val trackStatus: String,
)

data class DownloadsState(
    val items: List<DownloadItem> = emptyList(),
    val spotify: SpotifyProgress? = null,
)
