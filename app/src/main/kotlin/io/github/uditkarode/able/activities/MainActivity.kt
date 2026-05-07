/*
    Copyright 2020 Udit Karode <udit.karode@gmail.com>

    This file is part of AbleMusicPlayer.

    AbleMusicPlayer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, version 3 of the License.

    AbleMusicPlayer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with AbleMusicPlayer.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.github.uditkarode.able.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.R
import io.github.uditkarode.able.adapters.ViewPagerAdapter
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.fragments.Home
import io.github.uditkarode.able.fragments.Search
import io.github.uditkarode.able.model.MusicMode
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.main.MainScreen
import io.github.uditkarode.able.presentation.main.MainViewModel
import io.github.uditkarode.able.services.DownloadService
import io.github.uditkarode.able.utils.CustomDownloader
import io.github.uditkarode.able.utils.Shared
import org.schabi.newpipe.extractor.NewPipe
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Entry-point activity. Hosts the Compose navigation shell (mini player + bottom nav)
 * while keeping legacy XML fragments alive in a ViewPager2 via AndroidView.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), Search.SongCallback {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var connection: MusicServiceConnection

    // Kept as a field because sendItem() calls home.streamAudio()
    private val home = Home()

    override fun onCreate(savedInstanceState: Bundle?) {
        NewPipe.init(CustomDownloader.getInstance())
        Shared.cleanupTempFiles()

        // First launch → go to Welcome screen
        if (!getSharedPreferences("able_prefs", MODE_PRIVATE)
                .getBoolean("welcome_shown", false)
        ) {
            super.onCreate(savedInstanceState)
            startActivity(Intent(this, Welcome::class.java))
            finish()
            return
        }

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        super.onCreate(savedInstanceState)

        // Pre-load the default album art bitmap used across the app
        val rawDrawable = ResourcesCompat.getDrawable(resources, R.drawable.def_albart, null)
        if (rawDrawable is BitmapDrawable) {
            val out = ByteArrayOutputStream()
            rawDrawable.bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out)
            val bytes = out.toByteArray()
            Shared.defBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        connection.bind(this)

        setContent {
            MaterialTheme {
                val miniPlayerState by viewModel.miniPlayerState.collectAsState()
                MainScreen(
                    miniPlayerState = miniPlayerState,
                    onPlayPause     = viewModel::playPause,
                    onOpenPlayer    = {
                        startActivity(Intent(this@MainActivity, Player::class.java))
                    },
                    vpSetup = { vp ->
                        vp.adapter = ViewPagerAdapter(this@MainActivity, home)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.unbind(this)
    }

    // ── Search.SongCallback ───────────────────────────────────────────────────

    override fun sendItem(song: Song, mode: String) {
        var currentMode = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("mode_key", MusicMode.download)
        if (mode.isNotEmpty()) currentMode = mode

        song.ytmThumbnail = Shared.upscaleThumbnailUrl(song.ytmThumbnail)

        when (currentMode) {
            MusicMode.download -> {
                if (DownloadService.isAlreadyQueued(song.youtubeLink)) {
                    Toast.makeText(this, "${song.name} is already downloading", Toast.LENGTH_SHORT).show()
                    return
                }
                val extras = arrayListOf(song.name, song.youtubeLink, song.artist, song.ytmThumbnail)
                val dlIntent = Intent(this, DownloadService::class.java)
                    .putStringArrayListExtra("song", extras)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(dlIntent)
                } else {
                    startService(dlIntent)
                }
                Toast.makeText(this, "${song.name} ${getString(R.string.dl_added)}", Toast.LENGTH_SHORT).show()
            }

            MusicMode.stream -> {
                home.streamAudio(song)
            }
        }
    }
}
