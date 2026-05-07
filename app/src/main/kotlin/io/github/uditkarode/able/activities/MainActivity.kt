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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.R
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.model.MusicMode
import io.github.uditkarode.able.presentation.main.MainScreen
import io.github.uditkarode.able.presentation.main.MainViewModel
import io.github.uditkarode.able.services.DownloadService
import io.github.uditkarode.able.utils.CustomDownloader
import io.github.uditkarode.able.utils.Shared
import org.schabi.newpipe.extractor.NewPipe
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var connection: MusicServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        NewPipe.init(CustomDownloader.getInstance())
        Shared.cleanupTempFiles()

        if (!getSharedPreferences("able_prefs", MODE_PRIVATE)
                .getBoolean("welcome_shown", false)
        ) {
            super.onCreate(savedInstanceState)
            startActivity(Intent(this, Welcome::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        super.onCreate(savedInstanceState)

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
                    onOpenSettings  = {
                        startActivity(Intent(this@MainActivity, Settings::class.java))
                    },
                    onSendItem      = { song, mode -> sendItem(song, mode) },
                    onOpenGroup     = { title, songs ->
                        LibraryDetail.pendingSongs = ArrayList(songs)
                        startActivity(
                            Intent(this@MainActivity, LibraryDetail::class.java)
                                .putExtra("title", title)
                        )
                    },
                    onOpenPlaylist  = { name ->
                        startActivity(
                            Intent(this@MainActivity, LocalPlaylist::class.java)
                                .putExtra("name", name)
                        )
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.unbind(this)
    }

    private fun sendItem(song: io.github.uditkarode.able.model.song.Song, mode: String) {
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
                viewModel.streamAudio(song)
            }
        }
    }
}
