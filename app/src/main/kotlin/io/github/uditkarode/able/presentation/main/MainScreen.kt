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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.home.HomeScreen
import io.github.uditkarode.able.presentation.library.LibraryScreen
import io.github.uditkarode.able.presentation.playlists.PlaylistsScreen
import io.github.uditkarode.able.presentation.search.SearchScreen
import kotlinx.coroutines.launch

private val BgColor  = Color(0xFF212121)
private val BgBottom = Color(0xFF212121)
private val White    = Color(0xFFFBFBFB)
private val WhiteDim = Color(0x80FBFBFB)
private val Accent   = Color(0xFF5E92F3)

private data class NavTab(val label: String, val iconRes: Int)

private val tabs = listOf(
    NavTab("Home",      R.drawable.ic_home_black_24dp),
    NavTab("Search",    R.drawable.search),
    NavTab("Library",   R.drawable.ic_music_note_black_24dp),
    NavTab("Playlists", R.drawable.ic_library_music_black_24dp),
)

@Composable
fun MainScreen(
    miniPlayerState: MiniPlayerState,
    onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSendItem: (song: Song, mode: String) -> Unit,
    onOpenGroup: (title: String, songs: List<Song>) -> Unit,
    onOpenPlaylist: (name: String) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding(),
    ) {
        // ── Tab content ───────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> HomeScreen(onOpenSettings = onOpenSettings)
                1 -> SearchScreen(onSendItem = onSendItem)
                2 -> LibraryScreen(onOpenGroup = onOpenGroup)
                3 -> PlaylistsScreen(onOpenPlaylist = onOpenPlaylist)
            }
        }

        // ── Mini player bar ───────────────────────────────────────────────────
        MiniPlayerBar(
            state       = miniPlayerState,
            onPlayPause = onPlayPause,
            onExpand    = onOpenPlayer,
        )

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        // ── Bottom navigation ─────────────────────────────────────────────────
        NavigationBar(
            containerColor = BgBottom,
            tonalElevation = 0.dp,
            modifier       = Modifier.navigationBarsPadding(),
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected  = pagerState.currentPage == index,
                    onClick   = {
                        if (pagerState.currentPage != index) {
                            scope.launch { pagerState.scrollToPage(index) }
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(tab.label, fontSize = 12.sp) },
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth(),
                    color      = Accent,
                    trackColor = WhiteDim,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WhiteDim)
                )
                val animatedFraction by animateFloatAsState(
                    targetValue    = seekFraction,
                    animationSpec  = tween(300),
                    label          = "seekProgress",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(1.dp)
                        .background(White)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(51.5.dp)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.up_arrow),
                contentDescription = "Open player",
                tint     = White,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(5.dp))
            val label = if (!state.isVisible) "AbleMusicPlayer"
                        else "${state.songName} • ${state.artistName}"
            Text(
                text       = label,
                color      = White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            if (!state.isLoading && state.isVisible) {
                IconButton(
                    onClick  = onPlayPause,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
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
