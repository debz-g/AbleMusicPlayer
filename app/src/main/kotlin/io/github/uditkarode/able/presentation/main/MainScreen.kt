package io.github.uditkarode.able.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.home.HomeScreen
import io.github.uditkarode.able.presentation.library.LibraryScreen
import io.github.uditkarode.able.presentation.player.ExpandablePlayerPanel
import io.github.uditkarode.able.presentation.player.PlayerIntent
import io.github.uditkarode.able.presentation.player.PlayerState
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
    playerState: PlayerState,
    onPlayerIntent: (PlayerIntent) -> Unit,
    onOpenSettings: () -> Unit,
    onSendItem: (song: Song, mode: String) -> Unit,
    onOpenGroup: (title: String, songs: List<Song>) -> Unit,
    onOpenPlaylist: (name: String) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding(),
    ) {
        // ── Main content (tabs + bottom nav) ─────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state             = pagerState,
                userScrollEnabled = false,
                modifier          = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> HomeScreen(onOpenSettings = onOpenSettings)
                    1 -> SearchScreen(onSendItem = onSendItem)
                    2 -> LibraryScreen(onOpenGroup = onOpenGroup)
                    3 -> PlaylistsScreen(onOpenPlaylist = onOpenPlaylist)
                }
            }

            HorizontalDivider(color = Color.Black, thickness = 1.dp)

            NavigationBar(
                containerColor = BgBottom,
                tonalElevation = 0.dp,
                modifier       = Modifier.navigationBarsPadding(),
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick  = {
                            if (pagerState.currentPage != index) {
                                scope.launch { pagerState.scrollToPage(index) }
                            }
                        },
                        icon = {
                            Icon(
                                painter            = painterResource(tab.iconRes),
                                contentDescription = null,
                                modifier           = Modifier,
                            )
                        },
                        label  = { Text(tab.label, fontSize = 12.sp) },
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

        // ── Expandable player panel (overlays everything) ─────────────────────
        ExpandablePlayerPanel(
            playerState = playerState,
            onIntent    = onPlayerIntent,
            modifier    = Modifier.fillMaxSize(),
        )
    }
}
