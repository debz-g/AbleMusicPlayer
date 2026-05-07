package io.github.uditkarode.able.presentation.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.R
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.model.song.SongState
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.utils.ChunkedDownloader
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import javax.inject.Inject

data class MiniPlayerState(
    val isVisible: Boolean = false,
    val songName: String = "",
    val artistName: String = "",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val connection: MusicServiceConnection,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val miniPlayerState = connection.state
        .map { playback ->
            MiniPlayerState(
                isVisible  = playback.currentSong != null,
                songName   = playback.currentSong?.name ?: "",
                artistName = playback.currentSong?.artist ?: "",
                isPlaying  = playback.songState == SongState.playing,
                isLoading  = playback.isLoading,
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, MiniPlayerState())

    fun playPause() {
        val playback = connection.state.value
        connection.boundService?.setPlayPause(
            if (playback.songState == SongState.playing) SongState.paused else SongState.playing
        )
    }

    fun streamAudio(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!Shared.serviceRunning(MusicService::class.java, context)) {
                val intent = Intent(context, MusicService::class.java)
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    connection.bind(context)
                }
            }

            if (connection.boundService == null) {
                connection.boundServiceFlow.first { it != null }
            }
            val service = connection.boundService ?: return@launch

            withContext(Dispatchers.Main) {
                service.setQueue(arrayListOf(Song(name = context.getString(R.string.loading), artist = "")))
                service.setCurrentIndex(0)
                service.showNotif()
            }

            val streamInfo = try {
                StreamInfo.getInfo(song.youtubeLink)
            } catch (e: Exception) {
                Log.e("MainVM", "Stream resolve failed: $e")
                return@launch
            }

            val stream = streamInfo.audioStreams.maxByOrNull { it.averageBitrate }
                ?: streamInfo.audioStreams.firstOrNull() ?: return@launch
            val url = stream.content
            val ext = stream.getFormat()?.suffix ?: return@launch
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
                    Log.e("MainVM", "Art save failed: $e")
                }
            }

            if (!Constants.cacheDir.exists()) Constants.cacheDir.mkdirs()
            val tempFile = File(Constants.cacheDir, "$songId.tmp.$ext")
            try {
                ChunkedDownloader.download(url, tempFile)
            } catch (e: Exception) {
                Log.e("MainVM", "Stream download failed: $e")
                return@launch
            }

            song.filePath = tempFile.absolutePath
            withContext(Dispatchers.Main) {
                service.setQueue(arrayListOf(song))
                service.setIndex(0)
            }
        }
    }
}
