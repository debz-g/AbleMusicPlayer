package io.github.uditkarode.able.presentation.playlists

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.Playlist
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import java.io.File


private val Accent = Color(0xFF5E92F3)
private val SpotifyGreen = Color(0xFF1DB954)
private val YouTubeRed = Color(0xFFFF0000)

private enum class ImportSource { SPOTIFY, YOUTUBE }

@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reload whenever this screen becomes visible (e.g. after adding/removing songs)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            viewModel.reload()
        }
    }

    var longPressedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var newPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var showSourcePicker by rememberSaveable { mutableStateOf(false) }
    var importSource by remember { mutableStateOf<ImportSource?>(null) }
    var importUrlInput by rememberSaveable { mutableStateOf("") }
    var importUrlError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Control bar (New Playlist + Import) ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ElevatedButton(
                onClick = { newPlaylistDialogOpen = true },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                    containerColor = Accent,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Playlist")
            }

            ElevatedButton(
                onClick = {
                    if (state.isImporting) {
                        viewModel.cancelImport()
                    } else {
                        showSourcePicker = true
                    }
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                    containerColor = if (state.isImporting) MaterialTheme.colorScheme.error else Accent,
                    contentColor = Color.White,
                ),
            ) {
                if (state.isImporting) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancle_action),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel Import")
                } else {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Playlist")
                }
            }
        }

        // ── Playlist count ──────────────────────────────────────────────
        if (state.playlists.isNotEmpty()) {
            Text(
                text = "${state.playlists.size} Playlist${if (state.playlists.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
        }

        // ── Grid or empty state ─────────────────────────────────────────
        if (state.playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.QueueMusic,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No playlists yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.playlists, key = { it.name }) { playlist ->
                    PlaylistTile(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist.name) },
                        onLongClick = { longPressedPlaylist = playlist },
                    )
                }
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────

    if (newPlaylistDialogOpen) {
        CreatePlaylistDialog(
            onCreate = { name ->
                viewModel.createPlaylist(name)
                newPlaylistDialogOpen = false
            },
            onDismiss = { newPlaylistDialogOpen = false },
        )
    }

    // Step 1: Source picker
    if (showSourcePicker) {
        ImportSourcePickerDialog(
            onPickSpotify = {
                showSourcePicker = false
                importSource = ImportSource.SPOTIFY
                importUrlInput = ""
                importUrlError = null
            },
            onPickYouTube = {
                showSourcePicker = false
                importSource = ImportSource.YOUTUBE
                importUrlInput = ""
                importUrlError = null
            },
            onDismiss = { showSourcePicker = false },
        )
    }

    // Step 2: URL input dialog for chosen source
    importSource?.let { source ->
        ImportUrlDialog(
            source = source,
            input = importUrlInput,
            errorMessage = importUrlError,
            onInputChange = {
                importUrlInput = it
                importUrlError = null
            },
            onDismiss = { importSource = null },
            onConfirm = {
                when (source) {
                    ImportSource.SPOTIFY -> {
                        // Reject if it looks like a YouTube URL
                        if (looksLikeYouTubeUrl(importUrlInput)) {
                            importUrlError = "That looks like a YouTube URL. Use the YouTube Music option instead."
                        } else {
                            val id = parseSpotifyPlaylistId(importUrlInput)
                            if (id == null) {
                                importUrlError = "Invalid Spotify playlist URL"
                            } else {
                                viewModel.startSpotifyImport(id)
                                importSource = null
                            }
                        }
                    }
                    ImportSource.YOUTUBE -> {
                        // Reject if it looks like a Spotify URL
                        if (looksLikeSpotifyUrl(importUrlInput)) {
                            importUrlError = "That looks like a Spotify URL. Use the Spotify option instead."
                        } else {
                            val url = parseYouTubePlaylistUrl(importUrlInput)
                            if (url == null) {
                                importUrlError = "Invalid YouTube Music playlist URL"
                            } else {
                                viewModel.startYouTubeImport(url)
                                importSource = null
                            }
                        }
                    }
                }
            },
        )
    }

    longPressedPlaylist?.let { playlist ->
        PlaylistOptionsDialog(
            playlist = playlist,
            onDismiss = { longPressedPlaylist = null },
            onDeletePlaylist = {
                viewModel.deletePlaylist(playlist)
                longPressedPlaylist = null
            },
            onRemoveSong = { song ->
                viewModel.removeSongFromPlaylist(playlist, song)
                longPressedPlaylist = null
            },
        )
    }
}

// ── Playlist tile (Symphony style) ──────────────────────────────────────────

@Composable
private fun PlaylistTile(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val songCount = playlist.songs.length()
    val displayName = playlist.name.removeSuffix(".json")
    val songs = remember(playlist.name, songCount) { Shared.getSongsFromPlaylist(playlist) }

    Card(
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            // ── Artwork thumbnail ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (songs.isEmpty()) {
                    Icon(
                        Icons.Filled.QueueMusic,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                } else {
                    PlaylistThumbnailGrid(songs = songs.take(4))
                }

                // Play button (bottom-start)
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            RoundedCornerShape(12.dp),
                        ),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        null,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Options (long press indicator, top-end)
                IconButton(
                    onClick = onLongClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp),
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Title ───────────────────────────────────────────────
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Song count ──────────────────────────────────────────
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── 2×2 Thumbnail Grid ────────────────────────────────────────────────────────

@Composable
private fun PlaylistThumbnailGrid(songs: List<Song>) {
    // Fill up to 4 slots; if fewer than 4 songs, repeat to fill the grid
    val slots = when (songs.size) {
        1 -> listOf(songs[0], songs[0], songs[0], songs[0])
        2 -> listOf(songs[0], songs[1], songs[1], songs[0])
        3 -> listOf(songs[0], songs[1], songs[2], songs[0])
        else -> songs.take(4)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            ThumbnailCell(song = slots[0], modifier = Modifier.weight(1f))
            ThumbnailCell(song = slots[1], modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f)) {
            ThumbnailCell(song = slots[2], modifier = Modifier.weight(1f))
            ThumbnailCell(song = slots[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ThumbnailCell(song: Song, modifier: Modifier = Modifier) {
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
        modifier = modifier.fillMaxSize(),
    )
}

// ── Import Dialogs ──────────────────────────────────────────────────────────

@Composable
private fun ImportSourcePickerDialog(
    onPickSpotify: () -> Unit,
    onPickYouTube: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Import from") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyGreen.copy(alpha = 0.12f))
                        .clickable(onClick = onPickSpotify)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_spot),
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Spotify", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Import from a Spotify playlist URL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(YouTubeRed.copy(alpha = 0.12f))
                        .clickable(onClick = onPickYouTube)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("YouTube Music", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Import from a YouTube or YT Music playlist URL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun ImportUrlDialog(
    source: ImportSource,
    input: String,
    errorMessage: String?,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val accentColor = when (source) {
        ImportSource.SPOTIFY -> SpotifyGreen
        ImportSource.YOUTUBE -> YouTubeRed
    }
    val title = when (source) {
        ImportSource.SPOTIFY -> "Import Spotify Playlist"
        ImportSource.YOUTUBE -> "Import YouTube Playlist"
    }
    val label = when (source) {
        ImportSource.SPOTIFY -> "Spotify playlist URL"
        ImportSource.YOUTUBE -> "YouTube playlist URL"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(label) },
                    isError = errorMessage != null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        cursorColor = accentColor,
                    ),
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Import", color = accentColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun PlaylistOptionsDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveSong: (io.github.uditkarode.able.model.song.Song) -> Unit,
) {
    val songs = remember(playlist) { Shared.getSongsFromPlaylist(playlist) }
    var removeSongMode by remember { mutableStateOf(false) }

    if (removeSongMode) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Remove song") },
            text = {
                LazyColumn {
                    items(songs, key = { it.filePath }) { song ->
                        Text(
                            text = song.name,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRemoveSong(song) }
                                .padding(vertical = 10.dp),
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 0.5.dp,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(playlist.name.removeSuffix(".json")) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { removeSongMode = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Remove a song", fontSize = 14.sp)
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeletePlaylist() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Delete playlist", color = Color(0xFFEF5350), fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) {
                Text("Create", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

// ── URL Parsing & Validation ────────────────────────────────────────────────

private fun parseSpotifyPlaylistId(url: String): String? {
    val parts = url.replace("https://", "").replace("http://", "").split("/").toMutableList()
    if (parts.isEmpty() || parts[0] != "open.spotify.com") return null
    val playlistIndex = parts.indexOf("playlist")
    if (playlistIndex == -1 || playlistIndex + 1 >= parts.size) return null
    val raw = parts[playlistIndex + 1]
    return if (raw.contains("?")) raw.split("?")[0] else raw
}

/**
 * Accepts YouTube and YouTube Music playlist URLs in various formats:
 * - https://www.youtube.com/playlist?list=PLxxxxxx
 * - https://music.youtube.com/playlist?list=PLxxxxxx
 * - https://youtube.com/playlist?list=PLxxxxxx
 * - https://www.youtube.com/watch?v=xxx&list=PLxxxxxx
 * - https://music.youtube.com/watch?v=xxx&list=PLxxxxxx
 *
 * Returns a canonical playlist URL for NewPipe extractor.
 */
private fun parseYouTubePlaylistUrl(url: String): String? {
    val cleaned = url.trim()
    // Extract list= parameter
    val listId = Regex("[?&]list=([A-Za-z0-9_-]+)").find(cleaned)?.groupValues?.get(1)
    if (listId != null) {
        // Validate that the domain is YouTube
        val lower = cleaned.lowercase()
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return "https://www.youtube.com/playlist?list=$listId"
        }
        return null
    }
    // Also accept direct playlist path: youtube.com/playlist?list=...
    // Already handled above via regex
    return null
}

private fun looksLikeYouTubeUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("music.youtube")
}

private fun looksLikeSpotifyUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.contains("spotify.com") || lower.contains("open.spotify")
}
