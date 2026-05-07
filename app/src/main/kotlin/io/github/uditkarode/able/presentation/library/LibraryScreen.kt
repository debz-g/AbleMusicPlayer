package io.github.uditkarode.able.presentation.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val Bg      = Color(0xFF212121)
private val Surface = Color(0xFF2C2C2C)
private val White   = Color(0xFFFBFBFB)
private val Gray    = Color(0xFF888888)
private val Accent  = Color(0xFF5E92F3)

@Composable
fun LibraryScreen(
    onOpenGroup: (label: String, mode: LibraryMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        ModeToggle(mode = state.mode, onToggle = viewModel::toggleMode)

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = White)
                }

                state.groups.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        text = if (state.mode == LibraryMode.ALBUMS) "No albums found" else "No artists found",
                        color = Gray,
                        fontSize = 14.sp,
                    )
                }

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.groups, key = { it.label }) { group ->
                        GroupRow(
                            group = group,
                            onClick = { onOpenGroup(group.label, state.mode) },
                        )
                        HorizontalDivider(color = Surface, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(mode: LibraryMode, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleChip(label = "Artists", selected = mode == LibraryMode.ARTISTS, onClick = { if (mode != LibraryMode.ARTISTS) onToggle() })
        Spacer(Modifier.width(8.dp))
        ToggleChip(label = "Albums", selected = mode == LibraryMode.ALBUMS, onClick = { if (mode != LibraryMode.ALBUMS) onToggle() })
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) White else Gray,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Accent.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun GroupRow(group: LibraryGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = group.label, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (group.count == 1) "1 song" else "${group.count} songs",
            color = Gray,
            fontSize = 12.sp,
        )
    }
}
