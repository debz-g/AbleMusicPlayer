package io.github.uditkarode.able.presentation.playlists

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.Playlist
import io.github.uditkarode.able.utils.Shared

private val Bg      = Color(0xFF212121)
private val Surface = Color(0xFF2C2C2C)
private val White   = Color(0xFFFBFBFB)
private val Gray    = Color(0xFF888888)
private val Green   = Color(0xFF1DB954)

@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var spotifyDialogOpen by rememberSaveable { mutableStateOf(false) }
    var spotifyUrlInput by rememberSaveable { mutableStateOf("") }
    var spotifyUrlError by rememberSaveable { mutableStateOf(false) }

    var longPressedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        if (state.playlists.isEmpty()) {
            Text(
                text = "No playlists yet",
                color = Gray,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.playlists, key = { it.name }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist.name) },
                        onLongClick = { longPressedPlaylist = playlist },
                    )
                    HorizontalDivider(color = Surface, thickness = 0.5.dp)
                }
            }
        }

        SpotifyButton(
            isImporting = state.isImporting,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                if (state.isImporting) {
                    viewModel.cancelSpotifyImport()
                } else {
                    spotifyUrlInput = ""
                    spotifyUrlError = false
                    spotifyDialogOpen = true
                }
            },
        )
    }

    if (spotifyDialogOpen) {
        SpotifyImportDialog(
            input = spotifyUrlInput,
            hasError = spotifyUrlError,
            onInputChange = {
                spotifyUrlInput = it
                spotifyUrlError = false
            },
            onDismiss = { spotifyDialogOpen = false },
            onConfirm = {
                val id = parseSpotifyPlaylistId(spotifyUrlInput)
                if (id == null) {
                    spotifyUrlError = true
                } else {
                    viewModel.startSpotifyImport(id)
                    spotifyDialogOpen = false
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

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = playlist.name.removeSuffix(".json"),
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        val count = playlist.songs.length()
        Text(
            text = if (count == 1) "1 song" else "$count songs",
            color = Gray,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onLongClick,
            modifier = Modifier.height(24.dp),
        ) {
            Text(text = "Options", color = Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SpotifyButton(
    isImporting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .background(if (isImporting) Color(0xFF383838) else Green, shape = androidx.compose.foundation.shape.CircleShape),
    ) {
        Icon(
            painter = painterResource(if (isImporting) R.drawable.ic_cancle_action else R.drawable.ic_spot),
            contentDescription = if (isImporting) "Cancel import" else "Import from Spotify",
            tint = White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SpotifyImportDialog(
    input: String,
    hasError: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2C),
        title = { Text("Import Spotify Playlist", color = White) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("Spotify playlist URL", color = Gray) },
                    isError = hasError,
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        errorTextColor = White,
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Gray,
                        errorBorderColor = Color.Red,
                    ),
                )
                if (hasError) {
                    Spacer(Modifier.height(4.dp))
                    Text("Invalid Spotify playlist URL", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Import", color = Green) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Gray) }
        },
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
            containerColor = Color(0xFF2C2C2C),
            title = { Text("Remove song", color = White) },
            text = {
                LazyColumn {
                    items(songs, key = { it.filePath }) { song ->
                        Text(
                            text = song.name,
                            color = White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRemoveSong(song) }
                                .padding(vertical = 10.dp),
                        )
                        HorizontalDivider(color = Surface, thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Gray) }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF2C2C2C),
            title = { Text(playlist.name.removeSuffix(".json"), color = White) },
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
                            painter = painterResource(R.drawable.ic_music_note_black_24dp),
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Remove a song", color = White, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = Surface, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeletePlaylist() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancle_action),
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Delete playlist", color = Color.Red, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Gray) }
            },
        )
    }
}

private fun parseSpotifyPlaylistId(url: String): String? {
    val parts = url.replace("https://", "").split("/").toMutableList()
    if (parts.isEmpty() || parts[0] != "open.spotify.com") return null
    val playlistIndex = parts.indexOf("playlist")
    if (playlistIndex == -1 || playlistIndex + 1 >= parts.size) return null
    val raw = parts[playlistIndex + 1]
    return if (raw.contains("?")) raw.split("?")[0] else raw
}
