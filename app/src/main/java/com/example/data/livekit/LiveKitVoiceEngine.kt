package com.example.data.livekit

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class LiveKitTokenRequest(
    val roomId: String,
    val participantId: String,
    val participantName: String,
    val canPublish: Boolean
)

@JsonClass(generateAdapter = true)
data class LiveKitTokenResponse(
    val token: String,
    val serverUrl: String? = null
)

interface LiveKitTokenApi {
    @POST("livekit/token")
    suspend fun fetchRoomToken(
        @Header("Authorization") bearerAuth: String,
        @Body request: LiveKitTokenRequest
    ): LiveKitTokenResponse
}

sealed interface VoiceConnectionState {
    data object Disconnected : VoiceConnectionState
    data class Unconfigured(
        val hasServerUrl: Boolean,
        val hasTokenEndpoint: Boolean
    ) : VoiceConnectionState
    data class Connecting(val roomId: String) : VoiceConnectionState
    data class Connected(
        val roomId: String,
        val serverUrl: String,
        val micPublished: Boolean
    ) : VoiceConnectionState
    data class Error(val message: String) : VoiceConnectionState
}

/**
 * Clean LiveKit voice transport boundary.
 *
 * When LIVEKIT_URL and LIVEKIT_TOKEN_ENDPOINT are configured via AI Studio Secrets,
 * this fetches a Firebase-authenticated LiveKit room JWT from the token backend.
 * When unconfigured, it surfaces VoiceConnectionState.Unconfigured honestly without
 * simulating fake voice audio.
 */
class LiveKitVoiceEngine {
    private val _connectionState = MutableStateFlow<VoiceConnectionState>(evaluateConfigState())
    val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private fun validServerUrl(): String = BuildConfig.LIVEKIT_URL.trim().takeIf {
        it.isNotBlank() && !it.equals("UNCONFIGURED", ignoreCase = true) &&
            (it.startsWith("wss://") || it.startsWith("ws://"))
    } ?: ""

    private fun validTokenEndpoint(): String = BuildConfig.LIVEKIT_TOKEN_ENDPOINT.trim().takeIf {
        it.isNotBlank() && !it.equals("UNCONFIGURED", ignoreCase = true) &&
            (it.startsWith("https://") || it.startsWith("http://"))
    } ?: ""

    val isConfigured: Boolean
        get() = validServerUrl().isNotBlank() && validTokenEndpoint().isNotBlank()

    private fun evaluateConfigState(): VoiceConnectionState {
        val hasUrl = validServerUrl().isNotBlank()
        val hasEndpoint = validTokenEndpoint().isNotBlank()
        return if (hasUrl && hasEndpoint) {
            VoiceConnectionState.Disconnected
        } else {
            VoiceConnectionState.Unconfigured(hasServerUrl = hasUrl, hasTokenEndpoint = hasEndpoint)
        }
    }

    suspend fun connectToRoom(
        roomId: String,
        userId: String,
        username: String,
        canPublish: Boolean
    ): Result<Unit> {
        val serverUrl = validServerUrl()
        val endpoint = validTokenEndpoint()
        if (serverUrl.isBlank() || endpoint.isBlank()) {
            _connectionState.value = VoiceConnectionState.Unconfigured(
                hasServerUrl = serverUrl.isNotBlank(),
                hasTokenEndpoint = endpoint.isNotBlank()
            )
            return Result.success(Unit)
        }

        _connectionState.value = VoiceConnectionState.Connecting(roomId)
        return try {
            val idToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                ?: error("Authenticated Firebase ID token required for LiveKit token minting.")

            val normalizedBase = if (endpoint.endsWith("/")) endpoint else "$endpoint/"
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val api = Retrofit.Builder()
                .baseUrl(normalizedBase)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(LiveKitTokenApi::class.java)

            val response = api.fetchRoomToken(
                bearerAuth = "Bearer $idToken",
                request = LiveKitTokenRequest(
                    roomId = roomId,
                    participantId = userId,
                    participantName = username,
                    canPublish = canPublish
                )
            )
            val resolvedUrl = response.serverUrl?.takeIf { it.isNotBlank() } ?: serverUrl
            Log.i(TAG, "LiveKit token minted for room=$roomId server=$resolvedUrl")
            _connectionState.value = VoiceConnectionState.Connected(
                roomId = roomId,
                serverUrl = resolvedUrl,
                micPublished = canPublish
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "LiveKit connection setup failed: ${e.message}")
            _connectionState.value = VoiceConnectionState.Error(
                e.localizedMessage ?: "Unable to connect to LiveKit token server."
            )
            Result.failure(e)
        }
    }

    fun setMicPublished(published: Boolean) {
        val current = _connectionState.value
        if (current is VoiceConnectionState.Connected) {
            _connectionState.value = current.copy(micPublished = published)
        }
    }

    fun disconnect() {
        _connectionState.value = evaluateConfigState()
    }

    private companion object {
        const val TAG = "LiveKitVoiceEngine"
    }
}
