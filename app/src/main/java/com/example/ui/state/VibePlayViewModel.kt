package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.PreferencesRepository
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.FollowEdge
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftTransaction
import com.example.data.model.GlobalGiftAnnouncement
import com.example.data.model.ModerationReport
import com.example.data.model.PrivateAccount
import com.example.data.model.RoomCreationLock
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.data.repository.InboxRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoomRepository
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VibePlayViewModel(
    private val currentUserId: String,
    private val firebaseUser: FirebaseUser?,
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val inboxRepository: InboxRepository,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)

    private var onlineHeartbeatJob: Job? = null

    fun startOnlineHeartbeat() {
        if (onlineHeartbeatJob?.isActive == true) return
        onlineHeartbeatJob = viewModelScope.launch {
            while (true) {
                userRepository.setOnlineStatus(true)
                kotlinx.coroutines.delay(15_000L)
            }
        }
    }

    fun stopOnlineHeartbeat() {
        onlineHeartbeatJob?.cancel()
        onlineHeartbeatJob = null
        viewModelScope.launch { userRepository.setOnlineStatus(false) }
    }

    private var inspectedUserProfileJob: Job? = null
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val recentRoomIds: StateFlow<List<String>> = preferencesRepository.recentRoomIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val darkTheme: StateFlow<Boolean> = preferencesRepository.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val roomEffectsEnabled: StateFlow<Boolean> = preferencesRepository.roomEffectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private val _profileInitError = MutableStateFlow<String?>(null)
    private val _profileObserverRetry = MutableStateFlow(0)

    val currentUserProfile: StateFlow<UiState<UserProfile>> =
        combine(
            _profileObserverRetry.flatMapLatest {
                userRepository.observeUserProfile(currentUserId)
                    .map<UserProfile?, UiState<UserProfile>> { profile ->
                        if (profile != null) UiState.Success(profile) else UiState.Loading
                    }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        Log.w(TAG, "Error observing current profile", root)
                        emit(UiState.Error(root.message ?: "Unable to load profile"))
                    }
            },
            _profileInitError
        ) { observedState, initError ->
            when {
                observedState is UiState.Success -> observedState
                initError != null -> UiState.Error(initError)
                else -> observedState
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val liveRooms: StateFlow<UiState<List<VoiceRoom>>> =
        roomRepository.observeLiveRooms()
            .map<List<VoiceRoom>, UiState<List<VoiceRoom>>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing live rooms", root)
                emit(UiState.Error(root.message ?: "Unable to load live rooms"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeUsers: StateFlow<List<UserProfile>> =
        userRepository.observeActiveUsers()
            .catch { error ->
                val root = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing active users", root)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val privateAccount: StateFlow<PrivateAccount?> =
        userRepository.observePrivateAccount(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val roomCreationLock: StateFlow<RoomCreationLock?> =
        userRepository.observeRoomCreationLock(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val hasCreatedRoom: StateFlow<Boolean> =
        combine(currentUserProfile, roomCreationLock) { profileState, lock ->
            val profileCreated = (profileState as? UiState.Success)?.data?.hasCreatedRoom == true
            profileCreated || lock != null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _isCreatingRoom = MutableStateFlow(false)
    val isCreatingRoom: StateFlow<Boolean> = _isCreatingRoom.asStateFlow()

    val pinnedConversationIds: StateFlow<List<String>> =
        privateAccount
            .map { it?.pinnedConversationIds ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followingList: StateFlow<List<FollowEdge>> =
        userRepository.observeFollowing(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followersList: StateFlow<List<FollowEdge>> =
        userRepository.observeFollowers(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<Conversation>> =
        inboxRepository.observeConversations(currentUserId)
            .catch { error ->
                val root = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing conversations", root)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        notificationRepository.observeNotifications(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val giftHistory: StateFlow<List<GiftTransaction>> =
        roomRepository.observeGiftHistory(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val latestGlobalGiftAnnouncement: StateFlow<GlobalGiftAnnouncement?> =
        roomRepository.observeLatestGlobalGiftAnnouncement()
            .catch { error ->
                Log.w(TAG, "Unable to observe global gift announcements", unwrapFirestoreError(error))
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val giftRankings: StateFlow<List<UserProfile>> =
        userRepository.observeGiftRankings()
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeGiftsCatalog: StateFlow<List<GiftCatalogItem>> =
        roomRepository.observeActiveGifts()
            .map { remote ->
                val curatedById = remote
                    .filter { gift -> VibeCatalogDefaults.defaultGifts.any { it.id == gift.id } }
                    .associateBy { it.id }
                VibeCatalogDefaults.defaultGifts.map { defaultGift ->
                    val remoteGift = curatedById[defaultGift.id]
                    when (defaultGift.id) {
                        "gift_sunset_love" -> (remoteGift ?: defaultGift).copy(coinCost = 19999L)
                        "gift_royal_vows" -> (remoteGift ?: defaultGift).copy(coinCost = 99999L)
                        "gift_sunlit_dream" -> (remoteGift ?: defaultGift).copy(coinCost = 29999L)
                        "gift_prism_muse" -> (remoteGift ?: defaultGift).copy(coinCost = 49999L)
                        else -> remoteGift ?: defaultGift
                    }
                }
            }
            .catch { error ->
                unwrapFirestoreError(error)
                emit(VibeCatalogDefaults.defaultGifts)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), VibeCatalogDefaults.defaultGifts)

    init {
        retryProfileInit()
    }

    fun retryProfileInit() {
        _profileInitError.value = null
        _profileObserverRetry.value += 1
        viewModelScope.launch {
            firebaseUser?.let { user ->
                userRepository.ensureCurrentUserProfile(user)
                    .onSuccess { profile ->
                        // Existing accounts can remain marked offline after a previous sign-out.
                        // Restore presence after successful profile initialization on app launch.
                        if (profile.accountStatus == "ACTIVE") {
                            userRepository.setOnlineStatus(true)
                        }
                        roomRepository.syncExistingRoomCreationLock(user.uid)
                    }
                    .onFailure { error ->
                        val root = unwrapFirestoreError(error)
                        val msg = root.localizedMessage ?: "Unable to initialize VibePlay profile."
                        _profileInitError.value = msg
                        _statusMessage.value = msg
                    }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun recordRoomVisited(roomId: String) {
        viewModelScope.launch {
            preferencesRepository.recordRecentRoom(roomId)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkTheme(enabled) }
    }

    fun setRoomEffects(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRoomEffects(enabled) }
    }

    fun verifyRoomPasscode(room: VoiceRoom, enteredPin: String): Boolean {
        if (!room.hasPassword) return true
        return roomRepository.hashPasscode(enteredPin) == room.passcodeHash
    }

    fun createVoiceRoom(
        host: UserProfile,
        title: String,
        announcement: String,
        category: String,
        coverKey: String,
        locked: Boolean,
        rawPassword: String,
        onCreated: (String) -> Unit
    ) {
        if (_isCreatingRoom.value) return
        if (hasCreatedRoom.value || host.hasCreatedRoom || roomCreationLock.value != null) {
            _statusMessage.value = RoomRepository.ONE_ROOM_PER_USER_MESSAGE
            return
        }
        viewModelScope.launch {
            _isCreatingRoom.value = true
            try {
                roomRepository.createVoiceRoom(host, title, announcement, category, coverKey, locked, rawPassword)
                    .onSuccess { roomId ->
                        recordRoomVisited(roomId)
                        onCreated(roomId)
                    }
                    .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to create room" }
            } finally {
                _isCreatingRoom.value = false
            }
        }
    }

    fun updateProfile(
        username: String,
        bio: String,
        avatarUrl: String,
        coverUrl: String,
        gender: String,
        age: Long,
        showAge: Boolean,
        showGender: Boolean,
        showOnlineStatus: Boolean,
        profileTags: List<String>
    ) {
        viewModelScope.launch {
            userRepository.updateProfile(username, bio, avatarUrl, coverUrl, gender, age, showAge, showGender, showOnlineStatus, profileTags)
                .onSuccess { _statusMessage.value = "Profile updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Profile update failed" }
        }
    }

    fun inspectUser(
        userId: String,
        fallback: UserProfile? = null,
        onLoaded: (UserProfile) -> Unit
    ) {
        if (userId.isBlank()) return
        inspectedUserProfileJob?.cancel()

        // Render immediately from memory, then keep the opened profile synchronized
        // with Firestore so title, official status, frame and gift data do not stay stale.
        val cached = activeUsers.value.firstOrNull { it.userId == userId }
        (cached ?: fallback)?.let(onLoaded)

        inspectedUserProfileJob = viewModelScope.launch {
            userRepository.observeUserProfile(userId)
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    Log.w(TAG, "Error observing inspected user profile", root)
                    if (cached == null && fallback == null) {
                        _statusMessage.value = root.message ?: "Unable to load profile"
                    }
                }
                .collect { latest ->
                    if (latest != null) {
                        onLoaded(latest)
                    } else if (cached == null && fallback == null) {
                        _statusMessage.value = "User profile not found"
                    }
                }
        }
    }

    fun clearInspectedUser() {
        inspectedUserProfileJob?.cancel()
        inspectedUserProfileJob = null
    }

    fun observeFollowingFor(userId: String): Flow<List<FollowEdge>> =
        userRepository.observeFollowing(userId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }

    fun observeFollowersFor(userId: String): Flow<List<FollowEdge>> =
        userRepository.observeFollowers(userId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }

    fun claimDailyReward(profile: UserProfile) {
        viewModelScope.launch {
            userRepository.claimDailyLoginReward(profile)
                .onSuccess { coins -> _statusMessage.value = "+$coins daily login coins claimed!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Already claimed today" }
        }
    }


    fun purchaseShopCosmetic(category: String, itemId: String) {
        viewModelScope.launch {
            userRepository.purchaseShopCosmetic(category, itemId)
                .onSuccess { _statusMessage.value = "Purchase successful!" }
                .onFailure { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    val code = firestoreError?.code?.name ?: "NOT_FIRESTORE"
                    val detail = firestoreError?.localizedMessage
                        ?: error.localizedMessage
                        ?: "No error details returned"
                    _statusMessage.value = "Frame purchase failed [$code] · $itemId: $detail"
                    Log.e(TAG, "Frame purchase failed: category=$category itemId=$itemId code=$code", error)
                }
        }
    }

    fun equipShopCosmetic(category: String, itemId: String?) {
        viewModelScope.launch {
            userRepository.equipShopCosmetic(category, itemId)
                .onSuccess { _statusMessage.value = if (itemId.isNullOrBlank()) "Item removed from profile." else "Item equipped on profile!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to equip item." }
        }
    }

    fun upgradeVip(profile: UserProfile, targetTier: Int) {
        viewModelScope.launch {
            userRepository.upgradeVipTier(profile, targetTier)
                .onSuccess { _statusMessage.value = "Upgraded to VIP $targetTier!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Upgrade failed" }
        }
    }

    fun toggleFollow(currentUser: UserProfile, targetUser: UserProfile, currentlyFollowing: Boolean) {
        viewModelScope.launch {
            userRepository.toggleFollow(currentUser, targetUser, currentlyFollowing)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun toggleBlockUser(targetUserId: String, block: Boolean) {
        val blocked = privateAccount.value?.blockedUserIds ?: emptyList()
        viewModelScope.launch {
            userRepository.blockOrUnblockUser(targetUserId, block, blocked)
                .onSuccess {
                    _statusMessage.value = if (block) "User blocked" else "User unblocked"
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun openConversation(currentUser: UserProfile, peer: UserProfile, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            inboxRepository.getOrCreateConversation(currentUser, peer)
                .onSuccess(onOpened)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to open chat" }
        }
    }

    fun createGroupChat(
        creator: UserProfile,
        groupName: String,
        groupAvatarUrl: String,
        members: List<UserProfile>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            inboxRepository.createGroupConversation(creator, groupName, groupAvatarUrl, members)
                .onSuccess { convId ->
                    _statusMessage.value = "Group \"$groupName\" created!"
                    onCreated(convId)
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to create group" }
        }
    }

    fun observeDirectMessages(conversationId: String) =
        inboxRepository.observeMessages(conversationId)
            .catch { error ->
                unwrapFirestoreError(error)
                emit(emptyList())
            }

    fun sendDirectMessage(
        conversation: Conversation,
        sender: UserProfile,
        text: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            inboxRepository.sendMessage(
                conversation = conversation,
                sender = sender,
                text = text,
                imageUri = imageUri
            ).onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to send message" }
        }
    }

    fun sendInboxGift(conversation: Conversation, sender: UserProfile, gift: GiftCatalogItem, quantity: Long = 1L) {
        viewModelScope.launch {
            inboxRepository.sendInboxGift(conversation, sender, gift, quantity)
                .onSuccess { _statusMessage.value = "Gift sent successfully" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to send gift" }
        }
    }

    fun addGroupMembers(conversation: Conversation, actor: UserProfile, newMembers: List<UserProfile>) {
        viewModelScope.launch {
            inboxRepository.addGroupMembers(conversation, actor, newMembers)
                .onSuccess { _statusMessage.value = "Members added to group" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to add members" }
        }
    }

    fun removeGroupMember(conversation: Conversation, actor: UserProfile, targetUserId: String) {
        viewModelScope.launch {
            inboxRepository.removeGroupMember(conversation, actor, targetUserId)
                .onSuccess { _statusMessage.value = "Member removed from group" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to remove member" }
        }
    }

    fun updateGroupDetails(conversation: Conversation, groupName: String, groupAvatarUrl: String) {
        viewModelScope.launch {
            inboxRepository.updateGroupDetails(conversation, groupName, groupAvatarUrl)
                .onSuccess { _statusMessage.value = "Group updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to update group" }
        }
    }

    fun leaveGroup(conversation: Conversation, user: UserProfile, onLeft: () -> Unit) {
        viewModelScope.launch {
            inboxRepository.leaveGroup(conversation, user)
                .onSuccess {
                    _statusMessage.value = "Left group ${conversation.groupName}"
                    onLeft()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to leave group" }
        }
    }

    fun togglePinConversation(conversationId: String, pin: Boolean) {
        viewModelScope.launch {
            inboxRepository.togglePinConversation(currentUserId, conversationId, pin)
                .onSuccess {
                    _statusMessage.value = if (pin) "Chat pinned" else "Chat unpinned"
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to update pin" }
        }
    }

    fun markConversationRead(conversation: Conversation) {
        viewModelScope.launch {
            inboxRepository.markConversationRead(conversation, currentUserId)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { notificationRepository.markAsRead(notificationId) }
    }

    fun markAllNotificationsRead(notifications: List<AppNotification>) {
        val unreadIds = notifications.filter { !it.read }.map { it.id }.filter { it.isNotBlank() }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(unreadIds)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun inviteFriendToRoom(sender: UserProfile, friendId: String, room: VoiceRoom) {
        viewModelScope.launch {
            notificationRepository.inviteFriendToRoom(sender, friendId, room)
                .onSuccess { _statusMessage.value = "Room invite sent!" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun submitReport(
        reporter: UserProfile,
        targetType: String,
        targetId: String,
        targetName: String,
        reason: String,
        details: String
    ) {
        viewModelScope.launch {
            notificationRepository.submitReport(reporter, targetType, targetId, targetName, reason, details)
                .onSuccess { _statusMessage.value = "Report submitted for moderation review." }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    suspend fun setOfflineBeforeSignOut() {
        roomRepository.cleanupCurrentUserPresenceInLiveRooms()
            .onFailure { Log.w(TAG, "Unable to clean room presence before sign out", it) }
        userRepository.setOnlineStatus(false)
    }

    private companion object {
        const val TAG = "VibePlayVM"
    }
}
