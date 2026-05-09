package io.github.uditkarode.able.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Reusable scaffold that wraps any screen content with a mini player bar
 * at the bottom and a full-screen player sheet. Use this in secondary
 * activities (LocalPlaylist, LibraryDetail, AlbumPlaylist, Downloads, etc.)
 * so the player is accessible from everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScaffold(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    content: @Composable (Modifier) -> Unit,
) {
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showPlayerSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier.fillMaxSize()) {
        // Main content fills available space above mini player
        Box(modifier = Modifier.weight(1f)) {
            content(Modifier.fillMaxSize())
        }

        // Mini player bar
        MiniPlayerBar(
            playerState = playerState,
            onIntent = playerViewModel::onIntent,
            onClick = { showPlayerSheet = true },
        )
    }

    // Full-screen player sheet
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
                onIntent = playerViewModel::onIntent,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPlayerSheet = false
                    }
                },
            )
        }
    }
}
