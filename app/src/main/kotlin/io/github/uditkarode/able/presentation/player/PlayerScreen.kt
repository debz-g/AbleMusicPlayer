package io.github.uditkarode.able.presentation.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.SongState
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.Image

private val defaultHorizontalPadding = 20.dp

private enum class DialogKind { EDIT_TITLE, EDIT_ARTIST, FETCH_ART }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    var dialogKind by remember { mutableStateOf<DialogKind?>(null) }
    var dialogInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Now Playing",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            },
            actions = {
                Spacer(modifier = Modifier.size(48.dp))
            },
        )

        // ── Album art ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints {
                val dimension = min(maxHeight, maxWidth)

                Box(
                    modifier = Modifier
                        .size(dimension)
                        .padding(horizontal = defaultHorizontalPadding)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        state.isLoading -> CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                        state.albumArt != null -> Image(
                            bitmap = state.albumArt.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Icon(
                            painterResource(R.drawable.ic_music_note_black_24dp),
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(100.dp),
                        )
                    }
                }
            }
        }

        // ── Song info ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(defaultHorizontalPadding, 0.dp),
        ) {
            Text(
                state.displaySongName,
                style = MaterialTheme.typography.headlineSmall
                    .copy(fontWeight = FontWeight.Bold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = if (state.isLocalSong) Modifier.combinedClickable(
                    onClick = {
                        dialogInput = state.displaySongName
                        dialogKind = DialogKind.EDIT_TITLE
                    },
                    onLongClick = {},
                ) else Modifier,
            )
            if (state.displayArtistName.isNotBlank()) {
                Text(
                    state.displayArtistName,
                    maxLines = 2,
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
        }

        Spacer(modifier = Modifier.height(defaultHorizontalPadding + 8.dp))

        // ── Controls ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(defaultHorizontalPadding, 0.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(
                icon = if (state.songState != SongState.playing) Icons.Filled.PlayArrow
                       else Icons.Filled.Pause,
                onClick = { onIntent(PlayerIntent.PlayPause) },
                accent = true,
            )
            Spacer(modifier = Modifier.width(16.dp))
            ControlButton(
                icon = Icons.Filled.SkipPrevious,
                onClick = { onIntent(PlayerIntent.Previous) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            ControlButton(
                icon = Icons.Filled.SkipNext,
                onClick = { onIntent(PlayerIntent.Next) },
            )
        }

        Spacer(modifier = Modifier.height(defaultHorizontalPadding + 8.dp))

        // ── Seekbar ──────────────────────────────────────────────────────
        NowPlayingSeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeekTo = { onIntent(PlayerIntent.SeekTo(it)) },
        )

        Spacer(modifier = Modifier.height(defaultHorizontalPadding))

        // ── Bottom bar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val queueText = if (state.queue.isNotEmpty())
                    "Playing ${state.currentIndex + 1} of ${state.queue.size}"
                else "Queue"
                Text(queueText, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onIntent(PlayerIntent.ToggleRepeat) }) {
                Icon(
                    Icons.Filled.Repeat,
                    null,
                    tint = when {
                        state.isRepeating -> MaterialTheme.colorScheme.primary
                        else -> LocalContentColor.current
                    },
                )
            }
            IconButton(onClick = { onIntent(PlayerIntent.ToggleShuffle) }) {
                Icon(
                    Icons.Filled.Shuffle,
                    null,
                    tint = when {
                        state.isShuffling -> MaterialTheme.colorScheme.primary
                        else -> LocalContentColor.current
                    },
                )
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────
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

// ── Custom seekbar (Symphony style) ──────────────────────────────────────────

@Composable
private fun NowPlayingSeekBar(
    positionMs: Int,
    durationMs: Int,
    onSeekTo: (Int) -> Unit,
) {
    val ratio = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    Row(
        modifier = Modifier.padding(defaultHorizontalPadding, 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var seekRatio by remember { mutableStateOf<Float?>(null) }

        PositionText(
            seekRatio?.let { (it * durationMs).toLong() } ?: positionMs.toLong(),
            Alignment.CenterStart,
        )
        Box(modifier = Modifier.weight(1f)) {
            SeekBarTrack(
                ratio = ratio,
                onSeekEnd = { r ->
                    onSeekTo((r * durationMs).toInt())
                    seekRatio = null
                },
                onSeek = { seekRatio = it },
                onSeekStart = { seekRatio = 0f },
                onSeekCancel = { seekRatio = null },
            )
        }
        PositionText(durationMs.toLong(), Alignment.CenterEnd)
    }
}

@Composable
private fun SeekBarTrack(
    ratio: Float,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekEnd: (Float) -> Unit,
    onSeekCancel: () -> Unit,
) {
    val sliderHeight = 12.dp
    val thumbSize = 12.dp
    val thumbSizeHalf = thumbSize.div(2)
    val trackHeight = 4.dp

    var dragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(sliderHeight),
        contentAlignment = Alignment.Center,
    ) {
        val sliderWidth = maxWidth

        Box(
            modifier = Modifier
                .height(sliderHeight)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val tapRatio = (offset.x / sliderWidth.toPx()).coerceIn(0f..1f)
                            onSeekEnd(tapRatio)
                        }
                    )
                }
                .pointerInput(Unit) {
                    var offsetX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            offsetX = offset.x
                            dragging = true
                            onSeekStart()
                        },
                        onDragEnd = {
                            onSeekEnd(dragRatio)
                            offsetX = 0f
                            dragging = false
                            dragRatio = 0f
                        },
                        onDragCancel = {
                            onSeekCancel()
                            offsetX = 0f
                            dragging = false
                            dragRatio = 0f
                        },
                        onHorizontalDrag = { pointer, dragAmount ->
                            pointer.consume()
                            offsetX += dragAmount
                            dragRatio = (offsetX / sliderWidth.toPx()).coerceIn(0f..1f)
                            onSeek(dragRatio)
                        },
                    )
                }
        )
        Box(
            modifier = Modifier
                .padding(thumbSizeHalf, 0.dp)
                .height(trackHeight)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(thumbSizeHalf),
                )
        ) {
            Box(
                modifier = Modifier
                    .height(trackHeight)
                    .fillMaxWidth(if (dragging) dragRatio else ratio)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(thumbSizeHalf),
                    )
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .offset(
                        sliderWidth
                            .minus(thumbSizeHalf.times(2))
                            .times(if (dragging) dragRatio else ratio),
                        0.dp,
                    )
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun PositionText(duration: Long, alignment: Alignment) {
    val textStyle = MaterialTheme.typography.labelMedium
    val formatted = msToTime(duration.toInt())
    Box(contentAlignment = alignment) {
        Text("0".repeat(formatted.length), style = textStyle.copy(color = Color.Transparent))
        Text(formatted, style = textStyle)
    }
}

// ── Control buttons ──────────────────────────────────────────────────────────

@Composable
private fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    val bg = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (accent) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
    IconButton(
        modifier = Modifier
            .size(56.dp)
            .background(bg, CircleShape),
        onClick = onClick,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────

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
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
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
