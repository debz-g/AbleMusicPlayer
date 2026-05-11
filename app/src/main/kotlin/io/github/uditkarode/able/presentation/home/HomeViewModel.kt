package io.github.uditkarode.able.presentation.home

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.services.DownloadService
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connection: MusicServiceConnection,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 30
    }

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        observeCurrentSong()
        loadSongs()
        DownloadService.onDownloadComplete = { reloadSongs() }
    }

    override fun onCleared() {
        super.onCleared()
        if (DownloadService.onDownloadComplete != null) {
            DownloadService.onDownloadComplete = null
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Load -> loadSongs()
            is HomeIntent.TapSong -> tapSong(intent.index)
            is HomeIntent.AddToQueue -> addToQueue(intent.song)
            is HomeIntent.AddToPlaylist -> addToPlaylist(intent.song, intent.playlistName)
            is HomeIntent.CreatePlaylist -> createPlaylistAndAdd(intent.name, intent.song)
            is HomeIntent.DeleteSong -> deleteSong(intent.song)
        }
    }

    fun checkPendingDownload() {
        if (DownloadService.downloadCompletedSinceLastCheck) {
            DownloadService.downloadCompletedSinceLastCheck = false
            reloadSongs()
        }
    }

    private fun observeCurrentSong() {
        viewModelScope.launch {
            connection.state.collect { playback ->
                val path = playback.currentSong?.filePath ?: ""
                _state.update { it.copy(currentSongPath = path) }
            }
        }
    }

    private fun loadSongs() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val songs = buildSongList()
            val playlists = Shared.getPlaylists().map { it.name.removeSuffix(".json") }
            val recentSongs = filterRecentSongs(songs)
            val firstPage = songs.take(PAGE_SIZE)
            _state.update {
                it.copy(
                    songs = songs,
                    displayedSongs = firstPage,
                    recentSongs = recentSongs,
                    playlists = playlists,
                    isLoading = false,
                    hasMoreSongs = songs.size > PAGE_SIZE,
                )
            }
        }
    }

    private fun reloadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = buildSongList()
            val playlists = Shared.getPlaylists().map { it.name.removeSuffix(".json") }
            val recentSongs = filterRecentSongs(songs)
            val currentDisplayCount = _state.value.displayedSongs.size.coerceAtLeast(PAGE_SIZE)
            val displayed = songs.take(currentDisplayCount)
            _state.update {
                it.copy(
                    songs = songs,
                    displayedSongs = displayed,
                    recentSongs = recentSongs,
                    playlists = playlists,
                    hasMoreSongs = displayed.size < songs.size,
                )
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMoreSongs) return
        val nextCount = (current.displayedSongs.size + PAGE_SIZE).coerceAtMost(current.songs.size)
        _state.update {
            it.copy(
                displayedSongs = it.songs.take(nextCount),
                hasMoreSongs = nextCount < it.songs.size,
            )
        }
    }

    private fun filterRecentSongs(songs: List<Song>): List<Song> {
        val fourDaysAgo = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000)
        return songs.filter { song ->
            if (song.filePath.isBlank()) return@filter false
            try {
                val lastModified = java.io.File(song.filePath).lastModified()
                lastModified >= fourDaysAgo
            } catch (_: Exception) {
                false
            }
        }.sortedByDescending { java.io.File(it.filePath).lastModified() }
    }

    private fun buildSongList(): List<Song> {
        val songs = Shared.getSongList(Constants.ableSongDir, context)
        if (Shared.isLocalMusicEnabled(context) &&
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_AUDIO
            )
        ) {
            songs.addAll(Shared.getLocalSongs(context))
        }
        return songs.sortedBy { it.name.uppercase(Locale.getDefault()) }
    }

    private fun tapSong(index: Int) {
        val songs = _state.value.songs
        if (index < 0 || index >= songs.size) return

        if (!Shared.serviceRunning(MusicService::class.java, context)) {
            val intent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        viewModelScope.launch(Dispatchers.Main) {
            val service = connection.boundService
                ?: connection.boundServiceFlow.filterNotNull().first()

            service.setQueue(ArrayList(songs))
            service.setIndex(index)
            if (service.getShuffle()) {
                service.setShuffleRepeat(shuffle = true, repeat = service.getRepeat())
            }
        }
    }

    private fun addToQueue(song: Song) {
        connection.boundService?.addToQueue(song)
    }

    private fun addToPlaylist(song: Song, playlistName: String) {
        val playlist = Shared.getPlaylists().firstOrNull {
            it.name.removeSuffix(".json") == playlistName
        } ?: return
        Shared.addToPlaylist(playlist, song, context)
    }

    private fun createPlaylistAndAdd(name: String, song: Song) {
        Shared.createPlaylist(name, context)
        val playlist = Shared.getPlaylists().firstOrNull { it.name == "$name.json" } ?: return
        Shared.addToPlaylist(playlist, song, context)
        val playlists = Shared.getPlaylists().map { it.name.removeSuffix(".json") }
        _state.update { it.copy(playlists = playlists) }
    }

    private fun deleteSong(song: Song) {
        try {
            val file = File(song.filePath)
            val art = File(Constants.ableSongDir.absolutePath + "/album_art", file.nameWithoutExtension)
            file.delete()
            art.delete()
            MediaScannerConnection.scanFile(context, arrayOf(song.filePath), null, null)
            _state.update {
                it.copy(
                    songs = it.songs.filter { s -> s.filePath != song.filePath },
                    displayedSongs = it.displayedSongs.filter { s -> s.filePath != song.filePath },
                )
            }
        } catch (e: Exception) {
            Log.e("HomeVM", "Delete failed: $e")
        }
    }
}
