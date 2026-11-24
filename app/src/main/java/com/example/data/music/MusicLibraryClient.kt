package com.example.data.music

import android.content.ContentResolver
import android.provider.OpenableColumns
import android.net.Uri
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val size: Long,
    val contentType: String,
    val uploadedBy: String
)

/**
 * Authenticated client for the VibePlay music Worker.
 * The API base URL is intentionally empty until the Worker is deployed and its URL is known.
 */
class MusicLibraryClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl: String
        get() = BuildConfig.MUSIC_API_BASE_URL.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://")

    private suspend fun authorizedRequest(path: String): Request.Builder {
        if (!isConfigured) throw IllegalStateException(
            "Music storage setup is pending. Deploy the VibePlay music Worker first."
        )
        val token = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("Please sign in again to use the music library.")
        return Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", "Bearer $token")
    }

    suspend fun listTracks(): List<MusicTrack> {
        val request = authorizedRequest("/v1/music").get().build()
        return withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException(errorMessage(body, "Could not load music library."))
                val array = JSONObject(body).optJSONArray("tracks") ?: return@withContext emptyList()
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val id = item.optString("id")
                    if (id.isBlank()) return@mapNotNull null
                    MusicTrack(
                        id = id,
                        title = item.optString("title", "Untitled"),
                        artist = item.optString("artist", "Unknown artist"),
                        size = item.optLong("size"),
                        contentType = item.optString("contentType", "audio/mpeg"),
                        uploadedBy = item.optString("uploadedBy")
                    )
                }
            }
        }
    }

    suspend fun upload(
        resolver: ContentResolver,
        uri: Uri,
        title: String,
        artist: String
    ): MusicTrack {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val detectedMime = resolver.getType(uri)?.substringBefore(';')?.trim()?.lowercase()
            ?: "application/octet-stream"
        val extensionMime = when (extension) {
            "mp3" -> "audio/mpeg"
            "m4a", "mp4" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg", "oga", "opus" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> ""
        }
        // Providers disagree on MIME aliases; normalize common audio types and use the
        // actual filename extension only when the provider reports a generic/unknown type.
        val mime = when (detectedMime) {
            "video/mp4", "application/mp4", "audio/m4a", "audio/x-m4a" -> "audio/mp4"
            "audio/mp3", "audio/x-mp3" -> "audio/mpeg"
            "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "audio/wav"
            "audio/x-flac" -> "audio/flac"
            "audio/x-aac" -> "audio/aac"
            "application/ogg", "audio/opus", "audio/x-opus+ogg" -> "audio/ogg"
            "application/octet-stream", "application/x-unknown", "" -> extensionMime
            else -> detectedMime
        }
        if (mime !in setOf("audio/mpeg", "audio/mp4", "audio/aac", "audio/ogg", "audio/wav", "audio/flac")) {
            throw IllegalArgumentException("Unsupported audio format. Choose MP3, M4A, AAC, OGG, WAV or FLAC.")
        }
        val maxBytes = 15 * 1024 * 1024
        val declaredSize = withContext(Dispatchers.IO) {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else -1L
                } else -1L
            } ?: -1L
        }
        if (declaredSize > maxBytes) {
            throw IllegalArgumentException("Audio file must be 15 MB or smaller.")
        }
        val bytes = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream(
                    if (declaredSize in 1..maxBytes.toLong()) declaredSize.toInt() else 8192
                )
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) {
                        throw IllegalArgumentException("Audio file must be 15 MB or smaller.")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: throw IOException("Could not read the selected audio file.")
        }
        if (bytes.isEmpty()) throw IllegalArgumentException("Selected audio file is empty.")
        val fileName = withContext(Dispatchers.IO) {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameColumn >= 0) cursor.getString(nameColumn) else null
                } else null
            }
        }?.take(120)?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.take(120)
            ?: "audio"
        // Preserve the exact source-device filename, including its extension, as the library title.
        // The optional panel title must not overwrite the original filename.
        val originalFileName = fileName
        val fileBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", originalFileName.take(120))
            .addFormDataPart("artist", artist.take(80))
            .addFormDataPart("file", fileName, fileBody)
            .build()
        val request = authorizedRequest("/v1/music").post(multipart).build()
        return withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException(errorMessage(body, "Music upload failed."))
                val item = JSONObject(body)
                MusicTrack(
                    id = item.optString("id"),
                    title = item.optString("title", title),
                    artist = item.optString("artist", artist),
                    size = item.optLong("size", bytes.size.toLong()),
                    contentType = item.optString("contentType", mime),
                    uploadedBy = item.optString("uploadedBy")
                )
            }
        }
    }

    suspend fun delete(trackId: String) {
        val encoded = URLEncoder.encode(trackId, "UTF-8").replace("+", "%20")
        val request = authorizedRequest("/v1/music/$encoded").delete().build()
        withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException(errorMessage(body, "Could not remove track."))
            }
        }
    }

    suspend fun streamUrl(trackId: String): String {
        val encoded = URLEncoder.encode(trackId, "UTF-8").replace("+", "%20")
        return baseUrl + "/v1/music/$encoded"
    }

    private fun errorMessage(body: String, fallback: String): String =
        runCatching { JSONObject(body).optString("error").takeIf { it.isNotBlank() } }
            .getOrNull() ?: fallback
}
