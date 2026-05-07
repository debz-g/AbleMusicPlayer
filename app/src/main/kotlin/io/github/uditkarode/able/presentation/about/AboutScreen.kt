package io.github.uditkarode.able.presentation.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.uditkarode.able.R

private val Bg     = Color(0xFF212121)
private val White  = Color(0xFFFBFBFB)
private val Gray   = Color(0xFF888888)
private val Accent = Color(0xFF5E92F3)

@Composable
fun AboutScreen(
    buildType: String,
    onBack: () -> Unit,
    onOpenTelegram: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_home_black_24dp),
                contentDescription = "Back",
                tint = Gray,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note_black_24dp),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("AbleMusic", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildType.replaceFirstChar { it.uppercase() },
                color = Gray,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = "Open-source Android music player powered by YouTube Music.",
                color = Gray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onOpenTelegram,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("Support on Telegram", color = White)
            }
        }
    }
}
