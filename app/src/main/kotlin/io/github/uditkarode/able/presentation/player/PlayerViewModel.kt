package io.github.uditkarode.able.presentation.player

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.model.song.SongState
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: MusicServiceConnection,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var positionJob: Job? = null
    private var lastTrackedIndex = -1

    init {
        viewModelScope.launch {
            connection.state.collect { playback ->
                _state.update { current ->
                    current.copy(
                        currentSong  = playback.currentSong,
                        queue        = playback.queue,
                        currentIndex = playback.currentIndex,
                        songState    = playback.songState,
                        isLoading    = playback.isLoading,
                        isShuffling  = playback.isShuffling,
                        isRepeating  = playback.isRepeating,
                        durationMs   = playback.durationMs,
                    )
                }

                // Song changed — reset art + transient overrides and reload
                val changedSong = playback.currentSong
                if (playback.currentIndex != lastTrackedIndex && changedSong != null) {
                    lastTrackedIndex = playback.currentIndex
                    _state.update { it.copy(albumArt = null, editedSongName = null, editedArtistName = null) }
                    loadAlbumArt(changedSong)
                }

                // Drive the position ticker
                if (playback.songState == SongState.playing) {
                    startPositionTicker()
                } else {
                    positionJob?.cancel()
                }
            }
        }
    }

    // ── Intent dispatch ───────────────────────────────────────────────────────

    fun onIntent(intent: PlayerIntent) {
        val service = connection.boundService
        when (intent) {
            is PlayerIntent.PlayPause -> viewModelScope.launch(Dispatchers.Default) {
                val current = _state.value.songState
                service?.setPlayPause(if (current == SongState.playing) SongState.paused else SongState.playing)
            }
            is PlayerIntent.Next -> service?.setNextPrevious(next = true)
            is PlayerIntent.Previous -> service?.setNextPrevious(next = false)
            is PlayerIntent.SeekTo -> viewModelScope.launch(Dispatchers.Default) {
                service?.seekTo(intent.positionMs)
                _state.update { it.copy(positionMs = intent.positionMs) }
            }
            is PlayerIntent.ToggleShuffle -> {
                val s = _state.value
                service?.setShuffleRepeat(shuffle = !s.isShuffling, repeat = s.isRepeating)
            }
            is PlayerIntent.ToggleRepeat -> {
                val s = _state.value
                service?.setShuffleRepeat(shuffle = s.isShuffling, repeat = !s.isRepeating)
            }
            is PlayerIntent.FetchAlbumArt -> {
                val song = _state.value.currentSong ?: return
                viewModelScope.launch { loadAlbumArt(song, intent.customName, intent.forceDeezer) }
            }
            is PlayerIntent.EditSongTitle -> editSongTitle(intent.newTitle)
            is PlayerIntent.EditArtistName -> editArtistName(intent.newArtist)
        }
    }

    // ── Position ticker ───────────────────────────────────────────────────────

    private fun startPositionTicker() {
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                val pos = connection.boundService?.getMediaPlayer()?.currentPosition ?: 0
                _state.update { it.copy(positionMs = pos.coerceAtLeast(0)) }
                delay(1000)
            }
        }
    }

    // ── Album art loading ─────────────────────────────────────────────────────

    private fun loadAlbumArt(
        song: Song,
        customName: String? = null,
        forceDeezer: Boolean = false,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val albumArtFile = File(
                Constants.ableSongDir.absolutePath + "/album_art",
                File(song.filePath).nameWithoutExtension
            )
            val cacheFile = File(
                Constants.ableSongDir.absolutePath + "/cache",
                "sCache" + Shared.getIdFromLink(song.youtubeLink)
            )

            var bitmap: Bitmap? = null

            // (1) Local song: read album art from MediaStore
            if (song.isLocal && !forceDeezer) {
                bitmap = runCatching {
                    val artUri = Uri.parse("content://media/external/audio/albumart")
                    Glide.with(context)
                        .load(ContentUris.withAppendedId(artUri, song.albumId))
                        .signature(ObjectKey("player"))
                        .submit()
                        .get()
                        .toBitmap()
                }.getOrNull()
            }

            // (2) Non-local song: read from disk (album_art/ or cache/)
            if (bitmap == null && !song.isLocal && !forceDeezer) {
                val src = when {
                    albumArtFile.exists() -> albumArtFile
                    cacheFile.exists()    -> cacheFile
                    else                  -> null
                }
                if (src != null) {
                    bitmap = runCatching { BitmapFactory.decodeFile(src.absolutePath) }.getOrNull()
                }
            }

            // (3) Deezer API fallback
            if (bitmap == null && Shared.isInternetConnected(context)) {
                bitmap = fetchFromDeezer(song, customName, albumArtFile)
            }

            if (bitmap != null) {
                val colors = extractPaletteColors(bitmap)
                _state.update {
                    it.copy(
                        albumArt      = bitmap,
                        seekbarColor  = colors.first,
                        controlsColor = colors.second,
                    )
                }
            }
            // null bitmap → keep placeholder, colors stay at defaults
        }
    }

    private suspend fun fetchFromDeezer(
        song: Song,
        customName: String?,
        saveTarget: File,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val searchTerm = customName ?: song.name
        val request = Request.Builder()
            .url("${Constants.DEEZER_API}$searchTerm")
            .get()
            .addHeader("x-rapidapi-host", "deezerdevs-deezer.p.rapidapi.com")
            .addHeader("x-rapidapi-key", Constants.RAPID_API_KEY)
            .apply { if (customName == null) cacheControl(CacheControl.FORCE_NETWORK) }
            .build()

        runCatching {
            val response = OkHttpClient().newCall(request).execute()
            val json = JSONObject(response.body.string())
                .getJSONArray("data").getJSONObject(0).getJSONObject("album")
            val imgUrl   = json.getString("cover_xl")
            val albumTitle = json.getString("title")

            val bmp = Glide.with(context)
                .load(imgUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit()
                .get()
                .toBitmap()

            if (saveTarget.exists()) saveTarget.delete()
            Shared.saveAlbumArtToDisk(bmp, saveTarget)
            Shared.addThumbnails(song.filePath, albumTitle, context)

            // Refresh notification art
            val service = connection.boundService
            if (service != null) {
                val isPlaying = service.getMediaPlayer().isPlaying
                service.showNotification(
                    service.generateAction(
                        if (isPlaying) io.github.uditkarode.able.R.drawable.notif_pause
                        else            io.github.uditkarode.able.R.drawable.notif_play,
                        if (isPlaying) "Pause" else "Play",
                        if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY",
                    ), bmp
                )
            }

            bmp
        }.onFailure { Log.e("PlayerVM", "Deezer fetch failed: $it") }.getOrNull()
    }

    private fun extractPaletteColors(bitmap: Bitmap): Pair<Color, Color> {
        var seekbarColor = Color(0xFF5E92F3)
        var controlsColor = Color.White

        Palette.from(bitmap).generate { palette ->
            val dominant = palette?.getDominantColor(0x002171) ?: 0x002171
            val accent   = palette?.getLightMutedColor(dominant) ?: dominant
            if (accent != 0 && (accent and 0xff000000.toInt()) ushr 24 != 0) {
                seekbarColor  = Color(accent)
                controlsColor = Color.White
            }
        }
        return seekbarColor to controlsColor
    }

    // ── FFmpeg metadata editing ───────────────────────────────────────────────

    private fun editSongTitle(newTitle: String) {
        val song = _state.value.currentSong ?: return
        if (!song.filePath.contains("emulated/0/")) return
        viewModelScope.launch(Dispatchers.IO) {
            val ext = song.filePath.substringAfterLast(".")
            val tempPath = "${song.filePath}.new.$ext"
            val session = FFmpegKit.execute(
                "-i \"${song.filePath}\" -y -c copy " +
                "-metadata title=\"$newTitle\" " +
                "-metadata artist=\"${_state.value.displayArtistName}\" " +
                "\"$tempPath\""
            )
            when {
                ReturnCode.isSuccess(session.returnCode) -> {
                    File(song.filePath).delete()
                    File(tempPath).renameTo(File(song.filePath))
                    _state.update { it.copy(editedSongName = newTitle) }
                    if (song.isLocal) scanAndUpdateNotification(song.filePath, newTitle, _state.value.displayArtistName)
                }
                else -> Log.e("PlayerVM", "FFmpeg title edit failed rc=${session.returnCode}")
            }
        }
    }

    private fun editArtistName(newArtist: String) {
        val song = _state.value.currentSong ?: return
        if (!song.filePath.contains("emulated/0/")) return
        viewModelScope.launch(Dispatchers.IO) {
            val ext = song.filePath.substringAfterLast(".")
            val tempPath = "${song.filePath}.new.$ext"
            val session = FFmpegKit.execute(
                "-i \"${song.filePath}\" -c copy " +
                "-metadata title=\"${_state.value.displaySongName}\" " +
                "-metadata artist=\"$newArtist\" " +
                "\"$tempPath\""
            )
            when {
                ReturnCode.isSuccess(session.returnCode) -> {
                    File(song.filePath).delete()
                    File(tempPath).renameTo(File(song.filePath))
                    _state.update { it.copy(editedArtistName = newArtist) }
                    if (song.isLocal) scanAndUpdateNotification(song.filePath, _state.value.displaySongName, newArtist)
                }
                else -> Log.e("PlayerVM", "FFmpeg artist edit failed rc=${session.returnCode}")
            }
        }
    }

    private fun scanAndUpdateNotification(filePath: String, name: String, artist: String) {
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null) { _, _ ->
            val service = connection.boundService ?: return@scanFile
            val isPlaying = service.getMediaPlayer().isPlaying
            service.showNotification(
                service.generateAction(
                    if (isPlaying) io.github.uditkarode.able.R.drawable.notif_pause
                    else            io.github.uditkarode.able.R.drawable.notif_play,
                    if (isPlaying) "Pause" else "Play",
                    if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY",
                ),
                nameOverride   = name,
                artistOverride = artist,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionJob?.cancel()
    }
}
