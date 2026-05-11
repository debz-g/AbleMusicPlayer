package io.github.uditkarode.able.utils

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.services.MusicService
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File

object YouTubeMusicImport {
    private const val TAG = "YTMusicImport"
    private const val MAX_TRACKS = 500
    private const val MAX_PAGES = 50
    /** How many consecutive all-duplicate pages before we stop */
    private const val MAX_EMPTY_PAGES = 5
    /** Minimum ms between notification updates to avoid Android rate-limiting */
    private const val NOTIF_THROTTLE_MS = 500L

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile var isImporting = false
    private var importThread: Thread? = null

    // UI-observable state
    @Volatile var totalTracks: Int = 0
    @Volatile var currentTrackIndex: Int = 0
    @Volatile var currentTrackName: String = ""
    @Volatile var currentTrackStatus: String = ""

    private var lastNotifUpdateMs = 0L

    fun cancelImport() {
        Log.i(TAG, "cancelImport() called")
        isImporting = false
        importThread?.interrupt()
        importThread = null
    }

    /**
     * @return true if at least one song was imported successfully.
     */
    fun importPlaylist(
        playlistUrl: String,
        builder: Notification.Builder,
        context: Context,
    ): Boolean {
        if (isImporting) {
            Log.w(TAG, "Import already in progress, ignoring duplicate request")
            return false
        }
        isImporting = true
        importThread = Thread.currentThread()
        Log.i(TAG, "=== Starting YouTube playlist import ===")
        Log.i(TAG, "Playlist URL: $playlistUrl")
        try {
            Log.i(TAG, "Fetching playlist info via NewPipe extractor...")
            val info: PlaylistInfo
            try {
                info = PlaylistInfo.getInfo(ServiceList.YouTube, playlistUrl)
            } catch (e: Exception) {
                Log.e(TAG, "PlaylistInfo.getInfo() FAILED for URL: $playlistUrl", e)
                Log.e(TAG, "Exception class: ${e.javaClass.name}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.cause?.let { Log.e(TAG, "Caused by: ${it.javaClass.name}: ${it.message}") }
                showFailureNotification(builder, context, "Failed to load playlist: ${e.message}")
                return false
            }

            val playlistName = info.name.ifBlank { "YouTube Playlist" }
            val expectedCount = info.streamCount
            val playlistType = info.playlistType
            Log.i(TAG, "Playlist name: $playlistName")
            Log.i(TAG, "Playlist type: $playlistType, expected stream count: $expectedCount")
            Log.i(TAG, "Initial page item count: ${info.relatedItems.size}")
            val safeName = playlistName.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val fileName = "YouTube: $safeName.json"

            val items = mutableListOf<StreamInfoItem>()
            val seenUrls = mutableSetOf<String>()

            // Collect initial page
            for (item in info.relatedItems) {
                if (item is StreamInfoItem && seenUrls.add(item.url)) {
                    items.add(item)
                } else if (item !is StreamInfoItem) {
                    Log.d(TAG, "Skipping non-stream item: ${item.javaClass.simpleName} - ${item.name}")
                }
            }
            Log.i(TAG, "StreamInfoItems from initial page: ${items.size}")

            // Collect subsequent pages
            // Deduplicated to handle looping playlists (YT Mixes / RDCLAK5uy_ playlists).
            // We allow several consecutive all-duplicate pages before stopping, because
            // mix playlists sometimes return overlapping pages with a few new tracks scattered in.
            var nextPage = info.nextPage
            var pageNum = 1
            var consecutiveEmptyPages = 0
            while (nextPage != null && items.size < MAX_TRACKS && pageNum < MAX_PAGES) {
                if (!isImporting) {
                    Log.i(TAG, "Import cancelled during page fetching at page $pageNum")
                    return false
                }
                pageNum++
                Log.i(TAG, "Fetching page $pageNum...")
                try {
                    val page = PlaylistInfo.getMoreItems(ServiceList.YouTube, playlistUrl, nextPage)
                    val pageStreamItems = page.items.filterIsInstance<StreamInfoItem>()
                    var newCount = 0
                    for (si in pageStreamItems) {
                        if (seenUrls.add(si.url)) {
                            items.add(si)
                            newCount++
                        }
                    }
                    Log.i(TAG, "Page $pageNum: ${pageStreamItems.size} items, $newCount new (total unique: ${items.size})")

                    if (newCount == 0) {
                        consecutiveEmptyPages++
                        if (consecutiveEmptyPages >= MAX_EMPTY_PAGES) {
                            Log.i(TAG, "$MAX_EMPTY_PAGES consecutive pages with no new tracks — stopping pagination")
                            break
                        }
                        // If we know expected count and have them all, stop
                        if (expectedCount > 0 && items.size >= expectedCount) {
                            Log.i(TAG, "Reached expected stream count ($expectedCount) — stopping pagination")
                            break
                        }
                    } else {
                        consecutiveEmptyPages = 0
                        // If we know expected count and have them all, stop
                        if (expectedCount > 0 && items.size >= expectedCount) {
                            Log.i(TAG, "Reached expected stream count ($expectedCount) — stopping pagination")
                            break
                        }
                    }
                    nextPage = page.nextPage
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch page $pageNum", e)
                    nextPage = null
                }
            }
            if (items.size > MAX_TRACKS) {
                Log.i(TAG, "Capping track list from ${items.size} to $MAX_TRACKS")
                while (items.size > MAX_TRACKS) items.removeAt(items.lastIndex)
            }

            Log.i(TAG, "Total tracks found: ${items.size}" +
                    (if (expectedCount > 0) " (expected: $expectedCount)" else ""))

            if (items.isEmpty()) {
                Log.w(TAG, "Playlist is empty — no StreamInfoItems found")
                showFailureNotification(builder, context, "Playlist is empty")
                return false
            }

            totalTracks = items.size
            val songArr = ArrayList<Song>()

            if (!Constants.ableSongDir.exists()) Constants.ableSongDir.mkdirs()
            if (!Constants.albumArtDir.exists()) Constants.albumArtDir.mkdirs()

            for (i in items.indices) {
                if (!isImporting) {
                    Log.i(TAG, "Import cancelled by user at track ${i + 1}")
                    return false
                }

                val item = items[i]
                Log.i(TAG, "--- Track ${i + 1}/${items.size}: ${item.name} ---")
                Log.d(TAG, "  URL: ${item.url}")
                Log.d(TAG, "  Uploader: ${item.uploaderName}")

                val displayName = item.name.run {
                    if (length > 30) substring(0, 30) + "..." else this
                }
                currentTrackIndex = i + 1
                currentTrackName = item.name
                currentTrackStatus = "Downloading…"

                updateNotification(
                    builder, context, displayName,
                    "${i + 1} of ${items.size}",
                    items.size, i, true,
                    force = true,
                )

                try {
                    val song = downloadTrack(item, builder, context, i, items.size)
                    if (song != null) {
                        songArr.add(song)
                        Log.i(TAG, "  ✓ Downloaded: ${song.name} -> ${song.filePath}")
                    } else {
                        Log.w(TAG, "  ✗ downloadTrack returned null for: ${item.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  ✗ Exception downloading: ${item.name}", e)
                    Log.e(TAG, "  Exception class: ${e.javaClass.name}, message: ${e.message}")
                }
            }

            Log.i(TAG, "=== Import complete: ${songArr.size}/${items.size} songs downloaded ===")

            if (songArr.isNotEmpty()) {
                Shared.modifyPlaylist(fileName, songArr)
                Log.i(TAG, "Playlist saved: $fileName")
                return true
            } else {
                Log.w(TAG, "No songs were imported successfully")
                showFailureNotification(builder, context, "Failed to import any songs")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== FATAL: Import failed with unexpected exception ===", e)
            Log.e(TAG, "Exception class: ${e.javaClass.name}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.cause?.let { Log.e(TAG, "Caused by: ${it.javaClass.name}: ${it.message}") }
            showFailureNotification(builder, context, "Failed to load playlist")
            return false
        } finally {
            isImporting = false
            importThread = null
            totalTracks = 0
            currentTrackIndex = 0
            currentTrackName = ""
            currentTrackStatus = ""
            mainHandler.post {
                MusicService.registeredClients.forEach { it.spotifyImportChange(false) }
            }
        }
    }

    private fun downloadTrack(
        item: StreamInfoItem,
        builder: Notification.Builder,
        context: Context,
        trackIndex: Int,
        totalTracks: Int,
    ): Song? {
        val thumbnailUrl = Shared.getBestThumbnail(item.thumbnails, item.url)
        val songId = Shared.getIdFromLink(item.url)
        val sanitizedName = Shared.sanitizeFileName(item.name)
        Log.d(TAG, "  songId=$songId, sanitizedName=$sanitizedName")

        // Skip if already downloaded — check both id-based and title-based names, any extension
        val existingFile = findExistingFile(songId, sanitizedName)
        if (existingFile != null) {
            Log.i(TAG, "  Skipping already downloaded: ${existingFile.name}")
            currentTrackStatus = "Already exists"
            return Song(
                name = item.name,
                artist = item.uploaderName,
                youtubeLink = item.url,
                filePath = existingFile.absolutePath,
                ytmThumbnail = thumbnailUrl,
            )
        }

        val displayName = item.name.run {
            if (length > 30) substring(0, 30) + "..." else this
        }

        // Resolve stream
        Log.d(TAG, "  Resolving stream info for: ${item.url}")
        val streamInfo: StreamInfo
        try {
            streamInfo = StreamInfo.getInfo(item.url)
        } catch (e: Exception) {
            Log.e(TAG, "  StreamInfo.getInfo() FAILED for: ${item.url}", e)
            return null
        }

        Log.d(TAG, "  Audio streams available: ${streamInfo.audioStreams.size}")
        for ((idx, s) in streamInfo.audioStreams.withIndex()) {
            Log.d(TAG, "    [$idx] format=${s.getFormat()?.suffix}, bitrate=${s.averageBitrate}, url=${s.content.take(80)}...")
        }

        val stream = streamInfo.audioStreams
            .filter { it.getFormat()?.suffix?.lowercase() == "m4a" }
            .maxByOrNull { it.averageBitrate }
            ?: streamInfo.audioStreams.maxByOrNull { it.averageBitrate }
            ?: streamInfo.audioStreams.firstOrNull()
        if (stream == null) {
            Log.w(TAG, "  No audio streams for ${item.url}")
            return null
        }
        val url = stream.content
        val ext = stream.getFormat()?.suffix ?: run {
            Log.w(TAG, "  Stream has no format suffix")
            return null
        }
        Log.d(TAG, "  Selected stream: format=$ext, bitrate=${stream.averageBitrate}")

        val tempFile = File(Constants.ableSongDir, "$songId.tmp.$ext")

        currentTrackStatus = "0%"
        updateNotification(
            builder, context, displayName,
            "${trackIndex + 1} of $totalTracks — 0%",
            totalTracks, trackIndex, false,
            force = true,
        )

        Log.d(TAG, "  Downloading to: ${tempFile.absolutePath}")
        try {
            ChunkedDownloader.download(url, tempFile) { progress ->
                currentTrackStatus = "$progress%"
                updateNotification(
                    builder, context, displayName,
                    "${trackIndex + 1} of $totalTracks — $progress%",
                    100, progress, false,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Download FAILED for ${item.name}", e)
            tempFile.delete()
            return null
        }

        Log.d(TAG, "  Download complete, file size: ${tempFile.length()} bytes")

        currentTrackStatus = "Saving…"
        updateNotification(
            builder, context, displayName,
            "${trackIndex + 1} of $totalTracks — saving",
            totalTracks, trackIndex + 1, true,
            force = true,
        )

        val idFile = File(Constants.ableSongDir, "$songId.$ext")
        tempFile.renameTo(idFile)

        // Write metadata tags (may fail for YouTube's fMP4 m4a format — non-fatal)
        runCatching {
            val audioFile = AudioFileIO.read(idFile)
            val tag = audioFile.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, item.name)
            tag.setField(FieldKey.ARTIST, item.uploaderName)
            tag.setField(FieldKey.COMMENT, songId)
            audioFile.commit()
            Log.d(TAG, "  Metadata tags written")
        }.onFailure { Log.w(TAG, "  Tag write skipped (fMP4 format): $songId") }

        val finalFile = Shared.uniqueFile(Constants.ableSongDir, sanitizedName, ext)
        idFile.renameTo(finalFile)
        Log.d(TAG, "  Final file: ${finalFile.absolutePath}")

        // Save album art — try multiple thumbnail resolutions
        if (thumbnailUrl.isNotBlank()) {
            val artSaved = tryLoadThumbnail(context, thumbnailUrl, finalFile)
            if (!artSaved) {
                // If the best thumbnail 404'd, try other available thumbnails
                for (thumb in item.thumbnails) {
                    if (thumb.url != thumbnailUrl && tryLoadThumbnail(context, thumb.url, finalFile)) {
                        break
                    }
                }
            }
        }

        return Song(
            name = item.name,
            artist = item.uploaderName,
            youtubeLink = item.url,
            filePath = finalFile.absolutePath,
            ytmThumbnail = thumbnailUrl,
        )
    }

    /**
     * Look for an existing file by song ID or sanitized title, with any audio extension.
     */
    private fun findExistingFile(songId: String, sanitizedName: String): File? {
        val dir = Constants.ableSongDir
        if (!dir.exists()) return null
        val extensions = listOf("mp3", "m4a", "webm", "ogg", "opus")
        for (ext in extensions) {
            val idFile = File(dir, "$songId.$ext")
            if (idFile.exists()) return idFile
        }
        // Check title-based names (may have (2), (3) suffixes from previous imports)
        for (ext in extensions) {
            val titleFile = File(dir, "$sanitizedName.$ext")
            if (titleFile.exists()) return titleFile
        }
        return null
    }

    /**
     * Try to download and save a thumbnail. Returns true on success.
     */
    private fun tryLoadThumbnail(context: Context, url: String, audioFile: File): Boolean {
        return try {
            val drw = Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit().get()
            if (!Constants.albumArtDir.exists()) Constants.albumArtDir.mkdirs()
            Shared.saveAlbumArtToDisk(
                drw.toBitmap(),
                File(Constants.albumArtDir, audioFile.nameWithoutExtension),
            )
            Log.d(TAG, "  Album art saved from: ${url.take(60)}...")
            true
        } catch (e: Exception) {
            Log.d(TAG, "  Thumbnail failed (${url.take(60)}...): ${e.message}")
            false
        }
    }

    private fun updateNotification(
        builder: Notification.Builder,
        context: Context,
        title: String,
        text: String,
        max: Int,
        progress: Int,
        indeterminate: Boolean,
        force: Boolean = false,
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotifUpdateMs < NOTIF_THROTTLE_MS) return
        lastNotifUpdateMs = now

        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle(title)
            builder.setContentText(text)
            builder.setProgress(max, progress, indeterminate)
            builder.setOngoing(true)
            notify(4, builder.build())
        }
    }

    private fun showFailureNotification(
        builder: Notification.Builder,
        context: Context,
        message: String,
    ) {
        Log.w(TAG, "Showing failure notification: $message")
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle(message)
            builder.setContentText("")
            builder.setProgress(0, 0, false)
            builder.setOngoing(false)
            notify(4, builder.build())
        }
        isImporting = false
    }
}
