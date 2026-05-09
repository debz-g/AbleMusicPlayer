package io.github.uditkarode.able.presentation.songlist

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val connection: MusicServiceConnection,
) : ViewModel() {

    private val _state = MutableStateFlow(SongListState(isLoading = true))
    val state: StateFlow<SongListState> = _state.asStateFlow()

    init {
        val name = savedStateHandle.get<String>("name") ?: ""
        _state.update { it.copy(title = name.removeSuffix(".json")) }
        viewModelScope.launch(Dispatchers.IO) {
            val songs = Shared.getSongsFromPlaylistFile(name)
            _state.update { it.copy(songs = songs, isLoading = false) }
        }
    }

    /** Load all available songs on device for the song picker */
    fun loadAllSongs(): List<Song> {
        val songs = Shared.getSongList(Constants.ableSongDir, context)
        if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_AUDIO
            )
        ) {
            songs.addAll(Shared.getLocalSongs(context))
        }
        return songs.sortedBy { it.name.uppercase(Locale.getDefault()) }
    }

    /** Bulk-add songs to the current playlist */
    fun addSongsToPlaylist(songs: List<Song>) {
        val playlistName = _state.value.title + ".json"
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = Shared.getPlaylists().firstOrNull { it.name == playlistName } ?: return@launch
            val existing = Shared.getSongsFromPlaylist(playlist)
            val existingPaths = existing.map { it.filePath }.toSet()
            for (song in songs) {
                if (song.filePath !in existingPaths) {
                    existing.add(song)
                }
            }
            Shared.modifyPlaylist(
                playlistName,
                ArrayList(existing.sortedBy { it.name.uppercase(Locale.getDefault()) })
            )
            val updatedSongs = Shared.getSongsFromPlaylistFile(playlistName)
            _state.update { it.copy(songs = updatedSongs) }
        }
    }

    fun removeFromPlaylist(song: Song) {
        val playlistName = _state.value.title + ".json"
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = Shared.getPlaylists().firstOrNull { it.name == playlistName } ?: return@launch
            Shared.removeFromPlaylist(playlist, song)
            val updatedSongs = Shared.getSongsFromPlaylistFile(playlistName)
            _state.update { it.copy(songs = updatedSongs) }
        }
    }

    fun playQueue(songs: List<Song>, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            ensureServiceBound()
            val svc = connection.boundService ?: return@launch
            withContext(Dispatchers.Main) {
                svc.setQueue(ArrayList(songs))
                svc.setIndex(index)
                if (svc.getShuffle())
                    svc.setShuffleRepeat(shuffle = true, repeat = svc.getRepeat())
            }
        }
    }

    private suspend fun ensureServiceBound() {
        if (!Shared.serviceRunning(MusicService::class.java, context)) {
            withContext(Dispatchers.Main) {
                val intent = Intent(context, MusicService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(intent)
                else
                    context.startService(intent)
                connection.bind(context)
            }
        }
        if (connection.boundService == null) {
            connection.boundServiceFlow.first { it != null }
        }
    }
}
