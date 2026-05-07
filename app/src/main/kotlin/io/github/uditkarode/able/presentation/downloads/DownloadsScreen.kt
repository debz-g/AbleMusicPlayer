package io.github.uditkarode.able.presentation.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uditkarode.able.R

private val Bg      = Color(0xFF212121)
private val Surface = Color(0xFF2C2C2C)
private val White   = Color(0xFFFBFBFB)
private val Gray    = Color(0xFF888888)
private val Accent  = Color(0xFF5E92F3)
private val Green   = Color(0xFF1DB954)

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_black_24dp),
                    contentDescription = "Back",
                    tint = Gray,
                )
            }
            Text("Downloads", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        // Spotify import section
        state.spotify?.let { spotify ->
            SpotifyProgressCard(spotify)
            HorizontalDivider(color = Surface, thickness = 1.dp)
        }

        // Download queue
        if (state.items.isEmpty() && state.spotify == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No active downloads", color = Gray, fontSize = 14.sp)
            }
        } else if (state.items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text("No active downloads", color = Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.items, key = { "${it.name}_${it.artist}" }) { item ->
                    DownloadItemRow(item)
                    HorizontalDivider(color = Surface, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SpotifyProgressCard(spotify: SpotifyProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2A1A))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_spot),
                contentDescription = null,
                tint = Green,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Spotify Import", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(spotify.trackName, color = White, fontSize = 14.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            "Track ${spotify.trackIndex} of ${spotify.totalTracks} — ${spotify.trackStatus}",
            color = Gray,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        val total = spotify.totalTracks
        val songPercent = spotify.trackStatus.removeSuffix("%").toIntOrNull() ?: 0
        val progress = ((spotify.trackIndex - 1) * 100 + songPercent).toFloat() / (total * 100)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = Green,
            trackColor = Surface,
        )
    }
}

@Composable
private fun DownloadItemRow(item: DownloadItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(item.artist, color = Gray, fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.status,
            color = if (item.isActive) Accent else Gray.copy(alpha = 0.5f),
            fontSize = 12.sp,
        )
    }
}
