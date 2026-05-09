package io.github.uditkarode.able.presentation.home

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.utils.Constants
import io.github.uditkarode.able.utils.Shared
import java.io.File

private val BgColor    = Color(0xFF1A1A1A)
private val ItemBg     = Color(0xFF2C2C2C)
private val White      = Color(0xFFFBFBFB)
private val Gray       = Color(0xFF888888)
private val Accent     = Color(0xFF5E92F3)
private val ActionGray = Color(0xFF383838)

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkPendingDownload()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgColor),
    ) {
        HomeHeader(onOpenSettings = onOpenSettings)

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = White)
                }

                state.songs.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No songs found",
                        color = Gray,
                        fontSize = 14.sp,
                    )
                }

                else -> SongList(
                    songs = state.songs,
                    recentSongs = state.recentSongs,
                    currentSongPath = state.currentSongPath,
                    playlists = state.playlists,
                    onIntent = viewModel::onIntent,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AbleMusicPlayer",
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ── Recently Added horizontal card ──────────────────────────────────────────

@Composable
private fun RecentSongCard(
    song: Song,
    isPlaying: Boolean,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ItemBg)
            .clickable(onClick = onTap)
            .padding(8.dp),
    ) {
        SongArt(
            song = song,
            size = 114,
            cornerRadius = 8,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = song.name,
            color = if (isPlaying) Accent else White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist.ifBlank { "Unknown Artist" },
            color = Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Main song list with sections ────────────────────────────────────────────

@Composable
private fun SongList(
    songs: List<Song>,
    recentSongs: List<Song>,
    currentSongPath: String,
    playlists: List<String>,
    onIntent: (HomeIntent) -> Unit,
) {
    // Track which song row has its action panel open
    var expandedPath by rememberSaveable { mutableStateOf("") }

    // Dialog state
    var playlistDialogSong by remember { mutableStateOf<Song?>(null) }
    var createPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var deleteConfirmSong by remember { mutableStateOf<Song?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── Recently Added section ─────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            item(key = "recent_header") {
                Text(
                    text = "Recently Added",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp),
                )
            }

            item(key = "recent_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        recentSongs,
                        key = { _, s -> "recent_${s.filePath.ifEmpty { s.youtubeLink }}" },
                    ) { idx, song ->
                        RecentSongCard(
                            song = song,
                            isPlaying = song.filePath == currentSongPath,
                            onTap = {
                                // Find the index in the full songs list
                                val fullIndex = songs.indexOfFirst { it.filePath == song.filePath }
                                if (fullIndex >= 0) onIntent(HomeIntent.TapSong(fullIndex))
                            },
                        )
                    }
                }
            }

            item(key = "recent_spacer") {
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── All Songs section ──────────────────────────────────────
        item(key = "all_songs_header") {
            Text(
                text = "All Songs",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
        }

        itemsIndexed(songs, key = { _, s -> s.filePath.ifEmpty { s.youtubeLink } }) { idx, song ->
            SongItem(
                song = song,
                isPlaying = song.filePath == currentSongPath,
                isExpanded = expandedPath == song.filePath,
                onTap = {
                    expandedPath = ""
                    onIntent(HomeIntent.TapSong(idx))
                },
                onLongPress = {
                    expandedPath = if (expandedPath == song.filePath) "" else song.filePath
                },
                onAddToQueue = {
                    expandedPath = ""
                    onIntent(HomeIntent.AddToQueue(song))
                },
                onAddToPlaylist = {
                    expandedPath = ""
                    playlistDialogSong = song
                },
                onDelete = {
                    expandedPath = ""
                    deleteConfirmSong = song
                },
            )
            if (idx < songs.lastIndex) {
                HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 0.5.dp)
            }
        }
    }

    // Add to playlist dialog
    playlistDialogSong?.let { song ->
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { name ->
                onIntent(HomeIntent.AddToPlaylist(song, name))
                playlistDialogSong = null
            },
            onCreateNew = {
                playlistDialogSong = null
                createPlaylistSong = song
            },
            onDismiss = { playlistDialogSong = null },
        )
    }

    // Create playlist dialog
    createPlaylistSong?.let { song ->
        CreatePlaylistDialog(
            onCreate = { name ->
                onIntent(HomeIntent.CreatePlaylist(name, song))
                createPlaylistSong = null
            },
            onDismiss = { createPlaylistSong = null },
        )
    }

    // Delete confirm dialog
    deleteConfirmSong?.let { song ->
        DeleteConfirmDialog(
            songName = song.name,
            onConfirm = {
                onIntent(HomeIntent.DeleteSong(song))
                deleteConfirmSong = null
            },
            onDismiss = { deleteConfirmSong = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItem(
    song: Song,
    isPlaying: Boolean,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    val nameColor = if (isPlaying) Accent else White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isExpanded) ItemBg else BgColor)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongArt(song = song)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    color = nameColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = song.artist.ifBlank { "Unknown Artist" },
                    color = Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }

        if (isExpanded) {
            ActionRow(
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun SongArt(
    song: Song,
    size: Int = 48,
    cornerRadius: Int = 6,
) {
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
            song.ytmThumbnail.isNotBlank() -> Shared.getSmallThumbnailUrl(song.ytmThumbnail)
            else -> io.github.uditkarode.able.R.drawable.def_albart
        }
    }

    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color(0xFF2C2C2C)),
        error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2C2C2C)),
    )
}

@Composable
private fun ActionRow(
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 8.dp, bottom = 8.dp),
    ) {
        ActionChip(label = "Queue", icon = {
            Icon(Icons.Default.AddCircleOutline, null, tint = White, modifier = Modifier.size(14.dp))
        }, onClick = onAddToQueue)

        Spacer(Modifier.width(6.dp))

        ActionChip(label = "Playlist", icon = {
            Icon(Icons.Default.PlaylistAdd, null, tint = White, modifier = Modifier.size(14.dp))
        }, onClick = onAddToPlaylist)

        Spacer(Modifier.width(6.dp))

        ActionChip(label = "Delete", icon = {
            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
        }, onClick = onDelete)
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ActionGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(text = label, color = White, fontSize = 11.sp)
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<String>,
    onPick: (String) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2C),
        title = { Text("Add to playlist", color = White) },
        text = {
            Column {
                playlists.forEach { name ->
                    Text(
                        text = name,
                        color = White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(name) }
                            .padding(vertical = 10.dp),
                    )
                    HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)
                }
                Text(
                    text = "+ Create new playlist",
                    color = Accent,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateNew)
                        .padding(vertical = 10.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray)
            }
        },
    )
}

@Composable
private fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2C),
        title = { Text("New playlist", color = White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Gray,
                    cursorColor = White,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) {
                Text("Create", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray)
            }
        },
    )
}

@Composable
private fun DeleteConfirmDialog(
    songName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2C),
        title = { Text("Delete song?", color = White) },
        text = {
            Text(
                text = "\"$songName\" will be permanently removed from your device.",
                color = Gray,
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFEF5350))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray)
            }
        },
    )
}
