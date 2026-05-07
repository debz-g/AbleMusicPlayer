package io.github.uditkarode.able.presentation.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.R
import io.github.uditkarode.able.services.DownloadService
import io.github.uditkarode.able.utils.SpotifyImport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _state.value = buildState()
                delay(1000L)
            }
        }
    }

    private fun buildState(): DownloadsState {
        val items = mutableListOf<DownloadItem>()

        DownloadService.currentDownload?.let { current ->
            items.add(
                DownloadItem(
                    name = current.name,
                    artist = current.artist,
                    status = DownloadService.currentStatus,
                    isActive = true,
                )
            )
        }
        for (song in DownloadService.pendingQueue) {
            items.add(
                DownloadItem(
                    name = song.name,
                    artist = song.artist,
                    status = context.getString(R.string.downloads_waiting),
                    isActive = false,
                )
            )
        }

        val spotify = if (SpotifyImport.isImporting && SpotifyImport.totalTracks > 0) {
            SpotifyProgress(
                trackName = SpotifyImport.currentTrackName,
                trackIndex = SpotifyImport.currentTrackIndex,
                totalTracks = SpotifyImport.totalTracks,
                trackStatus = SpotifyImport.currentTrackStatus,
            )
        } else null

        return DownloadsState(items = items, spotify = spotify)
    }
}
