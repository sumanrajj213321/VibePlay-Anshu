package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
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
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
@Composable
internal fun RoomMusicPanel(
    roomMusic: RoomMusicState?,
    canControl: Boolean,
    onUpdateRoomMusic: (trackId: String, status: String, positionMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { MusicLibraryClient() }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, false)
        }
    }
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var volume by remember { mutableFloatStateOf(1f) }
    var playerTrackId by remember { mutableStateOf("") }

    DisposableEffect(player) {
        onDispose { player.release() }
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

    LaunchedEffect(Unit) {
        if (client.isConfigured) refreshLibrary()
        else error = "Music storage setup pending: Worker deployment is required."
    }

    LaunchedEffect(
        roomMusic?.trackId,
        roomMusic?.status,
        roomMusic?.positionMs,
        roomMusic?.updatedAt?.seconds,
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
            val mediaSource = DefaultMediaSourceFactory(sourceFactory)
                .createMediaSource(MediaItem.fromUri(client.streamUrl(track.id)))
            player.setMediaSource(mediaSource)
            player.prepare()
            val updatedAtMs = state.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            val elapsed = if (state.status == "PLAYING") {
                (System.currentTimeMillis() - updatedAtMs).coerceAtLeast(0L)
            } else 0L
            player.seekTo((state.positionMs + elapsed).coerceAtLeast(0L))
            if (state.status == "PLAYING") player.play() else player.pause()
            if (state.status == "STOPPED") player.seekTo(0L)
            playerTrackId = track.id
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
                    val fallbackTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                        ?.take(80).orEmpty().ifBlank { "Untitled" }
                    val uploaded = client.upload(
                        context.contentResolver,
                        uri,
                        title.ifBlank { fallbackTitle },
                        artist.ifBlank { "Unknown artist" }
                    )
                    title = ""
                    artist = ""
                    refreshLibrary()
                    onUpdateRoomMusic(uploaded.id, "PLAYING", 0L)
                } catch (e: Exception) {
                    error = e.localizedMessage ?: "Upload failed."
                } finally {
                    loading = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room Music") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    onUpdateRoomMusic(id, "PAUSED", player.currentPosition)
                                    player.pause()
                                } else {
                                    onUpdateRoomMusic(id, "PLAYING", player.currentPosition)
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
                                onUpdateRoomMusic(id, "STOPPED", 0L)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop music")
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
                if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tracks.filter {
                        it.title.contains(search, true) || it.artist.contains(search, true)
                    }, key = { it.id }) { track ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (track.id == roomMusic?.trackId)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                if (canControl) onUpdateRoomMusic(track.id, "PLAYING", 0L)
                            }
                        ) {
                            Column(Modifier.fillMaxWidth().padding(9.dp)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium)
                                Text(track.artist, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Track title (optional)") }
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Artist (optional)") }
                )
                Button(
                    onClick = { picker.launch(arrayOf("audio/*")) },
                    enabled = !loading && client.isConfigured,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Upload audio") }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Upload limit: 15 MB per track. Storage is shared by everyone in this room.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
