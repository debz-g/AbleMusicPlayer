package io.github.uditkarode.able.presentation.songlist

import android.content.ContentUris
import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.utils.Constants
import java.io.File

private val Bg      = Color(0xFF212121)
private val Surface = Color(0xFF2C2C2C)
private val White   = Color(0xFFFBFBFB)
private val Gray    = Color(0xFF888888)
private val Accent  = Color(0xFF5E92F3)

// ── LibraryDetail ─────────────────────────────────────────────────────────────

@Composable
fun LibraryDetailScreen(
    onBack: () -> Unit,
    viewModel: LibraryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LocalSongListContent(
        state = state,
        onBack = onBack,
        onPlayAll = { viewModel.playQueue(state.songs, 0) },
        onPlayAt = { index -> viewModel.playQueue(state.songs, index) },
    )
}

// ── LocalPlaylist ─────────────────────────────────────────────────────────────

@Composable
fun LocalPlaylistScreen(
    onBack: () -> Unit,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LocalSongListContent(
        state = state,
        onBack = onBack,
        onPlayAll = { viewModel.playQueue(state.songs, 0) },
        onPlayAt = { index -> viewModel.playQueue(state.songs, index) },
    )
}

// ── Shared local-song UI ──────────────────────────────────────────────────────

@Composable
private fun LocalSongListContent(
    state: SongListState,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayAt: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            SongListHeader(
                title = state.title,
                subtitle = songCountLabel(state.songs.size),
                onBack = onBack,
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = White)
                }
                state.songs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No songs", color = Gray, fontSize = 14.sp)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(state.songs, key = { _, s -> s.filePath.ifBlank { s.name } }) { index, song ->
                        LocalSongRow(song = song, onClick = { onPlayAt(index) })
                        HorizontalDivider(color = Surface, thickness = 0.5.dp)
                    }
                }
            }
        }

        if (state.songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = onPlayAll,
                containerColor = Accent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_black_24dp),
                    contentDescription = "Play all",
                    tint = White,
                )
            }
        }
    }
}

@Composable
private fun LocalSongRow(song: Song, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artFile = File(
            Constants.ableSongDir.absolutePath + "/album_art",
            File(song.filePath).nameWithoutExtension,
        )
        val imageModel: Any = when {
            artFile.exists() -> artFile
            song.albumId.toString() != "-1" -> ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), song.albumId
            )
            else -> R.drawable.def_albart
        }
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist.ifBlank { "Unknown" },
                color = Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── AlbumPlaylist ─────────────────────────────────────────────────────────────

@Composable
fun AlbumPlaylistScreen(
    onBack: () -> Unit,
    viewModel: AlbumPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            AlbumPlaylistHeader(
                title = state.title,
                artist = state.artistName,
                artUrl = state.artUrl,
                songCount = state.songs.size,
                onBack = onBack,
            )

            when {
                state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = White)
                }
                state.error != null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text(state.error!!, color = Gray, fontSize = 14.sp)
                }
                state.songs.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text("No songs", color = Gray, fontSize = 14.sp)
                }
                else -> LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(state.songs, key = { _, s -> s.youtubeLink.ifBlank { s.name } }) { index, song ->
                        StreamSongRow(song = song, onClick = { viewModel.streamSong(state.songs, index) })
                        HorizontalDivider(color = Surface, thickness = 0.5.dp)
                    }
                }
            }
        }

        if (state.songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = { viewModel.playAll(state.songs) },
                containerColor = Accent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_black_24dp),
                    contentDescription = "Play all",
                    tint = White,
                )
            }
        }
    }
}

@Composable
private fun AlbumPlaylistHeader(
    title: String,
    artist: String,
    artUrl: String,
    songCount: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(bottom = 16.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_home_black_24dp),
                contentDescription = "Back",
                tint = Gray,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (artUrl.isNotBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg),
                )
                Spacer(Modifier.width(14.dp))
            }
            Column {
                Text(title, color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (artist.isNotBlank())
                    Text(artist, color = Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (songCount > 0)
                    Text(songCountLabel(songCount), color = Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StreamSongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.ytmThumbnail.ifBlank { R.drawable.def_albart },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist.ifBlank { "Unknown" },
                color = Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SongListHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_home_black_24dp),
                contentDescription = "Back",
                tint = Gray,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Gray, fontSize = 12.sp)
        }
    }
}

private fun songCountLabel(count: Int) = if (count == 1) "1 song" else "$count songs"
