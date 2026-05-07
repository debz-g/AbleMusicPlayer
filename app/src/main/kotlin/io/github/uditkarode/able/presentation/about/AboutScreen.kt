package io.github.uditkarode.able.presentation.about

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.uditkarode.able.R

private val Bg = Color(0xFF212121)
private val White = Color(0xFFFBFBFB)
private val Gray = Color(0xFF888888)
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
        // ── Back button ───────────────────────────────────────────────────────
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(
                painter = painterResource(R.drawable.down_arrow),
                contentDescription = "Back",
                tint = White,
                modifier = Modifier.size(20.dp),
            )
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // App header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val context = LocalContext.current
                val appIconBitmap = remember {
                    BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
                }
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap.asImageBitmap(),
                        contentDescription = "App icon",
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "AbleMusic",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 0.5.dp)
            Spacer(Modifier.height(20.dp))

            // ── Info section ──────────────────────────────────────────────────
            SectionHeader("Info")
            Spacer(Modifier.height(12.dp))

            InfoItem(label = "Flavour", value = buildType.replaceFirstChar { it.uppercase() })
            Spacer(Modifier.height(12.dp))

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 0.5.dp)
            Spacer(Modifier.height(20.dp))

            // ── Authors section ───────────────────────────────────────────────
            SectionHeader("Authors")
            Spacer(Modifier.height(12.dp))

            AuthorItem(name = "Udit Karode", role = "OG Developer")
            Spacer(Modifier.height(12.dp))
            AuthorItem(name = "Debayan", role = "Developer")
            Spacer(Modifier.height(12.dp))
            AuthorItem(name = "Jayesh Seth", role = "Developer")
            Spacer(Modifier.height(12.dp))
            AuthorItem(name = "Sajid Shaik", role = "Graphics Designer")
            Spacer(Modifier.height(12.dp))
            AuthorItem(name = "Safan Sulfikar", role = "Product Manager")

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Also, huge thanks to the NewPipe team for the extractor.",
                color = Gray,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onOpenTelegram,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.telegram),
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Support on Telegram", color = White, fontSize = 15.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(value, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Gray, fontSize = 12.sp)
    }
}

@Composable
private fun AuthorItem(name: String, role: String) {
    Column {
        Text(name, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(role, color = Gray, fontSize = 12.sp)
    }
}
