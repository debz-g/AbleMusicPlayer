package io.github.uditkarode.able.presentation.welcome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.uditkarode.able.R

private val Bg      = Color(0xFF212121)
private val Surface = Color(0xFF2C2C2C)
private val White   = Color(0xFFFBFBFB)
private val Gray    = Color(0xFF888888)
private val Accent  = Color(0xFF5E92F3)
private val Green   = Color(0xFF4CAF50)

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    fun isNotifGranted() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    fun isAudioGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED

    var notifGranted by remember { mutableStateOf(isNotifGranted()) }
    var audioGranted by remember { mutableStateOf(isAudioGranted()) }

    // Re-check on every resume (user may have granted via system settings)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = isNotifGranted()
                audioGranted = isAudioGranted()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifGranted = isNotifGranted()
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        audioGranted = isAudioGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note_black_24dp),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Welcome to AbleMusic",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A few permissions are needed for the best experience.",
            color = Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        PermissionRow(
            label = "Notifications",
            description = "Show playback controls in the notification bar",
            granted = notifGranted,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        HorizontalDivider(color = Surface, modifier = Modifier.padding(vertical = 12.dp))
        PermissionRow(
            label = "Audio files",
            description = "Read local music from your device",
            granted = audioGranted,
            onRequest = { audioLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO) },
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text("Continue", color = White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Gray, fontSize = 12.sp)
        }
        if (granted) {
            Text(
                text = "Granted",
                color = Green,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Green.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Grant", color = White, fontSize = 13.sp)
            }
        }
    }
}
