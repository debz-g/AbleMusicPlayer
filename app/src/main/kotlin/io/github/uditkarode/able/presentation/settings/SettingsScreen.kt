package io.github.uditkarode.able.presentation.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import io.github.uditkarode.able.R

private val Bg      = Color(0xFF212121)
private val Surface = Color(0xFF2C2C2C)
private val White   = Color(0xFFFBFBFB)
private val Gray    = Color(0xFF888888)
private val Accent  = Color(0xFF5E92F3)

private const val KEY_SOURCE = "source_key"
private const val KEY_MODE   = "mode_key"

private val sourceOptions = listOf("Youtube Music", "Youtube (Videos)")
private val modeOptions   = listOf("Download", "Stream")

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var sourceValue by remember { mutableStateOf(prefs.getString(KEY_SOURCE, "Youtube Music") ?: "Youtube Music") }
    var modeValue   by remember { mutableStateOf(prefs.getString(KEY_MODE,   "Download")      ?: "Download") }

    var showSourceDialog by remember { mutableStateOf(false) }
    var showModeDialog   by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        // ── Toolbar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Surface)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter           = painterResource(R.drawable.down_arrow),
                    contentDescription = "Back",
                    tint              = White,
                    modifier          = Modifier.size(20.dp),
                )
            }
            Text(
                text       = "Settings",
                color      = White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Preferences ───────────────────────────────────────────────────────
        PrefRow(
            title   = "Search source",
            summary = sourceValue,
            onClick = { showSourceDialog = true },
        )
        HorizontalDivider(color = Surface, thickness = 0.5.dp)

        PrefRow(
            title   = "Play mode",
            summary = modeValue,
            onClick = { showModeDialog = true },
        )
        HorizontalDivider(color = Surface, thickness = 0.5.dp)

        PrefRow(
            title   = "Downloads",
            summary = "View active and queued downloads",
            onClick = onOpenDownloads,
        )
        HorizontalDivider(color = Surface, thickness = 0.5.dp)

        PrefRow(
            title   = "About",
            summary = "App info and credits",
            onClick = onOpenAbout,
        )
        HorizontalDivider(color = Surface, thickness = 0.5.dp)
    }

    if (showSourceDialog) {
        PickerDialog(
            title    = "Search source",
            options  = sourceOptions,
            selected = sourceValue,
            onSelect = { choice ->
                sourceValue = choice
                prefs.edit().putString(KEY_SOURCE, choice).apply()
                showSourceDialog = false
            },
            onDismiss = { showSourceDialog = false },
        )
    }

    if (showModeDialog) {
        PickerDialog(
            title    = "Play mode",
            options  = modeOptions,
            selected = modeValue,
            onSelect = { choice ->
                modeValue = choice
                prefs.edit().putString(KEY_MODE, choice).apply()
                showModeDialog = false
            },
            onDismiss = { showModeDialog = false },
        )
    }
}

@Composable
private fun PrefRow(title: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(title,   color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(summary, color = Gray,  fontSize = 13.sp)
    }
}

@Composable
private fun PickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface,
        title            = { Text(title, color = White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick  = { onSelect(option) },
                            colors   = RadioButtonDefaults.colors(
                                selectedColor   = Accent,
                                unselectedColor = Gray,
                            ),
                        )
                        Text(option, color = White, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton  = {},
        dismissButton  = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Gray) }
        },
    )
}
