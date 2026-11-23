package com.example.data.agora

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AgoraTokenRequest(val roomId: String)

@JsonClass(generateAdapter = true)
data class AgoraTokenResponse(
    val appId: String,
    val channelName: String,
    val uid: Int,
    val token: String,
    val role: String,
    val seatIndex: Int? = null,
    val expiresIn: Int = 3600
)

interface AgoraTokenApi {
    @POST("/")
    suspend fun fetchToken(
        @Header("Authorization") authorization: String,
        @Body request: AgoraTokenRequest
    ): AgoraTokenResponse
}

sealed interface VoiceConnectionState {
    data object Disconnected : VoiceConnectionState
    data class Unconfigured(val hasServerUrl: Boolean, val hasTokenEndpoint: Boolean) : VoiceConnectionState
    data class Connecting(val roomId: String) : VoiceConnectionState
    data class Connected(val roomId: String, val micPublished: Boolean) : VoiceConnectionState
    data class Error(val message: String) : VoiceConnectionState
}

/** Real Agora voice transport. The server, never the client, decides whether this user may publish. */
class AgoraVoiceEngine {
    private val appContext: Context = com.google.firebase.FirebaseApp.getInstance().applicationContext
    private val _connectionState = MutableStateFlow<VoiceConnectionState>(VoiceConnectionState.Disconnected)
    val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private var engine: RtcEngine? = null
    private var tokenApi: AgoraTokenApi? = null
    private var activeRoomId: String? = null
    private var tokenAllowsPublish = false
    private var micShouldPublish = false
    private var joined = false

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            joined = true
            _connectionState.value = VoiceConnectionState.Connected(
                roomId = activeRoomId.orEmpty(),
                micPublished = micShouldPublish && tokenAllowsPublish
            )
            Log.i(TAG, "Joined Agora voice channel")
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora RTC error: $err")
            _connectionState.value = VoiceConnectionState.Error("Agora connection error ($err)")
        }

        override fun onConnectionLost() {
            joined = false
            _connectionState.value = VoiceConnectionState.Error("Voice connection lost. Re-enter the room to reconnect.")
        }
    }

    private fun tokenApi(): AgoraTokenApi {
        tokenApi?.let { return it }
        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl(TOKEN_ENDPOINT)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AgoraTokenApi::class.java)
            .also { tokenApi = it }
    }

    suspend fun connectToRoom(
        roomId: String,
        userId: String,
        username: String,
        canPublish: Boolean
    ): Result<Unit> {
        if (roomId.isBlank()) return Result.failure(IllegalArgumentException("Invalid room ID"))
        if (activeRoomId == roomId && joined) return Result.success(Unit)
        _connectionState.value = VoiceConnectionState.Connecting(roomId)
        activeRoomId = roomId
        return try {
            joinWithFreshServerAuthorization(roomId)
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Agora join failed", error)
            _connectionState.value = VoiceConnectionState.Error(
                error.localizedMessage ?: "Unable to connect to Agora voice."
            )
            Result.failure(error)
        }
    }

    private suspend fun joinWithFreshServerAuthorization(roomId: String) {
        val idToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
            ?: error("Please sign in again to join voice.")
        val response = tokenApi().fetchToken(
            authorization = "Bearer $idToken",
            request = AgoraTokenRequest(roomId)
        )
        require(response.appId.isNotBlank() && response.token.isNotBlank() &&
            response.channelName.isNotBlank() && response.uid > 0) {
            "The voice server returned an invalid Agora token."
        }

        tokenAllowsPublish = response.role == "speaker"
        val hasMicPermission = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        micShouldPublish = tokenAllowsPublish && hasMicPermission

        if (engine == null) {
            val config = RtcEngineConfig().apply {
                mContext = appContext
                mAppId = response.appId
                mEventHandler = eventHandler
            }
            engine = RtcEngine.create(config)
            check(engine?.enableAudio() == 0) { "Agora audio could not be enabled." }
            engine?.setEnableSpeakerphone(true)
        } else if (joined) {
            engine?.leaveChannel()
            joined = false
        }

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            clientRoleType = if (tokenAllowsPublish) {
                Constants.CLIENT_ROLE_BROADCASTER
            } else {
                Constants.CLIENT_ROLE_AUDIENCE
            }
            publishMicrophoneTrack = micShouldPublish
            publishCameraTrack = false
            autoSubscribeAudio = true
            autoSubscribeVideo = false
        }
        val result = engine?.joinChannel(response.token, response.channelName, response.uid, options)
            ?: error("Agora engine could not be initialized.")
        check(result == 0) { "Agora rejected the channel join request ($result)." }
    }

    /** Called from the Firestore seat stream. False always mutes immediately; true requires a fresh server authorization. */
    suspend fun setMicPublished(published: Boolean) {
        if (!published) {
            micShouldPublish = false
            engine?.muteLocalAudioStream(true)

            // A muted/removed/locked seat must lose its server-side publish privilege too.
            // Rejoin as audience after the Firestore seat write has completed.
            if (tokenAllowsPublish) {
                val roomId = activeRoomId
                if (roomId != null) {
                    joinWithFreshServerAuthorization(roomId)
                }
            }

            val current = _connectionState.value
            if (current is VoiceConnectionState.Connected) {
                _connectionState.value = current.copy(micPublished = false)
            }
            return
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            micShouldPublish = false
            engine?.muteLocalAudioStream(true)
            return
        }

        if (!tokenAllowsPublish) {
            val roomId = activeRoomId ?: return
            joinWithFreshServerAuthorization(roomId)
            return
        }

        micShouldPublish = true
        engine?.muteLocalAudioStream(false)
        val current = _connectionState.value
        if (current is VoiceConnectionState.Connected) {
            _connectionState.value = current.copy(micPublished = true)
        }
    }

    fun disconnect() {
        try {
            engine?.leaveChannel()
            RtcEngine.destroy()
        } catch (error: Exception) {
            Log.w(TAG, "Agora cleanup issue", error)
        } finally {
            engine = null
            joined = false
            tokenAllowsPublish = false
            micShouldPublish = false
            activeRoomId = null
            _connectionState.value = VoiceConnectionState.Disconnected
        }
    }

    private companion object {
        const val TAG = "AgoraVoiceEngine"
        const val TOKEN_ENDPOINT = "https://vibeplay-agora-token.sumanrajj213321.workers.dev/"
    }
}
