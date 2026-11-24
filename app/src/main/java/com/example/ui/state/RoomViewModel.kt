package com.example.ui.state

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.agora.AgoraVoiceEngine
import com.example.data.agora.VoiceConnectionState
import com.example.data.model.GiftBurstEvent
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftTransaction
import com.example.data.model.RoomMusicState
import com.example.data.model.RoomListener
import com.example.data.model.RoomMessage
import com.example.data.model.RoomOnlineMember
import com.example.data.model.SpeakerRequest
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.example.data.repository.RoomRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class RoomViewModel(
    private val roomId: String,
    private val currentUserId: String,
    private val roomRepository: RoomRepository,
    private val voiceEngine: AgoraVoiceEngine = AgoraVoiceEngine()
) : ViewModel() {


    private val _roomSessionRemoved = MutableStateFlow(false)
    val roomSessionRemoved: StateFlow<Boolean> = _roomSessionRemoved.asStateFlow()

    val roomMusic: StateFlow<RoomMusicState?> = roomRepository.observeRoomMusic(roomId)
        .catch { error ->
            Log.w(TAG, "Room music sync failed", unwrapFirestoreError(error))
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    fun updateRoomMusic(trackId: String, status: String, positionMs: Long = 0L, queueTrackIds: List<String> = emptyList()) {
        viewModelScope.launch {
            roomRepository.updateRoomMusic(roomId, trackId, status, positionMs, queueTrackIds)
                .onFailure { error -> _roomError.value = error.localizedMessage ?: "Music control failed." }
        }
    }

    fun addTrackToRoomMusicQueue(trackId: String) {
        viewModelScope.launch {
            roomRepository.addTrackToRoomMusicQueue(roomId, trackId)
                .onFailure { error -> _roomError.value = error.localizedMessage ?: "Could not add song to queue." }
        }
    }

    fun enqueueTrackToRoomMusicQueue(trackId: String) {
        viewModelScope.launch {
            roomRepository.enqueueTrackToRoomMusicQueue(roomId, trackId)
                .onFailure { error -> _roomError.value = error.localizedMessage ?: "Could not add song to queue." }
        }
    }

    fun advanceRoomMusicQueue(expectedTrackId: String) {
        viewModelScope.launch {
            roomRepository.advanceRoomMusicQueue(roomId, expectedTrackId)
                .onFailure { error -> _roomError.value = error.localizedMessage ?: "Could not advance room music queue." }
        }
    }

    private val _roomError = MutableStateFlow<String?>(null)
    val roomError: StateFlow<String?> = _roomError.asStateFlow()

    private val _giftBurst = MutableStateFlow<GiftBurstEvent?>(null)
    val giftBurst: StateFlow<GiftBurstEvent?> = _giftBurst.asStateFlow()
    private val seenGiftMessageIds = mutableSetOf<String>()

    private val _vipEntrance = MutableStateFlow<UserProfile?>(null)
    val vipEntrance: StateFlow<UserProfile?> = _vipEntrance.asStateFlow()

    private val _knownGifts = MutableStateFlow(VibeCatalogDefaults.defaultGifts)
    private val announcedVipIds = mutableSetOf<String>()

    val voiceConnectionState: StateFlow<VoiceConnectionState> = voiceEngine.connectionState
    val activeSpeakerUids: StateFlow<Set<Int>> = voiceEngine.activeSpeakerUids

    val room: StateFlow<VoiceRoom?> = roomRepository.observeRoom(roomId)
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing room", root)
            _roomError.value = root.message
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val rawSeats: StateFlow<List<VoiceSeat>> = roomRepository.observeNineSeats(roomId)
        .catch { error ->
            unwrapFirestoreError(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val voiceStage: StateFlow<VoiceStage?> = combine(room, rawSeats) { currentRoom, seats ->
        currentRoom?.let { VoiceStage.buildStrictNineSeatStage(it, seats) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _roomGiftTransactions = MutableStateFlow<List<GiftTransaction>>(emptyList())
    val roomGiftTransactions: StateFlow<List<GiftTransaction>> = _roomGiftTransactions.asStateFlow()

    private val _messages = MutableStateFlow<List<RoomMessage>>(emptyList())
    val messages: StateFlow<List<RoomMessage>> = _messages.asStateFlow()

    val listeners: StateFlow<List<RoomListener>> = roomRepository.observeListeners(roomId)
        .catch { error ->
            unwrapFirestoreError(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val onlineUsers: StateFlow<List<RoomOnlineMember>> =
        combine(room, voiceStage, listeners) { currentRoom, stage, currentListeners ->
            if (currentRoom == null || stage == null) {
                emptyList()
            } else {
                VoiceStage.buildOnlineMembersList(currentRoom, stage, currentListeners)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val mySeatInvitation: StateFlow<SpeakerRequest?> =
        roomRepository.observeSeatInvitation(roomId, currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private var roomGiftTransactionsJob: Job? = null
    private var roomMessagesJob: Job? = null
    private var roomJoinJob: Job? = null
    private var roomSessionJob: Job? = null
    private var roomHeartbeatJob: Job? = null
    private var joinedListenerDone = false
    private var roomJoinInProgress = false
    private var lastShownGiftMsgId: String? = null

    init {
        viewModelScope.launch {
            rawSeats.collect { seats ->
                val mySeat = seats.firstOrNull { it.occupantId == currentUserId }
                val maySpeak = mySeat != null && !mySeat.muted && !mySeat.seatLocked
                voiceEngine.setMicPublished(maySpeak)
            }
        }
        viewModelScope.launch {
            messages.collect { msgs ->
                val unseen = msgs.filter { it.type == "GIFT" && it.id.isNotBlank() && seenGiftMessageIds.add(it.id) }
                unseen.forEach { giftMessage ->
                    val event = GiftBurstEvent(
                        id = giftMessage.id,
                        senderName = giftMessage.senderName,
                        recipientName = giftMessage.recipientName,
                        giftName = giftMessage.giftName,
                        coinCost = giftMessage.giftCoins,
                        giftId = giftMessage.giftId,
                        quantity = giftMessage.giftQuantity.coerceIn(1L, 999L)
                    )
                    // Replace any currently playing gift so this new gift starts without queue wait.
                    _giftBurst.value = event
                }
            }
        }
        viewModelScope.launch {
            combine(voiceStage, listeners) { currentStage, currentListeners ->
                val vipSeated = currentStage?.let { s ->
                    (listOf(s.hostSeat) + s.speakerSeats)
                        .firstOrNull { it.isOccupied && it.occupantVipLevel > 0L && it.occupantId !in announcedVipIds }
                }
                if (vipSeated != null) {
                    UserProfile(
                        userId = vipSeated.occupantId,
                        username = vipSeated.occupantName,
                        avatarUrl = vipSeated.occupantAvatarUrl,
                        vipLevel = vipSeated.occupantVipLevel
                    )
                } else {
                    val vipListener = currentListeners.firstOrNull {
                        it.vipLevel > 0L && it.userId !in announcedVipIds
                    }
                    vipListener?.let {
                        UserProfile(
                            userId = it.userId,
                            username = it.username,
                            avatarUrl = it.avatarUrl,
                            vipLevel = it.vipLevel
                        )
                    }
                }
            }.collect { newcomer ->
                if (newcomer != null && announcedVipIds.add(newcomer.userId)) {
                    _vipEntrance.value = newcomer
                }
            }
        }
    }

    fun updateGiftsCatalog(catalog: List<GiftCatalogItem>) {
        if (catalog.isNotEmpty()) {
            _knownGifts.value = catalog
        }
    }

    fun onRoomLoaded(loadedRoom: VoiceRoom, currentUser: UserProfile) {
        if (joinedListenerDone || roomJoinInProgress) return

        // Clear any previous visit's chat before starting a new room session.
        // The repository then reads this visit's fresh room_sessions/{uid}.joinedAt
        // and only subscribes to messages from that boundary onward.
        roomJoinInProgress = true
        _messages.value = emptyList()
        _giftBurst.value = null
        seenGiftMessageIds.clear()
        roomMessagesJob?.cancel()
        roomMessagesJob = null

        roomJoinJob = viewModelScope.launch {
            roomRepository.joinAsListener(loadedRoom, currentUser)
                .onSuccess {
                    joinedListenerDone = true
                    roomHeartbeatJob?.cancel()
                    roomHeartbeatJob = launch {
                        while (joinedListenerDone) {
                            roomRepository.heartbeatRoomSession(roomId, currentUserId)
                                .onFailure { error ->
                                    Log.w(TAG, "Room presence heartbeat failed", unwrapFirestoreError(error))
                                }
                            delay(25_000L)
                        }
                    }
                    roomSessionJob?.cancel()
                    roomSessionJob = launch {
                        roomRepository.observeRoomSession(roomId, currentUserId).collect { exists ->
                            if (exists == false && joinedListenerDone) {
                                _roomSessionRemoved.value = true
                                roomHeartbeatJob?.cancel()
                                voiceEngine.disconnect()
                                roomMessagesJob?.cancel()
                                roomGiftTransactionsJob?.cancel()
                                _messages.value = emptyList()
                                _giftBurst.value = null
                                _roomGiftTransactions.value = emptyList()
                            }
                        }
                    }
                    roomGiftTransactionsJob?.cancel()
                    roomGiftTransactionsJob = launch {
                        roomRepository.observeRoomGiftTransactions(roomId)
                            .catch { error ->
                                Log.w(TAG, "Room gift rankings stream failed", unwrapFirestoreError(error))
                                emit(emptyList())
                            }
                            .collect { _roomGiftTransactions.value = it }
                    }
                    roomMessagesJob?.cancel()
                    roomMessagesJob = launch {
                        roomRepository.observeRoomMessages(roomId)
                            .catch { error ->
                                val root = unwrapFirestoreError(error)
                                Log.w(TAG, "Room chat stream failed", root)
                                _roomError.value = root.localizedMessage
                                    ?: "Room messages could not be loaded. Please check room access and Firestore index."
                                emit(emptyList())
                            }
                            .collect { _messages.value = it }
                    }
                    voiceEngine.connectToRoom(
                        roomId = loadedRoom.id,
                        userId = currentUser.userId,
                        username = currentUser.username,
                        canPublish = loadedRoom.isOwner(currentUser.userId)
                    )
                }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to join room" }
            roomJoinInProgress = false
        }
    }

    fun clearError() {
        _roomError.value = null
    }

    fun clearGiftBurst() {
        _giftBurst.value = null
    }

    fun clearVipEntrance() {
        _vipEntrance.value = null
    }

    fun joinSpeakerSeat(room: VoiceRoom, seatIndex: Int, user: UserProfile) {
        viewModelScope.launch {
            roomRepository.joinSpeakerSeat(room, seatIndex, user)
                .onSuccess { voiceEngine.setMicPublished(true) }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to take seat" }
        }
    }

    fun enableMicrophone() {
        viewModelScope.launch {
            voiceEngine.setMicPublished(true)
        }
    }

    fun vacateSpeakerSeat(seatIndex: Int, user: UserProfile) {
        viewModelScope.launch {
            roomRepository.vacateSpeakerSeat(roomId, seatIndex, user)
                .onSuccess { voiceEngine.setMicPublished(false) }
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun toggleSeatMute(seat: VoiceSeat) {
        viewModelScope.launch {
            val nextMuted = !seat.muted
            roomRepository.toggleSeatMute(roomId, seat, nextMuted)
                .onSuccess {
                    if (seat.occupantId == currentUserId) {
                        voiceEngine.setMicPublished(!nextMuted)
                    }
                }
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun toggleSeatLock(seat: VoiceSeat, locked: Boolean) {
        viewModelScope.launch {
            roomRepository.toggleSeatLock(roomId, seat.seatIndex.toInt(), locked)
                .onSuccess {
                    _roomError.value = if (locked) "Seat ${seat.seatIndex} locked" else "Seat ${seat.seatIndex} unlocked"
                }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to update seat lock" }
        }
    }

    fun muteAllSpeakers() {
        viewModelScope.launch {
            roomRepository.muteAllSpeakers(roomId)
                .onSuccess { _roomError.value = "All speaker mics muted" }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to mute all speakers" }
        }
    }

    fun hostRemoveSpeaker(seatIndex: Int) {
        viewModelScope.launch {
            roomRepository.hostRemoveSpeaker(roomId, seatIndex)
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun hostAssignUserToSeat(
        room: VoiceRoom,
        seatIndex: Int,
        userId: String,
        username: String,
        avatarUrl: String,
        vipLevel: Long,
        gender: String = "UNSPECIFIED"
    ) {
        viewModelScope.launch {
            roomRepository.sendSeatInvitation(room, seatIndex, userId, username, avatarUrl, vipLevel, gender)
                .onSuccess { _roomError.value = "Seat $seatIndex invitation sent to $username. They must accept it first." }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to send seat invitation" }
        }
    }

    fun respondToSeatInvitation(request: SpeakerRequest, accept: Boolean) {
        viewModelScope.launch {
            roomRepository.respondToSeatInvitation(roomId, request, accept)
                .onSuccess {
                    if (accept) voiceEngine.setMicPublished(true)
                    _roomError.value = if (accept) "Seat invitation accepted." else "Seat invitation declined."
                }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to respond to seat invitation" }
        }
    }

    fun promoteOrDemoteAdmin(targetUserId: String, targetUsername: String, promote: Boolean) {
        viewModelScope.launch {
            roomRepository.promoteOrDemoteRoomAdmin(roomId, targetUserId, targetUsername, promote)
                .onSuccess {
                    _roomError.value = if (promote) {
                        "$targetUsername promoted to Room Admin"
                    } else {
                        "Removed $targetUsername from Room Admins"
                    }
                }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to update Room Admin" }
        }
    }

    fun updateRoomSettings(
        title: String,
        announcement: String,
        category: String? = null,
        coverKey: String,
        keepCustomCover: Boolean,
        locked: Boolean,
        newPassword: String?
    ) {
        viewModelScope.launch {
            roomRepository.updateRoomSettings(
                roomId = roomId,
                title = title,
                announcement = announcement,
                category = category,
                coverKey = coverKey,
                keepCustomCover = keepCustomCover,
                locked = locked,
                newRawPassword = newPassword
            )
                .onSuccess { _roomError.value = "Room settings updated" }
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun uploadRoomCover(uri: Uri) {
        viewModelScope.launch {
            try {
                val storage = FirebaseStorage.getInstance("gs://vibeplay-anshu.firebasestorage.app")
                val coverRef = storage.reference
                    .child("room-covers/$roomId/cover_${System.currentTimeMillis()}.jpg")
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()
                coverRef.putFile(uri, metadata).await()
                val downloadUrl = coverRef.downloadUrl.await().toString()
                roomRepository.updateRoomCoverUrl(roomId, downloadUrl)
                    .onSuccess { _roomError.value = "Room cover uploaded successfully." }
                    .onFailure { _roomError.value = it.localizedMessage ?: "Could not save room cover." }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _roomError.value = error.localizedMessage ?: "Room cover upload failed."
            }
        }
    }

    fun transferOwnership(room: VoiceRoom, targetSeat: VoiceSeat) {
        viewModelScope.launch {
            roomRepository.transferOwnershipSafely(room, targetSeat)
                .onSuccess { _roomError.value = "Host ownership transferred to ${targetSeat.occupantName}" }
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun sendRoomMessage(sender: UserProfile, text: String) {
        viewModelScope.launch {
            roomRepository.sendRoomMessage(roomId, sender, text)
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun sendGift(
        sender: UserProfile,
        recipientId: String,
        recipientName: String,
        gift: GiftCatalogItem,
        quantity: Long = 1L
    ) {
        viewModelScope.launch {
            roomRepository.sendVirtualGift(roomId, sender, recipientId, recipientName, gift, quantity)
                .onFailure { _roomError.value = it.localizedMessage ?: "Gift send failed" }
        }
    }

    fun endRoom(onEnded: () -> Unit) {
        viewModelScope.launch {
            voiceEngine.disconnect()
            roomRepository.endRoom(roomId)
                .onSuccess { onEnded() }
                .onFailure { _roomError.value = it.localizedMessage }
        }
    }

    fun kickRoomMember(targetUserId: String) {
        viewModelScope.launch {
            roomRepository.kickRoomMember(roomId, targetUserId)
                .onFailure { _roomError.value = it.localizedMessage ?: "Could not remove this member." }
        }
    }

    fun unblockRoomMember(targetUserId: String) {
        viewModelScope.launch {
            roomRepository.unblockRoomMember(roomId, targetUserId)
                .onFailure { _roomError.value = it.localizedMessage ?: "Could not unblock this member." }
        }
    }

    fun leaveRoom(onLeft: () -> Unit) {
        val currentRoom = room.value
        val seats = rawSeats.value

        // Stop the previous visit immediately, even if the presence cleanup later fails.
        joinedListenerDone = false
        roomJoinInProgress = false
        roomJoinJob?.cancel()
        roomJoinJob = null
        roomSessionJob?.cancel()
        roomSessionJob = null
        roomHeartbeatJob?.cancel()
        roomHeartbeatJob = null
        voiceEngine.disconnect()
        _giftBurst.value = null
        roomMessagesJob?.cancel()
        roomMessagesJob = null
        roomGiftTransactionsJob?.cancel()
        roomGiftTransactionsJob = null
        _roomGiftTransactions.value = emptyList()
        _messages.value = emptyList()
        _giftBurst.value = null

        if (currentRoom == null) {
            onLeft()
            return
        }
        viewModelScope.launch(NonCancellable) {
            val result = roomRepository.leaveRoom(currentRoom, currentUserId, seats)
            if (result.isFailure) {
                _roomError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Could not update your room presence. Please try leaving again."
            }
            onLeft()
        }
    }

    override fun onCleared() {
        super.onCleared()
        roomHeartbeatJob?.cancel()
        voiceEngine.disconnect()
    }

    private companion object {
        const val TAG = "RoomVM"
    }
}
