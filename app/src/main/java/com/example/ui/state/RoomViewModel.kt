package com.example.ui.state

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.livekit.LiveKitVoiceEngine
import com.example.data.livekit.VoiceConnectionState
import com.example.data.model.GiftBurstEvent
import com.example.data.model.GiftCatalogItem
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomViewModel(
    private val roomId: String,
    private val currentUserId: String,
    private val roomRepository: RoomRepository,
    private val voiceEngine: LiveKitVoiceEngine = LiveKitVoiceEngine()
) : ViewModel() {

    private val _roomError = MutableStateFlow<String?>(null)
    val roomError: StateFlow<String?> = _roomError.asStateFlow()

    private val _giftBurst = MutableStateFlow<GiftBurstEvent?>(null)
    val giftBurst: StateFlow<GiftBurstEvent?> = _giftBurst.asStateFlow()

    private val _vipEntrance = MutableStateFlow<UserProfile?>(null)
    val vipEntrance: StateFlow<UserProfile?> = _vipEntrance.asStateFlow()

    private val _knownGifts = MutableStateFlow(VibeCatalogDefaults.defaultGifts)
    private val announcedVipIds = mutableSetOf<String>()

    val voiceConnectionState: StateFlow<VoiceConnectionState> = voiceEngine.connectionState

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

    val myPendingSpeakerRequest: StateFlow<SpeakerRequest?> =
        roomRepository.observeMySpeakerRequest(roomId, currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _speakerRequests = MutableStateFlow<List<SpeakerRequest>>(emptyList())
    val speakerRequests: StateFlow<List<SpeakerRequest>> = _speakerRequests.asStateFlow()

    private var speakerRequestsJob: Job? = null
    private var roomMessagesJob: Job? = null
    private var joinedListenerDone = false
    private var lastShownGiftMsgId: String? = null

    init {
        viewModelScope.launch {
            messages.collect { msgs ->
                val latestGift = msgs.lastOrNull { it.type == "GIFT" }
                if (latestGift != null && latestGift.id != lastShownGiftMsgId && latestGift.id.isNotBlank()) {
                    lastShownGiftMsgId = latestGift.id
                    val catalogMatch = _knownGifts.value.firstOrNull { it.id == latestGift.giftId }
                        ?: VibeCatalogDefaults.defaultGifts.firstOrNull { it.id == latestGift.giftId }
                    _giftBurst.value = GiftBurstEvent(
                        id = latestGift.id,
                        senderName = latestGift.senderName,
                        recipientName = latestGift.recipientName,
                        giftName = latestGift.giftName,
                        giftEmoji = catalogMatch?.emoji ?: "\uD83C\uDF81",
                        coinCost = latestGift.giftCoins
                    )
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
        if (!joinedListenerDone) {
            viewModelScope.launch {
                roomRepository.joinAsListener(loadedRoom, currentUser)
                    .onSuccess {
                        joinedListenerDone = true
                        _messages.value = emptyList()
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
            }
        }
        if (loadedRoom.isManager(currentUserId)) {
            if (speakerRequestsJob == null) {
                speakerRequestsJob = viewModelScope.launch {
                    roomRepository.observeSpeakerRequests(roomId)
                        .catch { error -> unwrapFirestoreError(error); emit(emptyList()) }
                        .collect { _speakerRequests.value = it }
                }
            }
        } else {
            speakerRequestsJob?.cancel()
            speakerRequestsJob = null
            if (_speakerRequests.value.isNotEmpty()) {
                _speakerRequests.value = emptyList()
            }
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

    fun requestToSpeak(room: VoiceRoom, user: UserProfile, preferredSeatIndex: Int = 0) {
        viewModelScope.launch {
            roomRepository.requestToSpeak(room, user, preferredSeatIndex)
                .onSuccess {
                    _roomError.value = if (preferredSeatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) {
                        "Request for Seat $preferredSeatIndex sent to Host!"
                    } else {
                        "Request to speak sent to Host!"
                    }
                }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to request seat" }
        }
    }

    fun cancelSpeakerRequest(userId: String) {
        viewModelScope.launch {
            roomRepository.cancelSpeakerRequest(roomId, userId)
                .onSuccess { _roomError.value = "Speaker request cancelled." }
                .onFailure { _roomError.value = it.localizedMessage ?: "Unable to cancel request" }
        }
    }

    fun resolveSpeakerRequest(room: VoiceRoom, request: SpeakerRequest, approve: Boolean) {
        viewModelScope.launch {
            roomRepository.resolveSpeakerRequest(room, request, approve)
                .onSuccess {
                    _roomError.value = if (approve) {
                        "Seat invitation sent to ${request.username}. They must accept it first."
                    } else {
                        "Declined ${request.username}'s request."
                    }
                }
                .onFailure { _roomError.value = it.localizedMessage ?: "No open speaker seats" }
        }
    }

    fun updateRoomSettings(
        title: String,
        announcement: String,
        category: String? = null,
        coverKey: String,
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
                val coverRef = FirebaseStorage.getInstance().reference
                    .child("room-covers/$roomId/cover.jpg")
                coverRef.putFile(uri).await()
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

    fun leaveRoom(onLeft: () -> Unit) {
        val currentRoom = room.value
        val seats = rawSeats.value
        voiceEngine.disconnect()
        roomMessagesJob?.cancel()
        roomMessagesJob = null
        _messages.value = emptyList()
        if (currentRoom == null) {
            onLeft()
            return
        }
        viewModelScope.launch(NonCancellable) {
            val result = roomRepository.leaveRoom(currentRoom, currentUserId, seats)
            if (result.isSuccess) {
                joinedListenerDone = false
            } else {
                _roomError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Could not update your room presence. Please try leaving again."
            }
            onLeft()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.disconnect()
    }

    private companion object {
        const val TAG = "RoomVM"
    }
}
