package io.github.uditkarode.able.presentation.search

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Collections.singletonList
import javax.inject.Inject

data class SendItemEvent(val song: Song, val mode: String)

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _sendItemEvents = Channel<SendItemEvent>(Channel.BUFFERED)
    val sendItemEvents = _sendItemEvents.receiveAsFlow()

    private val prefs = context.getSharedPreferences("search", Context.MODE_PRIVATE)

    init {
        val savedMode = when (prefs.getString("mode", "Music")) {
            "Album" -> SearchMode.Album
            "Playlists" -> SearchMode.Playlists
            else -> SearchMode.Music
        }
        val useYtMusic = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("source_key", "Youtube Music") == "Youtube Music"
        _state.update { it.copy(mode = savedMode, useYouTubeMusic = useYtMusic) }
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> _state.update { it.copy(query = intent.query) }
            is SearchIntent.ExecuteSearch -> executeSearch()
            is SearchIntent.CycleMode -> cycleMode()
            is SearchIntent.TapResult -> viewModelScope.launch {
                intent.song.ytmThumbnail = Shared.upscaleThumbnailUrl(intent.song.ytmThumbnail)
                _sendItemEvents.send(SendItemEvent(intent.song, intent.forceMode))
            }
            is SearchIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun cycleMode() {
        val next = when (_state.value.mode) {
            SearchMode.Music -> SearchMode.Album
            SearchMode.Album -> SearchMode.Playlists
            SearchMode.Playlists -> SearchMode.Music
        }
        prefs.edit().putString("mode", next.name).apply()
        _state.update { it.copy(mode = next) }
    }

    private fun executeSearch() {
        if (!Shared.isInternetConnected(context)) {
            _state.update { it.copy(error = "No Internet Connection") }
            return
        }

        val rawQuery = _state.value.query
        val (query, useYtm) = resolveQueryAndSource(rawQuery)

        _state.update { it.copy(isLoading = true, results = emptyList(), error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = fetchResults(query, useYtm, _state.value.mode)
                _state.update { it.copy(results = results, isLoading = false) }
            } catch (e: Exception) {
                Log.e("SearchVM", "Search failed: $e")
                _state.update { it.copy(isLoading = false, error = "Search failed") }
            }
        }
    }

    private fun resolveQueryAndSource(rawQuery: String): Pair<String, Boolean> {
        val globalUseYtm = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("source_key", "Youtube Music") == "Youtube Music"
        return when {
            rawQuery.startsWith("!") -> rawQuery.replaceFirst(Regex("^!\\s*"), "") to true
            rawQuery.startsWith("?") -> rawQuery.replaceFirst(Regex("^\\?\\s*"), "") to false
            else -> (rawQuery.ifEmpty { "songs" }) to globalUseYtm
        }
    }

    private fun fetchResults(query: String, useYtm: Boolean, mode: SearchMode): List<Song> {
        if (!useYtm) {
            val extractor = YouTube.getSearchExtractor(
                query, singletonList(YoutubeSearchQueryHandlerFactory.VIDEOS), ""
            )
            extractor.fetchPage()
            return extractor.initialPage.items.mapNotNull { item ->
                runCatching {
                    val ex = item as PlaylistInfoItem
                    Song(name = ex.name, artist = ex.uploaderName, youtubeLink = ex.url,
                        ytmThumbnail = Shared.getBestThumbnail(item.thumbnails, ex.url))
                }.getOrNull()
            }
        }

        return when (mode) {
            SearchMode.Music -> {
                val extractor = YouTube.getSearchExtractor(
                    query, singletonList(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), ""
                )
                extractor.fetchPage()
                extractor.initialPage.items.mapNotNull { item ->
                    runCatching {
                        val ex = item as StreamInfoItem
                        Song(name = ex.name, artist = ex.uploaderName, youtubeLink = ex.url,
                            ytmThumbnail = Shared.getBestThumbnail(item.thumbnails, ex.url))
                    }.getOrNull()
                }
            }

            SearchMode.Album -> {
                val extractor = YouTube.getSearchExtractor(
                    query, singletonList(YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS), ""
                )
                extractor.fetchPage()
                extractor.initialPage.items.mapNotNull { item ->
                    runCatching {
                        val ex = item as PlaylistInfoItem
                        Song(name = ex.name, artist = ex.uploaderName, youtubeLink = ex.url,
                            ytmThumbnail = Shared.getBestThumbnail(item.thumbnails, ex.url))
                    }.getOrNull()
                }
            }

            SearchMode.Playlists -> {
                val extractor = if (query.startsWith("https://"))
                    YouTube.getPlaylistExtractor(query)
                else
                    YouTube.getSearchExtractor(
                        query, singletonList(YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS), ""
                    )
                extractor.fetchPage()
                extractor.initialPage.items.mapNotNull { item ->
                    runCatching {
                        val ex = item as PlaylistInfoItem
                        Song(name = ex.name, artist = ex.uploaderName, youtubeLink = ex.url,
                            ytmThumbnail = Shared.getBestThumbnail(item.thumbnails, ex.url))
                    }.getOrNull()
                }
            }
        }
    }
}
