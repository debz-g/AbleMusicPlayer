package io.github.uditkarode.able.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import io.github.uditkarode.able.model.song.SongState

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playerState.currentSong == null) return

    val seekFraction = if (playerState.durationMs > 0)
        playerState.positionMs.toFloat() / playerState.durationMs.toFloat() else 0f

    val animatedFraction by animateFloatAsState(
        targetValue = seekFraction,
        animationSpec = tween(300),
        label = "miniSeek",
    )

    Column(modifier = modifier) {
        // Progress line
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.25f))
                .height(2.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(MaterialTheme.colorScheme.surfaceTint)
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
            )
        }

        // Card body
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RectangleShape,
            onClick = onClick,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(0.dp, 8.dp),
            ) {
                Spacer(modifier = Modifier.width(12.dp))

                // Album art
                AnimatedContent(
                    label = "mini-player-art",
                    targetState = playerState.albumArt,
                    transitionSpec = {
                        fadeIn(tween(300, delayMillis = 300)) togetherWith fadeOut(tween(300))
                    },
                ) { art ->
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (art != null) {
                            Image(
                                bitmap = art.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(15.dp))

                // Song info with swipe-to-skip
                MiniBarSongInfo(
                    state = playerState,
                    onSkipNext = { onIntent(PlayerIntent.Next) },
                    onSkipPrevious = { onIntent(PlayerIntent.Previous) },
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(15.dp))

                IconButton(onClick = { onIntent(PlayerIntent.Previous) }) {
                    Icon(Icons.Filled.SkipPrevious, null)
                }
                IconButton(onClick = { onIntent(PlayerIntent.PlayPause) }) {
                    Icon(
                        when {
                            playerState.songState != SongState.playing -> Icons.Filled.PlayArrow
                            else -> Icons.Filled.Pause
                        },
                        null,
                    )
                }
                IconButton(onClick = { onIntent(PlayerIntent.Next) }) {
                    Icon(Icons.Filled.SkipNext, null)
                }

                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun MiniBarSongInfo(
    state: PlayerState,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val cardWidthPx = constraints.maxWidth
        var offsetX by remember { mutableFloatStateOf(0f) }
        val cardOffsetX by animateFloatAsState(
            offsetX / 2,
            label = "mini-card-offset-x",
        )
        val cardOpacity by animateFloatAsState(
            if (offsetX != 0f) 0.7f else 1f,
            label = "mini-card-opacity",
        )

        Box(
            modifier = Modifier
                .graphicsLayer(alpha = cardOpacity, translationX = cardOffsetX)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val thresh = cardWidthPx / 4
                            when {
                                -offsetX > thresh -> onSkipNext()
                                offsetX > thresh -> onSkipPrevious()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount },
                    )
                },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.displaySongName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.displayArtistName.isNotBlank()) {
                    Text(
                        state.displayArtistName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
