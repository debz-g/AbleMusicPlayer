package io.github.uditkarode.able.presentation.songlist

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.activities.LibraryDetail
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val connection: MusicServiceConnection,
) : ViewModel() {

    private val _state = MutableStateFlow(SongListState())
    val state: StateFlow<SongListState> = _state.asStateFlow()

    init {
        val title = savedStateHandle.get<String>("title") ?: ""
        val songs = LibraryDetail.pendingSongs ?: arrayListOf()
        LibraryDetail.pendingSongs = null
        _state.update { it.copy(title = title, songs = songs) }
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
