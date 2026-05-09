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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val Accent = Color(0xFF5E92F3)

// ── LibraryDetail ─────────────────────────────────────────────────────────────

@Composable
fun LibraryDetailScreen(
    onBack: () -> Unit,
    viewModel: LibraryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SongListContent(
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
    var showSongPicker by rememberSaveable { mutableStateOf(false) }

    if (showSongPicker) {
        SongPickerScreen(
            loadSongs = { viewModel.loadAllSongs() },
            existingPaths = state.songs.map { it.filePath }.toSet(),
            onConfirm = { selected ->
                viewModel.addSongsToPlaylist(selected)
                showSongPicker = false
            },
            onDismiss = { showSongPicker = false },
        )
    } else {
        SongListContent(
            state = state,
            onBack = onBack,
            onPlayAll = { viewModel.playQueue(state.songs, 0) },
            onPlayAt = { index -> viewModel.playQueue(state.songs, index) },
            onRemoveSong = { song -> viewModel.removeFromPlaylist(song) },
            onAddSongs = { showSongPicker = true },
        )
    }
}

// ── Shared song-list UI (Symphony style) ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongListContent(
    state: SongListState,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemoveSong: ((Song) -> Unit)? = null,
    onAddSongs: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ─────────────────────────────────────────────
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (onAddSongs != null) {
                        IconButton(onClick = onAddSongs) {
                            Icon(Icons.Filled.Add, "Add songs", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            // ── Song count + shuffle ────────────────────────────────
            if (state.songs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = songCountLabel(state.songs.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onPlayAll) {
                        Icon(
                            Icons.Filled.Shuffle,
                            "Shuffle play",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // ── Song list ───────────────────────────────────────────
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.songs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (onAddSongs != null) {
                            Spacer(Modifier.height(16.dp))
                            ElevatedButton(
                                onClick = onAddSongs,
                                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Filled.LibraryAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add Songs")
                            }
                        }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(
                        state.songs,
                        key = { _, s -> s.filePath.ifBlank { s.name } },
                    ) { index, song ->
                        SongCard(
                            song = song,
                            onClick = { onPlayAt(index) },
                            onRemove = onRemoveSong?.let { remove -> { remove(song) } },
                        )
                    }
                }
            }
        }

        // ── Play all FAB ────────────────────────────────────────────
        if (state.songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = onPlayAll,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, "Play all")
            }
        }
    }
}

// ── Song Picker (multi-select) ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongPickerScreen(
    loadSongs: () -> List<Song>,
    existingPaths: Set<String> = emptySet(),
    onConfirm: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
) {
    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val selectedPaths = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val songs = withContext(Dispatchers.IO) { loadSongs() }
        allSongs = songs.filter { it.filePath !in existingPaths }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ─────────────────────────────────────────────
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (selectedPaths.isEmpty()) "Select Songs"
                        else "${selectedPaths.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Cancel")
                    }
                },
                actions = {
                    if (selectedPaths.isNotEmpty()) {
                        IconButton(onClick = {
                            val selected = allSongs.filter { it.filePath in selectedPaths }
                            onConfirm(selected)
                        }) {
                            Icon(Icons.Filled.Check, "Done", tint = Accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            // ── Song count ──────────────────────────────────────────
            if (allSongs.isNotEmpty()) {
                Text(
                    text = "${allSongs.size} songs available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                )
            }

            // ── Song list with checkboxes ───────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                allSongs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No songs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(
                        allSongs,
                        key = { _, s -> s.filePath.ifBlank { s.name } },
                    ) { _, song ->
                        val isSelected = song.filePath in selectedPaths
                        PickerSongRow(
                            song = song,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) selectedPaths.remove(song.filePath)
                                else selectedPaths.add(song.filePath)
                            },
                        )
                    }
                }
            }
        }

        // ── Confirm FAB ─────────────────────────────────────────────
        if (selectedPaths.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    val selected = allSongs.filter { it.filePath in selectedPaths }
                    onConfirm(selected)
                },
                containerColor = Accent,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.Check, "Add selected")
            }
        }
    }
}

@Composable
private fun PickerSongRow(
    song: Song,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(if (isSelected) Accent.copy(alpha = 0.1f) else Color.Transparent)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Accent,
                uncheckedColor = Color(0xFF888888),
                checkmarkColor = Color.White,
            ),
        )

        SongArt(song = song)

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
            )
            if (song.artist.isNotBlank()) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Song card (Symphony style) ──────────────────────────────────────────────

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art (45dp, rounded 10dp — Symphony spec)
        SongArt(song = song)

        Spacer(Modifier.width(16.dp))

        // Song info
        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
            )
            if (song.artist.isNotBlank()) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(15.dp))

        // Options menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    "Options",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Play") },
                    onClick = {
                        menuExpanded = false
                        onClick()
                    },
                )
                if (onRemove != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from playlist", color = Color(0xFFEF5350)) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongArt(song: Song) {
    val model: Any = remember(song.filePath, song.albumId) {
        val artFile = File(
            Constants.ableSongDir.absolutePath + "/album_art",
            File(song.filePath).nameWithoutExtension,
        )
        when {
            artFile.exists() -> artFile
            song.albumId != 0L -> ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                song.albumId,
            )
            song.ytmThumbnail.isNotBlank() -> song.ytmThumbnail
            else -> R.drawable.def_albart
        }
    }

    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(45.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

// ── AlbumPlaylist ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPlaylistScreen(
    onBack: () -> Unit,
    viewModel: AlbumPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ─────────────────────────────────────────────
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.artistName.isNotBlank()) {
                            Text(
                                state.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            // ── Album art header ────────────────────────────────────
            if (state.artUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = state.artUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Song count ──────────────────────────────────────────
            if (state.songs.isNotEmpty()) {
                Text(
                    text = songCountLabel(state.songs.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                )
            }

            // ── Song list ───────────────────────────────────────────
            when {
                state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.error != null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                state.songs.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text("No songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(
                        state.songs,
                        key = { _, s -> s.youtubeLink.ifBlank { s.name } },
                    ) { index, song ->
                        StreamSongCard(
                            song = song,
                            onClick = { viewModel.streamSong(state.songs, index) },
                        )
                    }
                }
            }
        }

        // ── Play all FAB ────────────────────────────────────────────
        if (state.songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = { viewModel.playAll(state.songs) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, "Play all")
            }
        }
    }
}

@Composable
private fun StreamSongCard(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.ytmThumbnail.ifBlank { R.drawable.def_albart },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (song.artist.isNotBlank()) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

private fun songCountLabel(count: Int) = if (count == 1) "1 song" else "$count songs"
