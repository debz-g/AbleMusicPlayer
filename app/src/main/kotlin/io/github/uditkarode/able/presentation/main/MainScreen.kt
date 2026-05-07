package io.github.uditkarode.able.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import io.github.uditkarode.able.R
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.home.HomeScreen
import io.github.uditkarode.able.presentation.library.LibraryScreen
import io.github.uditkarode.able.presentation.player.MiniPlayerBar
import io.github.uditkarode.able.presentation.player.PlayerIntent
import io.github.uditkarode.able.presentation.player.PlayerScreen
import io.github.uditkarode.able.presentation.player.PlayerState
import io.github.uditkarode.able.presentation.playlists.PlaylistsScreen
import io.github.uditkarode.able.presentation.search.SearchScreen
import kotlinx.coroutines.launch

private data class NavTab(val label: String, val iconRes: Int)

private val tabs = listOf(
    NavTab("Home", R.drawable.ic_home_black_24dp),
    NavTab("Search", R.drawable.search),
    NavTab("Library", R.drawable.ic_music_note_black_24dp),
    NavTab("Playlists", R.drawable.ic_library_music_black_24dp),
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var showPlayerSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
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

            MiniPlayerBar(
                playerState = playerState,
                onIntent = onPlayerIntent,
                onClick = { showPlayerSheet = true },
            )

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                scope.launch { pagerState.scrollToPage(index) }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    }

    if (showPlayerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null,
            shape = RectangleShape,
        ) {
            PlayerScreen(
                state = playerState,
                onIntent = onPlayerIntent,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPlayerSheet = false
                    }
                },
            )
        }
    }
}
