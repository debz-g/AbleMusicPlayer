package io.github.uditkarode.able.presentation.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.SongState
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.Image

// Background of the player screen (unchanged from original XML)
private val BgColor = Color(0xFF212121)
private val TextPrimary = Color(0xFFFBFBFB)
private val TextSecondary = Color(0x80FBFBFB)
private val AccentInactive = Color(0x80FBFBFB)
private val AccentActive = Color(0x805E92F3)

private enum class DialogKind { EDIT_TITLE, EDIT_ARTIST, FETCH_ART }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onBack: () -> Unit,
    onShowQueue: () -> Unit,
) {
    var dialogKind by remember { mutableStateOf<DialogKind?>(null) }
    var dialogInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(75.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.down_arrow),
                        contentDescription = "Back",
                        tint = TextPrimary,
                    )
                }
                IconButton(onClick = onShowQueue, modifier = Modifier.size(68.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.pl_playlist),
                        contentDescription = "Queue",
                        tint = TextPrimary,
                    )
                }
            }

            // ── Album art (1:1 square, fills remaining space above controls) ─
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2A))
                        .combinedClickable(
                            onClick = {
                                if (state.isLocalSong) {
                                    dialogInput = ""
                                    dialogKind = DialogKind.FETCH_ART
                                }
                            },
                            onLongClick = {
                                if (state.isLocalSong) {
                                    dialogInput = state.displaySongName
                                    dialogKind = DialogKind.FETCH_ART
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.isLoading -> CircularProgressIndicator(color = state.seekbarColor)
                        state.albumArt != null -> Image(
                            bitmap = state.albumArt.asImageBitmap(),
                            contentDescription = "Album art",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Icon(
                            painter = painterResource(R.drawable.ic_music_note_black_24dp),
                            contentDescription = "No art",
                            tint = TextSecondary,
                            modifier = Modifier.size(128.dp),
                        )
                    }
                }
            }

            // ── Controls ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {

                // Song name + artist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = state.displaySongName,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (state.isLocalSong) Modifier.combinedClickable(
                            onClick = {
                                dialogInput = state.displaySongName
                                dialogKind = DialogKind.EDIT_TITLE
                            },
                            onLongClick = {},
                        ) else Modifier,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.displayArtistName,
                        color = TextSecondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (state.isLocalSong) Modifier.combinedClickable(
                            onClick = {
                                dialogInput = state.displayArtistName
                                dialogKind = DialogKind.EDIT_ARTIST
                            },
                            onLongClick = {},
                        ) else Modifier,
                    )
                }

                // Seekbar
                var isDragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }

                val sliderValue = if (isDragging) dragValue
                    else if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs.toFloat()
                    else 0f

                Slider(
                    value = sliderValue,
                    onValueChange = { v ->
                        isDragging = true
                        dragValue = v
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        val seekMs = (dragValue * state.durationMs).toInt()
                        onIntent(PlayerIntent.SeekTo(seekMs))
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = state.seekbarColor,
                        activeTrackColor = state.seekbarColor,
                        inactiveTrackColor = TextSecondary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                // Time labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = msToTime(state.positionMs),
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = msToTime(state.durationMs),
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Playback controls row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { onIntent(PlayerIntent.ToggleShuffle) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = "Shuffle",
                            tint = if (state.isShuffling) AccentActive else AccentInactive,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = { onIntent(PlayerIntent.Previous) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = state.controlsColor,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    // Play / Pause (center, larger)
                    IconButton(
                        onClick = { onIntent(PlayerIntent.PlayPause) },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.songState == SongState.playing) R.drawable.nobg_pause
                                else R.drawable.nobg_play
                            ),
                            contentDescription = if (state.songState == SongState.playing) "Pause" else "Play",
                            tint = state.controlsColor,
                            modifier = Modifier.size(62.dp),
                        )
                    }

                    // Next
                    IconButton(
                        onClick = { onIntent(PlayerIntent.Next) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = state.controlsColor,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = { onIntent(PlayerIntent.ToggleRepeat) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.repeat),
                            contentDescription = "Repeat",
                            tint = if (state.isRepeating) AccentActive else AccentInactive,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                }
            }

            // ── Cast row (disabled) ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.cast),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Cast (disabled)",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    when (dialogKind) {
        DialogKind.EDIT_TITLE -> InputDialog(
            title = stringResource(R.string.enter_new_song),
            initial = dialogInput,
            onConfirm = { onIntent(PlayerIntent.EditSongTitle(it)); dialogKind = null },
            onDismiss = { dialogKind = null },
        )
        DialogKind.EDIT_ARTIST -> InputDialog(
            title = stringResource(R.string.enter_new_art),
            initial = dialogInput,
            onConfirm = { onIntent(PlayerIntent.EditArtistName(it)); dialogKind = null },
            onDismiss = { dialogKind = null },
        )
        DialogKind.FETCH_ART -> InputDialog(
            title = stringResource(R.string.enter_song),
            initial = dialogInput,
            onConfirm = { onIntent(PlayerIntent.FetchAlbumArt(it.ifBlank { null }, forceDeezer = true)); dialogKind = null },
            onDismiss = { dialogKind = null },
        )
        null -> Unit
    }
}

@Composable
private fun InputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Color(0xFF5E92F3),
                    unfocusedBorderColor = TextSecondary,
                    cursorColor = Color(0xFF5E92F3),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("OK", color = Color(0xFF5E92F3))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
    )
}

private fun msToTime(ms: Int): String {
    val duration = ms.toLong()
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
            TimeUnit.MINUTES.toSeconds(minutes)
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
