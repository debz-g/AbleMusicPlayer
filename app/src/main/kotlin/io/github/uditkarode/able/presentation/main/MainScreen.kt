package io.github.uditkarode.able.presentation.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.viewpager2.widget.ViewPager2
import io.github.uditkarode.able.R

private val BgColor   = Color(0xFF212121)
private val BgBottom  = Color(0xFF212121)
private val White     = Color(0xFFFBFBFB)
private val WhiteDim  = Color(0x80FBFBFB)
private val Accent    = Color(0xFF5E92F3)

// Tab descriptor — keeps nav bar and ViewPager in sync
private data class NavTab(val labelRes: Int, val iconRes: Int)

private val tabs = listOf(
    NavTab(R.string.home,      R.drawable.ic_home_black_24dp),
    NavTab(R.string.search,    R.drawable.search),
    NavTab(R.string.library,   R.drawable.ic_music_note_black_24dp),
    NavTab(R.string.playlists, R.drawable.ic_library_music_black_24dp),
)

/**
 * Root Compose layout for MainActivity.
 *
 * @param miniPlayerState   Live state for the mini player bar.
 * @param onPlayPause       Forwarded to the ViewModel.
 * @param onOpenPlayer      Navigate to the full Player activity.
 * @param vpSetup           One-time ViewPager2 initialisation supplied by the Activity.
 */
@Composable
fun MainScreen(
    miniPlayerState: MiniPlayerState,
    onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    vpSetup: (ViewPager2) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // Store the ViewPager2 reference so tab taps can call setCurrentItem
    var viewPager by remember { mutableStateOf<ViewPager2?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── ViewPager2 content (fills all remaining vertical space) ──────────
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    ViewPager2(ctx).also { vp ->
                        vp.isUserInputEnabled = false
                        vp.offscreenPageLimit = 3
                        vpSetup(vp)
                        viewPager = vp
                    }
                },
                update = { vp ->
                    if (vp.currentItem != selectedTab) vp.setCurrentItem(selectedTab, false)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Mini player bar ───────────────────────────────────────────────────
        MiniPlayerBar(
            state       = miniPlayerState,
            onPlayPause = onPlayPause,
            onExpand    = onOpenPlayer,
        )

        // ── 1 dp divider between mini player and bottom nav ──────────────────
        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        // ── Bottom navigation ─────────────────────────────────────────────────
        NavigationBar(
            containerColor = BgBottom,
            tonalElevation = 0.dp,
            modifier       = Modifier.navigationBarsPadding(),
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected  = selectedTab == index,
                    onClick   = {
                        if (selectedTab != index) {
                            selectedTab = index
                            viewPager?.setCurrentItem(index, false)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = {
                        Text(
                            // Using hardcoded strings to avoid resource look-up in tight recompose
                            text = when (index) {
                                0 -> "Home"
                                1 -> "Search"
                                2 -> "Library"
                                else -> "Playlists"
                            },
                            fontSize = 12.sp,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = White,
                        selectedTextColor   = White,
                        unselectedIconColor = WhiteDim,
                        unselectedTextColor = WhiteDim,
                        indicatorColor      = Accent.copy(alpha = 0.2f),
                    ),
                )
            }
        }
    }
}

// ── Mini player bar ───────────────────────────────────────────────────────────

@Composable
private fun MiniPlayerBar(
    state: MiniPlayerState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
) {
    val seekFraction = if (state.durationMs > 0)
        state.positionMs.toFloat() / state.durationMs.toFloat()
    else 0f

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(BgBottom)
        .clickable(enabled = state.isVisible, onClick = onExpand)
    ) {
        // ── Seekbar row (1 dp track, or indeterminate when loading) ──────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = Accent,
                    trackColor = WhiteDim,
                )
            } else {
                // Static thin seek track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WhiteDim)
                )
                // Progress fill
                val animatedFraction by animateFloatAsState(
                    targetValue = seekFraction,
                    animationSpec = tween(300),
                    label = "seekProgress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(1.dp)
                        .background(White)
                )
            }
        }

        // ── Content row ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(51.5.dp)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Expand arrow
            Icon(
                painter = painterResource(R.drawable.up_arrow),
                contentDescription = "Open player",
                tint = White,
                modifier = Modifier.size(32.dp),
            )

            Spacer(Modifier.width(5.dp))

            // Song name • artist (scrolling text)
            val label = when {
                !state.isVisible -> "AbleMusicPlayer"
                else -> "${state.songName} • ${state.artistName}"
            }
            Text(
                text = label,
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(10.dp))

            // Play / Pause icon (hidden while loading)
            if (!state.isLoading && state.isVisible) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = White,
                        modifier = Modifier.size(29.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}
