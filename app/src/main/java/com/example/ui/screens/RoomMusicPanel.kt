package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.RoomMusicState
import com.example.data.music.MusicLibraryClient
import com.example.data.music.MusicTrack
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(UnstableApi::class)
@Composable
internal fun RoomMusicPanel(
    visible: Boolean,
    roomMusic: RoomMusicState?,
    canControl: Boolean,
    canUpload: Boolean,
    onUpdateRoomMusic: (trackId: String, status: String, positionMs: Long, queueTrackIds: List<String>) -> Unit,
    onAddTrackToQueue: (trackId: String) -> Unit,
    onAdvanceQueue: (expectedTrackId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val client = remember { MusicLibraryClient() }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }
    }
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var volume by remember { mutableFloatStateOf(1f) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Advance the shared queue when the current track finishes. Only a room music
    // controller writes the next shared state, so listeners do not race one another.
    DisposableEffect(player, roomMusic?.trackId, roomMusic?.queueTrackIds, canControl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (
                    playbackState == Player.STATE_ENDED &&
                    canControl
                ) {
                    val finishedTrackId = roomMusic?.trackId.orEmpty()
                    if (finishedTrackId.isNotBlank()) onAdvanceQueue(finishedTrackId)
                }
            }

            override fun onPlayerError(playbackError: androidx.media3.common.PlaybackException) {
                error = playbackError.localizedMessage ?: "This audio file could not be played."
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    suspend fun refreshLibrary() {
        loading = true
        error = null
        try {
            tracks = client.listTracks()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Could not load music."
        } finally {
            loading = false
        }
    }

    // Every room member loads the shared catalogue on room entry, not only when opening the panel.
    LaunchedEffect(Unit) {
        if (client.isConfigured) refreshLibrary()
        else error = "Music storage setup pending: Worker deployment is required."
    }

    LaunchedEffect(visible) {
        if (visible && client.isConfigured) refreshLibrary()
    }

    LaunchedEffect(
        roomMusic?.trackId,
        roomMusic?.status,
        roomMusic?.positionMs,
        roomMusic?.updatedAt,
        roomMusic?.updatedBy,
        tracks
    ) {
        val state = roomMusic ?: return@LaunchedEffect
        val track = tracks.firstOrNull { it.id == state.trackId } ?: return@LaunchedEffect
        if (!client.isConfigured) return@LaunchedEffect
        try {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                ?: throw IllegalStateException("Please sign in again.")
            val sourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            val mediaItem = MediaItem.Builder()
                .setUri(client.streamUrl(track.id))
                .setMimeType(track.contentType)
                .build()
            val mediaSource = DefaultMediaSourceFactory(sourceFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            val updatedAtMs = state.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            val elapsed = if (state.status == "PLAYING") {
                (System.currentTimeMillis() - updatedAtMs).coerceAtLeast(0L)
            } else 0L
            player.seekTo((state.positionMs + elapsed).coerceAtLeast(0L))
            if (state.status == "PLAYING") player.play() else player.pause()
            if (state.status == "STOPPED") player.seekTo(0L)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Could not start room music."
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                loading = true
                error = null
                try {
                    val fallbackTitle = context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameColumn >= 0) cursor.getString(nameColumn) else null
                        } else null
                    }?.take(120)?.takeIf { it.isNotBlank() }
                        ?: uri.lastPathSegment?.substringAfterLast('/')?.take(120)
                        ?: "Untitled"
                    val uploaded = client.upload(
                        context.contentResolver,
                        uri,
                        fallbackTitle,
                        "Unknown artist"
                    )
                    refreshLibrary()
                    onUpdateRoomMusic(
                        uploaded.id,
                        "PLAYING",
                        0L,
                        roomMusic?.queueTrackIds.orEmpty()
                    )
                } catch (e: Exception) {
                    error = e.localizedMessage ?: "Upload failed."
                } finally {
                    loading = false
                }
            }
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room Music") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = (configuration.screenHeightDp * 0.62f).dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        roomMusic?.status == "PLAYING" -> "Now playing"
                        roomMusic?.status == "PAUSED" -> "Paused"
                        else -> "Shared music library"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tracks.firstOrNull { it.id == roomMusic?.trackId }
                        ?.let { "${it.title} · ${it.artist}" } ?: "No track selected",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        enabled = canControl && roomMusic?.trackId?.isNotBlank() == true,
                        onClick = {
                            val id = roomMusic?.trackId.orEmpty()
                            if (id.isNotBlank()) {
                                if (roomMusic?.status == "PLAYING") {
                                    onUpdateRoomMusic(id, "PAUSED", player.currentPosition, roomMusic?.queueTrackIds.orEmpty())
                                    player.pause()
                                } else {
                                    onUpdateRoomMusic(id, "PLAYING", player.currentPosition, roomMusic?.queueTrackIds.orEmpty())
                                    player.play()
                                }
                            }
                        }
                    ) {
                        Icon(
                            if (roomMusic?.status == "PLAYING") Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play or pause"
                        )
                    }
                    IconButton(
                        enabled = canControl && roomMusic?.trackId?.isNotBlank() == true,
                        onClick = {
                            val id = roomMusic?.trackId.orEmpty()
                            if (id.isNotBlank()) {
                                player.pause()
                                player.seekTo(0L)
                                onUpdateRoomMusic(id, "STOPPED", 0L, roomMusic?.queueTrackIds.orEmpty())
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop music")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canControl && !roomMusic?.queueTrackIds.isNullOrEmpty(),
                        onClick = {
                            val queue = roomMusic?.queueTrackIds.orEmpty()
                            val nextId = queue.firstOrNull() ?: return@Button
                            onUpdateRoomMusic(nextId, "PLAYING", 0L, queue.drop(1))
                        }
                    ) { Text("Next in queue") }
                }
                Text("Queue · ${roomMusic?.queueTrackIds?.size ?: 0}")
                roomMusic?.queueTrackIds?.forEachIndexed { index, id ->
                    val queued = tracks.firstOrNull { it.id == id }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}. ${queued?.title ?: "Track"}", style = MaterialTheme.typography.labelSmall)
                        if (canControl) {
                            TextButton(onClick = {
                                val queue = roomMusic?.queueTrackIds.orEmpty()
                                onUpdateRoomMusic(
                                    roomMusic?.trackId.orEmpty(),
                                    roomMusic?.status ?: "STOPPED",
                                    player.currentPosition.coerceAtLeast(0L),
                                    queue.filterIndexed { itemIndex, _ -> itemIndex != index }
                                )
                            }) { Text("Remove") }
                        }
                    }
                }
                Text("Volume")
                Slider(value = volume, onValueChange = {
                    volume = it
                    player.volume = it
                })
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search music") }
                )
                if (canUpload) {
                    Button(
                        onClick = { picker.launch(arrayOf("audio/*", "video/mp4", "application/mp4")) },
                        enabled = !loading && client.isConfigured,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Upload audio") }
                } else {
                    Text(
                        "Only users sitting on a speaker seat can upload songs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { scope.launch { refreshLibrary() } },
                    enabled = !loading && client.isConfigured
                ) { Text("Refresh shared library") }
                if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tracks.filter {
                        it.title.contains(search, true) || it.artist.contains(search, true)
                    }.forEach { track ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (track.id == roomMusic?.trackId)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                if (canControl) onUpdateRoomMusic(track.id, "PLAYING", 0L, roomMusic?.queueTrackIds.orEmpty())
                            }
                        ) {
                            Column(Modifier.fillMaxWidth().padding(9.dp)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (canUpload) {
                                        TextButton(
                                            onClick = { onAddTrackToQueue(track.id) },
                                            enabled = canControl
                                        ) { Text("Play now") }
                                    }
                                    if (track.uploadedBy == FirebaseAuth.getInstance().currentUser?.uid) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                loading = true
                                                error = null
                                                try {
                                                    client.delete(track.id)
                                                    refreshLibrary()
                                                } catch (e: Exception) {
                                                    error = e.localizedMessage ?: "Could not delete your upload."
                                                } finally {
                                                    loading = false
                                                }
                                            }
                                        }, enabled = !loading) { Text("Delete my upload") }
                                    }
                                }
                            }
                        }
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Upload limit: 15 MB per track. This music library is shared across all VibePlay rooms.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
