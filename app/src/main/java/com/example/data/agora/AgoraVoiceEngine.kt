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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    // joinChannel() is asynchronous; keep duplicate seat updates from issuing a second join.
    @Volatile private var joinInProgress = false
    private var localAgoraUid = 0
    private val _activeSpeakerUids = MutableStateFlow<Set<Int>>(emptySet())
    val activeSpeakerUids: StateFlow<Set<Int>> = _activeSpeakerUids.asStateFlow()
    private var localIsSpeaking = false
    private var remoteSpeakingUids: Set<Int> = emptySet()
    private val renewalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val renewalMutex = Mutex()
    // Serialize seat/mic changes so duplicate Firestore and UI callbacks cannot race channel joins.
    private val voiceOperationMutex = Mutex()

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            joinInProgress = false
            joined = true
            _connectionState.value = VoiceConnectionState.Connected(
                roomId = activeRoomId.orEmpty(),
                micPublished = micShouldPublish && tokenAllowsPublish
            )
            Log.i(TAG, "Joined Agora voice channel")
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            // Agora reports the local user with uid 0 in volume indications.
            // Map that entry to the server-assigned UID used by room seat rendering.
            val active = speakers.orEmpty()
                .filter { it.volume >= SPEAKING_VOLUME_THRESHOLD }
                .map { info -> if (info.uid == 0) localAgoraUid else info.uid }
                .filter { it > 0 }
                .toSet()
            remoteSpeakingUids = active - localAgoraUid
            localIsSpeaking = localAgoraUid in active
            _activeSpeakerUids.value = active
        }

        override fun onError(err: Int) {
            joinInProgress = false
            Log.e(TAG, "Agora RTC error: $err")
            _connectionState.value = VoiceConnectionState.Error("Agora connection error ($err)")
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            renewChannelToken()
        }

        override fun onRequestToken() {
            renewChannelToken()
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            remoteSpeakingUids = remoteSpeakingUids - uid
            _activeSpeakerUids.value = remoteSpeakingUids +
                if (localIsSpeaking && localAgoraUid > 0) setOf(localAgoraUid) else emptySet()
        }

        override fun onConnectionLost() {
            joinInProgress = false
            joined = false
            localIsSpeaking = false
            remoteSpeakingUids = emptySet()
            _activeSpeakerUids.value = emptySet()
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
    ): Result<Unit> = voiceOperationMutex.withLock {
        if (roomId.isBlank()) return@withLock Result.failure(IllegalArgumentException("Invalid room ID"))
        if (activeRoomId == roomId && joined) return@withLock Result.success(Unit)
        _connectionState.value = VoiceConnectionState.Connecting(roomId)
        activeRoomId = roomId
        try {
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

    /** Refreshes the signed token before expiry without leaving the live audio channel. */
    private fun renewChannelToken() {
        val roomId = activeRoomId ?: return
        renewalScope.launch {
            renewalMutex.withLock {
                try {
                    val idToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                        ?: error("Please sign in again to renew voice access.")
                    val response = tokenApi().fetchToken(
                        authorization = "Bearer $idToken",
                        request = AgoraTokenRequest(roomId)
                    )
                    require(response.token.isNotBlank() && response.channelName == "vp_$roomId" && response.uid > 0) {
                        "Voice server returned an invalid renewal token."
                    }

                    // A delayed network response must never affect a room that has already been left.
                    if (activeRoomId != roomId || engine == null || !joined) return@withLock

                    // Re-check server authorization, but preserve the user's existing mic-mute choice.
                    tokenAllowsPublish = response.role == "speaker"
                    if (!tokenAllowsPublish) {
                        micShouldPublish = false
                        engine?.muteLocalAudioStream(true)
                    } else if (!micShouldPublish) {
                        engine?.muteLocalAudioStream(true)
                    }

                    val result = engine?.renewToken(response.token) ?: -1
                    check(result == 0) { "Agora token renewal failed ($result)." }
                    Log.i(TAG, "Agora channel token renewed")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "Agora token renewal failed", error)
                    if (activeRoomId == roomId) {
                        _connectionState.value = VoiceConnectionState.Error(
                            error.localizedMessage ?: "Voice authorization expired. Re-enter the room."
                        )
                    }
                }
            }
        }
    }

    private suspend fun joinWithFreshServerAuthorization(roomId: String) {
        // Agora accepts only one outstanding join per engine. Seat streams can emit
        // while the initial asynchronous join is still pending.
        if (joinInProgress && activeRoomId == roomId) {
            Log.d(TAG, "Skipping duplicate Agora join while the current join is pending")
            return
        }
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
        localAgoraUid = response.uid
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
            engine?.enableAudioVolumeIndication(VOLUME_INDICATION_INTERVAL_MS, VOLUME_INDICATION_SMOOTHING, true)
            engine?.setEnableSpeakerphone(true)
        } else if (joined) {
            // A room-role change starts a fresh channel session; discard old speaking indicators.
            engine?.leaveChannel()
            joined = false
            localIsSpeaking = false
            remoteSpeakingUids = emptySet()
            _activeSpeakerUids.value = emptySet()
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
        val targetEngine = engine ?: error("Agora engine could not be initialized.")
        joinInProgress = true
        val result = targetEngine.joinChannel(response.token, response.channelName, response.uid, options)
        if (result != 0) {
            joinInProgress = false
            Log.e(TAG, "Agora rejected channel join request: $result (room=$roomId)")
            _connectionState.value = VoiceConnectionState.Error(
                "Agora could not join the voice room (error $result). Please try again."
            )
        }
    }

    /** Called from the Firestore seat stream. False always mutes immediately; true requires a fresh server authorization. */
    suspend fun setMicPublished(published: Boolean) = voiceOperationMutex.withLock {
        try {
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
            return@withLock
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            micShouldPublish = false
            engine?.muteLocalAudioStream(true)
            return@withLock
        }

        if (!tokenAllowsPublish) {
            val roomId = activeRoomId ?: return@withLock
            joinWithFreshServerAuthorization(roomId)
            return@withLock
        }

        micShouldPublish = true
        engine?.muteLocalAudioStream(false)
        val current = _connectionState.value
        if (current is VoiceConnectionState.Connected) {
            _connectionState.value = current.copy(micPublished = true)
        }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to update Agora microphone state", error)
            _connectionState.value = VoiceConnectionState.Error(
                error.localizedMessage ?: "Unable to update voice connection. Please try again."
            )
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
            joinInProgress = false
            tokenAllowsPublish = false
            micShouldPublish = false
            localAgoraUid = 0
            _activeSpeakerUids.value = emptySet()
            localIsSpeaking = false
            remoteSpeakingUids = emptySet()
            activeRoomId = null
            _connectionState.value = VoiceConnectionState.Disconnected
        }
    }

    private companion object {
        const val TAG = "AgoraVoiceEngine"
        const val VOLUME_INDICATION_INTERVAL_MS = 200
        const val VOLUME_INDICATION_SMOOTHING = 3
        const val SPEAKING_VOLUME_THRESHOLD = 12
        const val TOKEN_ENDPOINT = "https://vibeplay-agora-token.sumanrajj213321.workers.dev/"
    }
}
