package io.github.uditkarode.able.presentation.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.utils.Shared

@Composable
fun SearchScreen(
    onSendItem: (Song, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.sendItemEvents.collect { event ->
            onSendItem(event.song, event.mode)
        }
    }

    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.onIntent(SearchIntent.DismissError)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF212121))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                query = state.query,
                mode = state.mode,
                onQueryChange = { viewModel.onIntent(SearchIntent.UpdateQuery(it)) },
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.onIntent(SearchIntent.ExecuteSearch)
                },
                onCycleMode = { viewModel.onIntent(SearchIntent.CycleMode) },
            )

            val contentKey = when {
                state.isLoading -> 0
                state.results.isNotEmpty() -> 1
                else -> 2
            }
            AnimatedContent(
                targetState = contentKey,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "search_content",
            ) { key ->
                when (key) {
                    0 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    1 -> ResultsList(
                        results = state.results,
                        mode = state.mode,
                        useYouTubeMusic = state.useYouTubeMusic,
                        onTap = { song -> viewModel.onIntent(SearchIntent.TapResult(song)) },
                        onStream = { song -> viewModel.onIntent(SearchIntent.TapResult(song, "Stream")) },
                        onDownload = { song -> viewModel.onIntent(SearchIntent.TapResult(song, "Download")) },
                    )
                    else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Search for songs, albums, or playlists",
                            color = Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    mode: SearchMode,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCycleMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search…", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2C2C2C),
                unfocusedContainerColor = Color(0xFF2C2C2C),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onCycleMode) {
            Icon(
                imageVector = when (mode) {
                    SearchMode.Music -> Icons.Default.MusicNote
                    SearchMode.Album -> Icons.Default.Album
                    SearchMode.Playlists -> Icons.Default.PlaylistPlay
                },
                contentDescription = "Search mode: ${mode.name}",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ResultsList(
    results: List<Song>,
    mode: SearchMode,
    useYouTubeMusic: Boolean,
    onTap: (Song) -> Unit,
    onStream: (Song) -> Unit,
    onDownload: (Song) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(results, key = { _, song -> song.youtubeLink }) { index, song ->
            if (useYouTubeMusic) {
                YtmResultItem(
                    song = song,
                    mode = mode,
                    onTap = { onTap(song) },
                    onStream = { onStream(song) },
                    onDownload = { onDownload(song) },
                )
            } else {
                YtResultItem(
                    song = song,
                    onTap = { onTap(song) },
                    onStream = { onStream(song) },
                    onDownload = { onDownload(song) },
                )
            }
            if (index < results.lastIndex) {
                HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun YtmResultItem(
    song: Song,
    mode: SearchMode,
    onTap: () -> Unit,
    onStream: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = Shared.getSmallThumbnailUrl(song.ytmThumbnail),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2C2C2C)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = "${mode.name} • ${song.artist}",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }

        ActionButtons(onStream = onStream, onDownload = onDownload)
    }
}

@Composable
private fun YtResultItem(
    song: Song,
    onTap: () -> Unit,
    onStream: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }

        ActionButtons(onStream = onStream, onDownload = onDownload)
    }
}

@Composable
private fun ActionButtons(
    onStream: () -> Unit,
    onDownload: () -> Unit,
) {
    Row {
        IconButton(onClick = onStream, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = "Stream",
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Download",
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
