package io.github.uditkarode.able.presentation.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState(isLoading = true))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init { load() }

    fun reload() = load()

    fun toggleMode() {
        val next = if (_state.value.mode == LibraryMode.ARTISTS) LibraryMode.ALBUMS else LibraryMode.ARTISTS
        _state.update { it.copy(mode = next) }
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = Shared.getSongList(Constants.ableSongDir, context)
            if (Shared.isLocalMusicEnabled(context) &&
                android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_AUDIO
                )
            ) {
                songs.addAll(Shared.getLocalSongs(context))
            }

            val mode = _state.value.mode
            val groups = when (mode) {
                LibraryMode.ARTISTS -> songs
                    .groupBy { it.artist.ifBlank { "Unknown" } }
                    .map { (k, v) -> LibraryGroup(k, v.size) }
                    .sortedBy { it.label.uppercase(Locale.getDefault()) }

                LibraryMode.ALBUMS -> songs
                    .groupBy { it.album.ifBlank { "Unknown" } }
                    .map { (k, v) -> LibraryGroup(k, v.size) }
                    .sortedBy { it.label.uppercase(Locale.getDefault()) }
            }

            _state.update { it.copy(groups = groups, songs = songs, isLoading = false) }
        }
    }
}
