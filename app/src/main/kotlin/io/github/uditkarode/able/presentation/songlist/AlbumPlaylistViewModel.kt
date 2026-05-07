package io.github.uditkarode.able.presentation.songlist

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.R
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.utils.ChunkedDownloader
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
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject

@HiltViewModel
class AlbumPlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val connection: MusicServiceConnection,
) : ViewModel() {

    private val _state = MutableStateFlow(SongListState(isLoading = true))
    val state: StateFlow<SongListState> = _state.asStateFlow()

    init {
        val name   = savedStateHandle.get<String>("name")   ?: ""
        val artist = savedStateHandle.get<String>("artist") ?: ""
        val art    = savedStateHandle.get<String>("art")    ?: ""
        val link   = savedStateHandle.get<String>("link")   ?: ""
        _state.update { it.copy(title = name, artistName = artist, artUrl = art) }
        loadPlaylist(link)
    }

    private fun loadPlaylist(link: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = YouTube.getPlaylistExtractor(link)
                extractor.fetchPage()
                val songs = mutableListOf<Song>()
                for (item in extractor.initialPage.items) {
                    if (item !is StreamInfoItem) continue
                    var thumbnailUrl = ""
                    for (thumb in item.thumbnails) {
                        if (thumb.url.contains("ytimg")) {
                            thumbnailUrl = "https://i.ytimg.com/vi/${Shared.getIdFromLink(item.url)}/maxresdefault.jpg"
                            break
                        }
                    }
                    if (thumbnailUrl.isBlank() && item.thumbnails.isNotEmpty())
                        thumbnailUrl = item.thumbnails[0].url
                    songs.add(Song(name = item.name, artist = item.uploaderName, youtubeLink = item.url, ytmThumbnail = thumbnailUrl))
                }
                _state.update { it.copy(songs = songs, isLoading = false) }
            } catch (e: Exception) {
                Log.e("AlbumPlaylistVM", "Failed to load playlist: $e")
                _state.update { it.copy(isLoading = false, error = "Failed to load playlist") }
            }
        }
    }

    fun streamSong(songs: List<Song>, index: Int) {
        val song = songs.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ensureServiceBound()
            val service = connection.boundService ?: return@launch

            withContext(Dispatchers.Main) {
                service.setQueue(arrayListOf(Song(name = context.getString(R.string.loading), artist = "")))
                service.setCurrentIndex(0)
                service.showNotif()
            }

            val streamInfo = try {
                StreamInfo.getInfo(song.youtubeLink)
            } catch (e: Exception) {
                Log.e("AlbumPlaylistVM", "Stream resolve failed: $e")
                return@launch
            }

            val stream = streamInfo.audioStreams.maxByOrNull { it.averageBitrate }
                ?: streamInfo.audioStreams.firstOrNull() ?: return@launch
            val url  = stream.content
            val ext  = stream.getFormat()?.suffix ?: return@launch
            val songId = Shared.getIdFromLink(song.youtubeLink)

            if (song.ytmThumbnail.isNotBlank()) {
                try {
                    val bmp = Glide.with(context)
                        .load(Shared.upscaleThumbnailUrl(song.ytmThumbnail))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .submit().get()
                    Shared.saveStreamingAlbumArt(bmp.toBitmap(), songId)
                } catch (e: Exception) {
                    Log.e("AlbumPlaylistVM", "Art save failed: $e")
                }
            }

            if (!Constants.cacheDir.exists()) Constants.cacheDir.mkdirs()
            val tempFile = java.io.File(Constants.cacheDir, "$songId.tmp.$ext")
            try {
                ChunkedDownloader.download(url, tempFile)
            } catch (e: Exception) {
                Log.e("AlbumPlaylistVM", "Stream download failed: $e")
                return@launch
            }

            song.filePath = tempFile.absolutePath
            withContext(Dispatchers.Main) {
                service.setQueue(arrayListOf(song))
                service.setIndex(0)
            }
        }
    }

    fun playAll(songs: List<Song>) {
        if (songs.isEmpty()) return
        streamSong(songs, 0)
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
