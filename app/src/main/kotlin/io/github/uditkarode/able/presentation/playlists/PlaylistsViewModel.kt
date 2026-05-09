package io.github.uditkarode.able.presentation.playlists

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.Playlist
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.services.SpotifyImportService
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connection: MusicServiceConnection,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistsState())
    val state: StateFlow<PlaylistsState> = _state.asStateFlow()

    init {
        load()
        connection.state
            .map { it.isSpotifyImporting }
            .distinctUntilChanged()
            .onEach { importing ->
                _state.update { it.copy(isImporting = importing) }
                if (!importing) load()
            }
            .launchIn(viewModelScope)
    }

    fun reload() = load()

    fun createPlaylist(name: String) {
        Shared.createPlaylist(name, context)
        load()
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            File(Constants.playlistFolder.absolutePath + "/" + playlist.name).delete()
            load()
        }
    }

    fun removeSongFromPlaylist(playlist: Playlist, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            Shared.removeFromPlaylist(playlist, song)
            load()
        }
    }

    fun startSpotifyImport(playlistId: String) {
        val intent = Intent(context, SpotifyImportService::class.java)
            .putExtra("inputId", playlistId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelSpotifyImport() {
        MusicService.registeredClients.forEach { it.spotifyImportChange(false) }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = Shared.getPlaylists()
            _state.update { it.copy(playlists = playlists) }
        }
    }
}
