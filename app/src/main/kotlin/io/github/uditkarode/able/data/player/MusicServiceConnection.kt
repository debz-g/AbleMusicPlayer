package io.github.uditkarode.able.data.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.model.song.SongState
import io.github.uditkarode.able.services.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that bridges the legacy [MusicService] observer/callback pattern to
 * a [StateFlow] that ViewModels can collect. Also manages the ServiceConnection
 * lifecycle so every screen that injects this class shares one connection.
 */
@Singleton
class MusicServiceConnection @Inject constructor() :
    MusicService.MusicClient, ServiceConnection {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _boundServiceFlow = MutableStateFlow<MusicService?>(null)
    val boundServiceFlow: StateFlow<MusicService?> = _boundServiceFlow.asStateFlow()

    var boundService: MusicService? = null
        private set

    private var appContext: Context? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun bind(context: Context) {
        appContext = context.applicationContext
        MusicService.registerClient(this)
        tryBind()
    }

    fun unbind(context: Context) {
        MusicService.unregisterClient(this)
        if (boundService != null) {
            try { context.applicationContext.unbindService(this) } catch (_: Exception) {}
            boundService = null
            _boundServiceFlow.value = null
        }
    }

    private fun tryBind() {
        val ctx = appContext ?: return
        if (MusicService.isServiceRunning && boundService == null) {
            try {
                ctx.bindService(
                    Intent(ctx, MusicService::class.java),
                    this,
                    Context.BIND_AUTO_CREATE
                )
            } catch (e: Exception) {
                Log.e("MSConnection", "bindService failed: $e")
            }
        }
    }

    // ── ServiceConnection ─────────────────────────────────────────────────────

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = (binder as MusicService.MusicBinder).getService()
        boundService = service
        _boundServiceFlow.value = service
        // Eagerly sync the full state so observers get a consistent snapshot
        _state.update {
            PlaybackState(
                queue        = service.getPlayQueue().toList(),
                currentIndex = service.getCurrentIndex(),
                songState    = if (service.getMediaPlayer().isPlaying) SongState.playing
                               else SongState.paused,
                isLoading    = MusicService.isLoading,
                isShuffling  = service.getShuffle(),
                isRepeating  = service.getRepeat(),
                durationMs   = service.getMediaPlayer().duration.coerceAtLeast(0),
                positionMs   = service.getMediaPlayer().currentPosition.coerceAtLeast(0),
            )
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        boundService = null
        _boundServiceFlow.value = null
    }

    // ── MusicClient callbacks — convert to StateFlow updates ──────────────────

    override fun playStateChanged(state: SongState) {
        _state.update { it.copy(songState = state) }
    }

    override fun songChanged() {
        val service = boundService ?: return
        _state.update {
            it.copy(
                queue        = service.getPlayQueue().toList(),
                currentIndex = service.getCurrentIndex(),
                positionMs   = 0,
                durationMs   = 0,
            )
        }
    }

    override fun durationChanged(duration: Int) {
        _state.update { it.copy(durationMs = duration) }
    }

    override fun isExiting() {
        _state.update { it.copy(songState = SongState.paused) }
    }

    override fun queueChanged(arrayList: ArrayList<Song>) {
        _state.update { it.copy(queue = arrayList.toList()) }
    }

    override fun shuffleRepeatChanged(onShuffle: Boolean, onRepeat: Boolean) {
        _state.update { it.copy(isShuffling = onShuffle, isRepeating = onRepeat) }
    }

    override fun indexChanged(index: Int) {
        _state.update { it.copy(currentIndex = index) }
    }

    override fun isLoading(doLoad: Boolean) {
        _state.update { it.copy(isLoading = doLoad) }
    }

    override fun spotifyImportChange(starting: Boolean) {
        _state.update { it.copy(isSpotifyImporting = starting) }
    }
    override fun serviceStarted() {
        tryBind()
    }
}
