package io.github.uditkarode.able.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.SongState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BgColor  = Color(0xFF212121)
private val White    = Color(0xFFFBFBFB)
private val WhiteDim = Color(0x80FBFBFB)
private val Accent   = Color(0xFF5E92F3)

private val MINI_BAR_HEIGHT = 52.dp

enum class PlayerAnchor { Collapsed, Expanded }

@Composable
fun ExpandablePlayerPanel(
    playerState: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playerState.currentSong == null) return

    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier) {
        val maxHeightPx  = constraints.maxHeight.toFloat()
        val miniBarPx    = with(density) { MINI_BAR_HEIGHT.toPx() }
        val collapsedOffset = maxHeightPx - miniBarPx

        val dragState = remember(maxHeightPx) {
            AnchoredDraggableState(
                initialValue        = PlayerAnchor.Collapsed,
                anchors             = DraggableAnchors {
                    PlayerAnchor.Collapsed at collapsedOffset
                    PlayerAnchor.Expanded  at 0f
                },
                positionalThreshold = { distance -> distance * 0.3f },
                velocityThreshold   = { with(density) { 125.dp.toPx() } },
                snapAnimationSpec   = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessMedium,
                ),
                decayAnimationSpec  = exponentialDecay(),
            )
        }

        val panelOffset = runCatching { dragState.requireOffset() }.getOrDefault(collapsedOffset)
        val fraction = ((collapsedOffset - panelOffset) / collapsedOffset.coerceAtLeast(1f))
            .coerceIn(0f, 1f)

        BackHandler(enabled = fraction > 0.01f) {
            scope.launch { dragState.animateTo(PlayerAnchor.Collapsed) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = panelOffset }
                .anchoredDraggable(state = dragState, orientation = Orientation.Vertical)
                .background(BgColor),
        ) {
            // Full player — fades in as panel expands
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fraction),
            ) {
                PlayerScreen(
                    state       = playerState,
                    onIntent    = onIntent,
                    onBack      = { scope.launch { dragState.animateTo(PlayerAnchor.Collapsed) } },
                    onShowQueue = {},
                )
            }

            // Mini bar — fades out as panel expands
            PanelMiniBar(
                state       = playerState,
                onPlayPause = { onIntent(PlayerIntent.PlayPause) },
                onExpand    = { scope.launch { dragState.animateTo(PlayerAnchor.Expanded) } },
                modifier    = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .alpha(1f - fraction),
            )
        }
    }
}

// ── Mini bar shown at top of panel when collapsed ─────────────────────────────

@Composable
private fun PanelMiniBar(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seekFraction = if (state.durationMs > 0)
        state.positionMs.toFloat() / state.durationMs.toFloat() else 0f

    val animatedFraction by animateFloatAsState(
        targetValue   = seekFraction,
        animationSpec = tween(300),
        label         = "miniSeek",
    )

    Column(
        modifier = modifier
            .background(BgColor)
            .clickable(onClick = onExpand),
    ) {
        // Progress line
        Box(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth(),
                    color      = Accent,
                    trackColor = WhiteDim,
                )
            } else {
                Box(Modifier.fillMaxWidth().height(1.dp).background(WhiteDim))
                Box(Modifier.fillMaxWidth(animatedFraction).height(1.dp).background(White))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(51.dp)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter            = painterResource(R.drawable.up_arrow),
                contentDescription = "Open player",
                tint               = White,
                modifier           = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text       = "${state.displaySongName} • ${state.displayArtistName}",
                color      = White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            if (!state.isLoading) {
                IconButton(
                    onClick  = onPlayPause,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.songState == SongState.playing) R.drawable.pause
                            else R.drawable.play
                        ),
                        contentDescription = if (state.songState == SongState.playing) "Pause" else "Play",
                        tint     = White,
                        modifier = Modifier.size(29.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}
