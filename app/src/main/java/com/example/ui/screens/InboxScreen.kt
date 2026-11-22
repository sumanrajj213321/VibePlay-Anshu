package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.FollowEdge
import com.example.data.model.GiftCatalogItem
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.ui.components.ReportUserOrRoomDialog
import com.example.ui.components.VibeAvatar
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

private const val OFFICIAL_BOT_UID = "VIBEPLAY_OFFICIAL_BOT"

@Composable
fun InboxScreen(
    currentUser: UserProfile,
    conversations: List<Conversation>,
    activeUsers: List<UserProfile>,
    followingList: List<FollowEdge> = emptyList(),
    notifications: List<AppNotification>,
    blockedUserIds: List<String>,
    pinnedConversationIds: List<String> = emptyList(),
    selectedConversationId: String?,
    onSelectConversation: (String?) -> Unit,
    onOpenConversationWithUser: (UserProfile) -> Unit,
    onCreateGroupChat: (groupName: String, groupAvatarUrl: String, members: List<UserProfile>) -> Unit = { _, _, _ -> },
    onAddGroupMembers: (Conversation, List<UserProfile>) -> Unit = { _, _ -> },
    onRemoveGroupMember: (Conversation, String) -> Unit = { _, _ -> },
    onUpdateGroupDetails: (Conversation, String, String) -> Unit = { _, _, _ -> },
    onLeaveGroup: (Conversation) -> Unit = {},
    onTogglePinConversation: (conversationId: String, pin: Boolean) -> Unit = { _, _ -> },
    activeGifts: List<GiftCatalogItem> = VibeCatalogDefaults.defaultGifts,
    onSendGift: (Conversation, GiftCatalogItem, Long) -> Unit = { _, _, _ -> },
    observeMessages: (String) -> Flow<List<DirectMessage>>,
    onSendMessage: (Conversation, String, String?) -> Unit,
    onMarkConversationRead: (Conversation) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit = {},
    onOpenRoomById: (String) -> Unit,
    onOpenUserById: (String) -> Unit = {},
    onBlockUser: (String) -> Unit,
    onSubmitReport: (targetType: String, targetId: String, targetName: String, reason: String, details: String) -> Unit
) {
    var showNewChatSheet by remember { mutableStateOf(false) }
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val selectedConversation = conversations.firstOrNull { it.id == selectedConversationId }
    if (selectedConversation != null) {
        ChatDetailScreen(
            currentUser = currentUser,
            conversation = selectedConversation,
            activeUsers = activeUsers,
            followingList = followingList,
            blockedUserIds = blockedUserIds,
            isPinned = selectedConversation.id in pinnedConversationIds,
            observeMessages = observeMessages,
            onBack = { onSelectConversation(null) },
            onSendMessage = { text, imageUri -> onSendMessage(selectedConversation, text, imageUri) },
            onMarkRead = { onMarkConversationRead(selectedConversation) },
            onTogglePin = { pin -> onTogglePinConversation(selectedConversation.id, pin) },
            activeGifts = activeGifts,
            onSendGift = { gift, quantity -> onSendGift(selectedConversation, gift, quantity) },
            onInspectPeer = { peerId -> onOpenUserById(peerId) },
            onAddGroupMembers = { newMembers -> onAddGroupMembers(selectedConversation, newMembers) },
            onRemoveGroupMember = { targetUid -> onRemoveGroupMember(selectedConversation, targetUid) },
            onUpdateGroupDetails = { name, avatar -> onUpdateGroupDetails(selectedConversation, name, avatar) },
            onLeaveGroup = { onLeaveGroup(selectedConversation) },
            onBlockUser = onBlockUser,
            onSubmitReport = onSubmitReport
        )
        return
    }

    val unreadNotifications = notifications.count { !it.read }
    val pinnedSet = remember(pinnedConversationIds) { pinnedConversationIds.toSet() }
    val visibleConversations = remember(
        conversations,
        currentUser.userId,
        blockedUserIds,
        searchQuery,
        activeUsers,
        pinnedSet
    ) {
        conversations
            .filter { conv ->
                if (conv.isGroupChat) {
                    searchQuery.isBlank() ||
                        conv.groupName.contains(searchQuery, ignoreCase = true) ||
                        conv.lastMessage.contains(searchQuery, ignoreCase = true)
                } else {
                    val peerId = conv.participantIds.firstOrNull { it != currentUser.userId } ?: ""
                    val peerName = activeUsers.firstOrNull { it.userId == peerId }?.username
                        ?: conv.participantNames[peerId].orEmpty()
                    peerId !in blockedUserIds &&
                        (searchQuery.isBlank() ||
                            peerName.contains(searchQuery, ignoreCase = true) ||
                            conv.lastMessage.contains(searchQuery, ignoreCase = true))
                }
            }
            .sortedWith(
                compareByDescending<Conversation> { it.id in pinnedSet }
                    .thenByDescending { it.updatedAt?.seconds ?: 0L }
            )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inbox",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Direct chats, group chats & alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { showNotificationsSheet = true },
                    modifier = Modifier.testTag("inbox_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge { Text("$unreadNotifications") }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
                IconButton(
                    onClick = { showCreateGroupSheet = true },
                    modifier = Modifier.testTag("new_group_chat_button")
                ) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = "Create group chat", tint = VibeViolet)
                }
                IconButton(
                    onClick = { showNewChatSheet = true },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(Icons.Filled.AddComment, contentDescription = "Start new chat", tint = VibeCyan)
                }
            }
        }

        if (conversations.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search direct or group chats...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("inbox_search_input")
            )
        }

        if (visibleConversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkChatUnread,
                        contentDescription = null,
                        tint = VibeCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("No Messages Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Start a private 1-to-1 chat or create a group with people you follow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleConversations, key = { it.id }) { conv ->
                    val isPinned = conv.id in pinnedSet
                    val isUnread = conv.unreadBy.contains(currentUser.userId)
                    val peerId = conv.participantIds.firstOrNull { it != currentUser.userId } ?: ""
                    val isOfficialBot = !conv.isGroupChat && peerId == OFFICIAL_BOT_UID
                    val peerProfile = activeUsers.firstOrNull { it.userId == peerId }
                    val displayName = if (conv.isGroupChat) {
                        conv.groupName.ifBlank { "Group Chat" }
                    } else {
                        peerProfile?.username ?: conv.participantNames[peerId] ?: "VibeUser"
                    }
                    val displayAvatar = if (conv.isGroupChat) {
                        conv.groupAvatarUrl
                    } else {
                        peerProfile?.avatarUrl ?: conv.participantAvatars[peerId] ?: ""
                    }
                    val isOnline = if (conv.isGroupChat || isOfficialBot) false else (peerProfile?.let { it.online && it.showOnlineStatus } ?: false)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isUnread -> VibeViolet.copy(alpha = 0.16f)
                            isPinned -> VibeGold.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isUnread -> VibeCyan.copy(alpha = 0.55f)
                                isPinned -> VibeGold.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectConversation(conv.id) }
                            .testTag("conversation_${conv.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box {
                                VibeAvatar(
                                    avatarUrl = displayAvatar,
                                    username = displayName,
                                    size = 50.dp,
                                    vipLevel = if (conv.isGroupChat) 0L else (peerProfile?.let { if (it.isVipActive) it.vipLevel else 0L } ?: 0L),
                                    online = isOnline,
                                    frameColorArgb = if (conv.isGroupChat) null else peerProfile?.let { profile -> VibeShopCatalog.frames.firstOrNull { it.id == profile.activeFrameId && profile.isFrameActive(it.id) }?.colorArgb }
                                )
                                if (conv.isGroupChat) {
                                    Surface(
                                        shape = CircleShape,
                                        color = VibeViolet,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Groups,
                                                contentDescription = "Group chat",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isOfficialBot) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = VibeGold.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "BOT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VibeGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                                    .testTag("official_bot_badge_${conv.id}")
                                            )
                                        }
                                    }
                                    if (conv.isGroupChat) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = VibeCyan.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = "${conv.participantIds.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VibeCyan,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (isPinned) {
                                        Icon(
                                            imageVector = Icons.Filled.PushPin,
                                            contentDescription = "Pinned",
                                            tint = VibeGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = conv.lastMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isUnread) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(VibePink)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = { onTogglePinConversation(conv.id, !isPinned) },
                                    modifier = Modifier.testTag("pin_conversation_${conv.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = if (isPinned) "Unpin chat" else "Pin chat",
                                        tint = if (isPinned) VibeGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        val candidates = activeUsers.filter { it.userId != currentUser.userId && it.userId !in blockedUserIds }
        NewChatPickerSheet(
            users = candidates,
            onPickUser = { peer ->
                showNewChatSheet = false
                onOpenConversationWithUser(peer)
            },
            onDismiss = { showNewChatSheet = false }
        )
    }

    if (showCreateGroupSheet) {
        val followingCandidates = remember(followingList, activeUsers, blockedUserIds, currentUser.userId) {
            followingList
                .filter { it.peerId.isNotBlank() && it.peerId != currentUser.userId && it.peerId !in blockedUserIds }
                .map { edge ->
                    activeUsers.firstOrNull { it.userId == edge.peerId }
                        ?: UserProfile(
                            userId = edge.peerId,
                            username = edge.peerName.ifBlank { "VibeUser" },
                            avatarUrl = edge.peerAvatarUrl,
                            gender = edge.peerGender
                        )
                }
        }
        CreateGroupSheet(
            followingCandidates = followingCandidates,
            onCreateGroup = { groupName, groupAvatarUrl, members ->
                showCreateGroupSheet = false
                onCreateGroupChat(groupName, groupAvatarUrl, members)
            },
            onDismiss = { showCreateGroupSheet = false }
        )
    }

    if (showNotificationsSheet) {
        NotificationsCenterSheet(
            notifications = notifications,
            onMarkRead = onMarkNotificationRead,
            onMarkAllRead = onMarkAllNotificationsRead,
            onOpenRoomById = { roomId ->
                showNotificationsSheet = false
                onOpenRoomById(roomId)
            },
            onOpenConversationById = { convId ->
                showNotificationsSheet = false
                onSelectConversation(convId)
            },
            onOpenUserById = { uid ->
                showNotificationsSheet = false
                onOpenUserById(uid)
            },
            onDismiss = { showNotificationsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailScreen(
    currentUser: UserProfile,
    conversation: Conversation,
    activeUsers: List<UserProfile>,
    followingList: List<FollowEdge>,
    blockedUserIds: List<String>,
    isPinned: Boolean,
    observeMessages: (String) -> Flow<List<DirectMessage>>,
    onBack: () -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onMarkRead: () -> Unit,
    onTogglePin: (Boolean) -> Unit,
    activeGifts: List<GiftCatalogItem>,
    onSendGift: (GiftCatalogItem, Long) -> Unit,
    onInspectPeer: (String) -> Unit,
    onAddGroupMembers: (List<UserProfile>) -> Unit,
    onRemoveGroupMember: (String) -> Unit,
    onUpdateGroupDetails: (String, String) -> Unit,
    onLeaveGroup: () -> Unit,
    onBlockUser: (String) -> Unit,
    onSubmitReport: (targetType: String, targetId: String, targetName: String, reason: String, details: String) -> Unit
) {
    BackHandler(onBack = onBack)

    val isGroup = conversation.isGroupChat
    val peerId = conversation.participantIds.firstOrNull { it != currentUser.userId } ?: ""
    val isOfficialBot = !isGroup && peerId == OFFICIAL_BOT_UID
    val peerProfile = activeUsers.firstOrNull { it.userId == peerId }
    val headerTitle = if (isGroup) {
        conversation.groupName.ifBlank { "Group Chat" }
    } else {
        peerProfile?.username ?: conversation.participantNames[peerId] ?: "VibeUser"
    }
    val headerAvatar = if (isGroup) {
        conversation.groupAvatarUrl
    } else {
        peerProfile?.avatarUrl ?: conversation.participantAvatars[peerId] ?: ""
    }
    val relationshipPairId = remember(currentUser.userId, peerId) {
        if (isGroup || peerId.isBlank() || isOfficialBot) "" else {
            val raw = listOf(currentUser.userId, peerId).sorted().joinToString(":")
            MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
    var serverPairFamiliarity by remember(relationshipPairId) { mutableStateOf(0L) }
    var giftHistoryFamiliarity by remember(relationshipPairId) { mutableStateOf(0L) }
    val pairFamiliarity = maxOf(serverPairFamiliarity, giftHistoryFamiliarity)
    var pairTagStatus by remember(relationshipPairId) { mutableStateOf("NONE") }
    var pairTagType by remember(relationshipPairId) { mutableStateOf("") }
    DisposableEffect(relationshipPairId) {
        if (relationshipPairId.isBlank()) {
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection("relationship_pairs").document(relationshipPairId)
                .addSnapshotListener { snapshot, _ ->
                    serverPairFamiliarity = (snapshot?.getLong("familiarityPoints") ?: 0L).coerceAtLeast(0L)
                    pairTagStatus = snapshot?.getString("tagStatus") ?: "NONE"
                    pairTagType = snapshot?.getString("tagType")
                        ?: snapshot?.getString("acceptedTagType").orEmpty()
                }
            val giftsRegistration = FirebaseFirestore.getInstance()
                .collection("gift_transactions")
                .whereArrayContains("participantIds", currentUser.userId)
                .addSnapshotListener { snapshot, _ ->
                    val coinsBetweenPair = snapshot?.documents
                        ?.asSequence()
                        ?.filter { gift ->
                            val senderId = gift.getString("senderId")
                            val recipientId = gift.getString("recipientId")
                            (senderId == currentUser.userId && recipientId == peerId) ||
                                (senderId == peerId && recipientId == currentUser.userId)
                        }
                        ?.sumOf { gift -> (gift.getLong("coinCost") ?: 0L).coerceAtLeast(0L) }
                        ?: 0L
                    giftHistoryFamiliarity = coinsBetweenPair / 5L
                }
            onDispose {
                registration.remove()
                giftsRegistration.remove()
            }
        }
    }

    val messages by observeMessages(conversation.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var pendingImageUri by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showGiftPicker by remember { mutableStateOf(false) }
    var selectedGiftQuantity by remember { mutableStateOf(1L) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { pendingImageUri = it.toString() }
    }

    LaunchedEffect(conversation.id, conversation.unreadBy) {
        onMarkRead()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isGroup) {
                                showGroupInfoSheet = true
                            } else if (!isOfficialBot && peerId.isNotBlank()) {
                                onInspectPeer(peerId)
                            }
                        }
                        .testTag(if (isGroup) "chat_header_group_info" else "chat_header_profile")
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Inbox")
                    }
                    VibeAvatar(
                        avatarUrl = headerAvatar,
                        username = headerTitle,
                        size = 40.dp,
                        vipLevel = if (isGroup) 0L else (peerProfile?.let { if (it.isVipActive) it.vipLevel else 0L } ?: 0L),
                        online = if (isGroup) false else (peerProfile?.let { it.online && it.showOnlineStatus } ?: false)
                    )
                    Column {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isGroup -> "${conversation.participantIds.size} members · Tap for info"
                                isOfficialBot -> "BOT"
                                peerProfile?.online == true && peerProfile.showOnlineStatus -> "Online"
                                else -> "Offline"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isGroup -> VibeCyan
                                isOfficialBot -> VibeGold
                                peerProfile?.online == true && peerProfile.showOnlineStatus -> VibeEmerald
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = { onTogglePin(!isPinned) },
                        modifier = Modifier.testTag("chat_detail_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = if (isPinned) "Unpin chat" else "Pin chat",
                            tint = if (isPinned) VibeGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isGroup) {
                        IconButton(
                            onClick = { showGroupInfoSheet = true },
                            modifier = Modifier.testTag("chat_group_info_button")
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = "Group settings", tint = VibeCyan)
                        }
                    } else {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report user")
                        }
                        IconButton(onClick = { onBlockUser(peerId); onBack() }) {
                            Icon(Icons.Filled.Block, contentDescription = "Block user", tint = VibeCoral)
                        }
                    }
                }
            }
        }

        if (!isGroup && !isOfficialBot) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                shape = RoundedCornerShape(14.dp),
                color = VibeCyan.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, VibeCyan.copy(alpha = 0.28f))
            ) {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Familiarity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("%,d".format(Locale.US, pairFamiliarity), color = VibeCyan, fontWeight = FontWeight.ExtraBold)
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (pairFamiliarity.coerceAtMost(10000L) / 10000f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                        color = VibeCyan,
                        trackColor = VibeCyan.copy(alpha = 0.16f)
                    )
                    when {
                        pairTagStatus == "ACTIVE" -> Text(
                            "Relationship connected · ${pairTagType.lowercase().replaceFirstChar { it.uppercase() }}",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                        pairTagStatus == "WAITING_FAMILIARITY" -> Text(
                            "Invitation accepted. Reach 10,000 familiarity through gifts to connect your tag.",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        pairFamiliarity < 10000L -> Text(
                            "Familiarity is not enough. Send gifts to each other and reach 10,000 familiarity to unlock your Relationship Tag.",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> Text(
                            "10,000 familiarity reached. Accept the relationship invitation to connect your tag.",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeEmerald
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                messages,
                key = { index, msg -> msg.id.ifBlank { "dm_${index}_${msg.senderId}_${msg.text.hashCode()}" } }
            ) { _, msg ->
                val isOfficialBotMessage = msg.senderId == OFFICIAL_BOT_UID
                if (msg.type == "SYSTEM" && !isOfficialBotMessage) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = VibeCyan.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.labelSmall,
                                color = VibeCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    return@itemsIndexed
                }

                val isMine = msg.senderId == currentUser.userId
                val senderProfile = activeUsers.firstOrNull { it.userId == msg.senderId }
                val senderName = msg.senderName.ifBlank {
                    senderProfile?.username
                        ?: conversation.participantNames[msg.senderId]
                        ?: if (isMine) currentUser.username else "VibeUser"
                }
                val senderAvatar = msg.senderAvatarUrl.ifBlank {
                    senderProfile?.avatarUrl
                        ?: conversation.participantAvatars[msg.senderId]
                        ?: if (isMine) currentUser.avatarUrl else ""
                }
                val senderGender = if (msg.senderGender != "UNSPECIFIED" && msg.senderGender.isNotBlank()) {
                    msg.senderGender
                } else {
                    senderProfile?.gender ?: if (isMine) currentUser.gender else "UNSPECIFIED"
                }

                val timeText = remember(msg.createdAt) {
                    msg.createdAt?.toDate()?.let {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                    } ?: ""
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!isMine && msg.senderId.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .then(if (isOfficialBotMessage) Modifier else Modifier.clickable { onInspectPeer(msg.senderId) })
                                .testTag("dm_sender_avatar_${msg.id}")
                        ) {
                            VibeAvatar(
                                avatarUrl = senderAvatar,
                                username = senderName,
                                size = 30.dp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isMine -> VibeViolet
                            isOfficialBotMessage -> VibeGold.copy(alpha = 0.10f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (msg.senderId.isNotBlank() && (isGroup || !isMine)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .then(if (isOfficialBotMessage) Modifier else Modifier.clickable { onInspectPeer(msg.senderId) })
                                        .testTag("dm_sender_name_${msg.id}")
                                ) {
                                    Text(
                                        text = senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isMine -> Color.White.copy(alpha = 0.9f)
                                            isOfficialBotMessage -> VibeGold
                                            else -> VibeCyan
                                        }
                                    )
                                }
                            }
                            if (msg.type == "IMAGE" && msg.imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = msg.imageUri,
                                    contentDescription = "Shared photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (msg.text.isNotBlank()) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (timeText.isNotEmpty()) {
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        pendingImageUri?.let { uriStr ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = uriStr,
                            contentDescription = "Selected photo attachment",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text("Photo attached", style = MaterialTheme.typography.labelMedium, color = VibeCyan)
                    }
                    IconButton(onClick = { pendingImageUri = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove attachment")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("chat_pick_image_button")
            ) {
                Icon(Icons.Filled.Image, contentDescription = "Share image", tint = VibeCyan)
            }

            if (!isGroup) {
                IconButton(
                    onClick = { showGiftPicker = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(VibeGold.copy(alpha = 0.16f))
                        .testTag("inbox_send_gift_button")
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = "Send gift", tint = VibeGold)
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it.take(1000) },
                placeholder = { Text("Message $headerTitle...") },
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || pendingImageUri != null) {
                        onSendMessage(inputText, pendingImageUri)
                        inputText = ""
                        pendingImageUri = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VibeViolet)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = Color.White)
            }
        }
    }

    if (showGiftPicker && !isGroup) {
        ModalBottomSheet(onDismissRequest = { showGiftPicker = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Send a gift to $headerTitle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your current balance: ${currentUser.coinBalance} coins", style = MaterialTheme.typography.bodySmall, color = VibeGold)
                Text("Combo quantity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1L, 5L, 10L, 20L).forEach { quantity ->
                        OutlinedButton(
                            onClick = { selectedGiftQuantity = quantity },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedGiftQuantity == quantity) VibeCyan else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedGiftQuantity == quantity) VibeCyan else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text("${quantity}x", fontWeight = if (selectedGiftQuantity == quantity) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Text("Selected combo is sent as one gift transaction.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeGifts, key = { it.id }) { gift ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(gift.emoji, style = MaterialTheme.typography.headlineMedium)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(gift.name, fontWeight = FontWeight.SemiBold)
                                    val comboCost = gift.coinCost.toLong() * selectedGiftQuantity
                                    Text(
                                        "${gift.coinCost} coins each · ${comboCost} coins total",
                                        color = VibeGold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                val comboCost = gift.coinCost.toLong() * selectedGiftQuantity
                                Button(
                                    onClick = {
                                        onSendGift(gift, selectedGiftQuantity)
                                        showGiftPicker = false
                                        selectedGiftQuantity = 1L
                                    },
                                    enabled = comboCost <= currentUser.coinBalance,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(if (comboCost <= currentUser.coinBalance) "Send ${selectedGiftQuantity}x" else "Not enough")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportUserOrRoomDialog(
            targetName = headerTitle,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                onSubmitReport("USER", peerId, headerTitle, reason, details)
                showReportDialog = false
            }
        )
    }

    if (showGroupInfoSheet && isGroup) {
        GroupInfoSheet(
            currentUser = currentUser,
            conversation = conversation,
            activeUsers = activeUsers,
            followingList = followingList,
            blockedUserIds = blockedUserIds,
            onInspectMember = { memberId ->
                showGroupInfoSheet = false
                onInspectPeer(memberId)
            },
            onUpdateDetails = { newName, newAvatar ->
                onUpdateGroupDetails(newName, newAvatar)
            },
            onAddMembers = { newMembers ->
                onAddGroupMembers(newMembers)
            },
            onRemoveMember = { memberId ->
                onRemoveGroupMember(memberId)
            },
            onLeaveGroup = {
                showGroupInfoSheet = false
                onLeaveGroup()
            },
            onDismiss = { showGroupInfoSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatPickerSheet(
    users: List<UserProfile>,
    onPickUser: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Start Private Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (users.isEmpty()) {
                item {
                    Text("No other users available yet.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(users, key = { it.userId }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickUser(user) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VibeAvatar(user.avatarUrl, user.username, size = 44.dp, vipLevel = if (user.isVipActive) user.vipLevel else 0L, frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == user.activeFrameId && user.isFrameActive(it.id) }?.colorArgb)
                        Column {
                            Text(user.username, fontWeight = FontWeight.SemiBold)
                            Text(user.vibeId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupSheet(
    followingCandidates: List<UserProfile>,
    onCreateGroup: (groupName: String, groupAvatarUrl: String, members: List<UserProfile>) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupAvatarUrl by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { groupAvatarUrl = it.toString() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("create_group_sheet"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = VibeViolet)
                    Text("Create Group Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("create_group_pick_photo_button")
                    ) {
                        VibeAvatar(
                            avatarUrl = groupAvatarUrl,
                            username = groupName.ifBlank { "Group" },
                            size = 58.dp
                        )
                    }
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it.take(60) },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_group_name_input")
                    )
                }
            }
            item {
                Text(
                    text = "Select Following Members (${selectedIds.size} selected)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VibeCyan
                )
            }
            if (followingCandidates.isEmpty()) {
                item {
                    Text(
                        text = "Follow users in Explore or Rooms first to add them to a group chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(followingCandidates, key = { "grp_cand_${it.userId}" }) { member ->
                    val checked = member.userId in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (checked) selectedIds - member.userId else selectedIds + member.userId
                            }
                            .padding(vertical = 4.dp)
                            .testTag("create_group_member_${member.userId}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            VibeAvatar(member.avatarUrl, member.username, size = 40.dp, vipLevel = if (member.isVipActive) member.vipLevel else 0L, frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == member.activeFrameId && member.isFrameActive(it.id) }?.colorArgb)
                            Column {
                                Text(member.username, fontWeight = FontWeight.SemiBold)
                                if (member.vibeId.isNotBlank()) {
                                    Text(member.vibeId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectedIds = if (isChecked) selectedIds + member.userId else selectedIds - member.userId
                            }
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val chosen = followingCandidates.filter { it.userId in selectedIds }
                        onCreateGroup(groupName.trim(), groupAvatarUrl.trim(), chosen)
                    },
                    enabled = groupName.isNotBlank() && selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_create_group_button")
                ) {
                    Text("Create Group (${selectedIds.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupInfoSheet(
    currentUser: UserProfile,
    conversation: Conversation,
    activeUsers: List<UserProfile>,
    followingList: List<FollowEdge>,
    blockedUserIds: List<String>,
    onInspectMember: (String) -> Unit,
    onUpdateDetails: (String, String) -> Unit,
    onAddMembers: (List<UserProfile>) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val isGroupManager = currentUser.userId == conversation.ownerId || currentUser.userId in conversation.adminIds
    var editedName by remember(conversation.groupName) { mutableStateOf(conversation.groupName) }
    var editedAvatar by remember(conversation.groupAvatarUrl) { mutableStateOf(conversation.groupAvatarUrl) }
    var showAddPicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { editedAvatar = it.toString() }
    }

    val addableFollowing = remember(followingList, activeUsers, conversation.participantIds, blockedUserIds) {
        followingList
            .filter {
                it.peerId.isNotBlank() &&
                    it.peerId !in conversation.participantIds &&
                    it.peerId !in blockedUserIds
            }
            .map { edge ->
                activeUsers.firstOrNull { it.userId == edge.peerId }
                    ?: UserProfile(
                        userId = edge.peerId,
                        username = edge.peerName.ifBlank { "VibeUser" },
                        avatarUrl = edge.peerAvatarUrl,
                        gender = edge.peerGender
                    )
            }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("group_info_sheet"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = VibeCyan)
                    Text("Group Chat Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (isGroupManager) {
                                    Modifier.clickable {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("group_edit_photo_button")
                    ) {
                        VibeAvatar(
                            avatarUrl = editedAvatar,
                            username = editedName.ifBlank { "Group" },
                            size = 58.dp
                        )
                    }
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { if (isGroupManager) editedName = it.take(60) },
                        label = { Text("Group Name") },
                        enabled = isGroupManager,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isGroupManager) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onUpdateDetails(editedName.trim(), editedAvatar.trim()) },
                        enabled = editedName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("group_save_details_button")
                    ) {
                        Text("Save Group Info", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Members (${conversation.participantIds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VibeCyan
                    )
                    if (isGroupManager) {
                        TextButton(
                            onClick = { showAddPicker = !showAddPicker },
                            modifier = Modifier.testTag("group_toggle_add_members")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAddPicker) "Done" else "Add Members")
                        }
                    }
                }
            }

            if (showAddPicker && isGroupManager) {
                if (addableFollowing.isEmpty()) {
                    item {
                        Text(
                            text = "All people you follow are already in this group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(addableFollowing, key = { "add_grp_${it.userId}" }) { candidate ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VibeCyan.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    VibeAvatar(candidate.avatarUrl, candidate.username, size = 34.dp)
                                    Text(candidate.username, fontWeight = FontWeight.Medium)
                                }
                                OutlinedButton(onClick = { onAddMembers(listOf(candidate)) }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }

            items(conversation.participantIds, key = { "grp_mem_$it" }) { memberId ->
                val profile = activeUsers.firstOrNull { it.userId == memberId }
                val memberName = profile?.username ?: conversation.participantNames[memberId] ?: "VibeUser"
                val memberAvatar = profile?.avatarUrl ?: conversation.participantAvatars[memberId] ?: ""
                val isGroupOwner = memberId == conversation.ownerId
                val isAdmin = memberId in conversation.adminIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInspectMember(memberId) }
                        .padding(vertical = 4.dp)
                        .testTag("group_member_row_$memberId"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        VibeAvatar(memberAvatar, memberName, size = 38.dp, vipLevel = profile?.let { if (it.isVipActive) it.vipLevel else 0L } ?: 0L)
                        Column {
                            Text(memberName, fontWeight = FontWeight.SemiBold)
                            if (isGroupOwner || isAdmin) {
                                Text(
                                    text = if (isGroupOwner) "Group Owner" else "Group Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibeGold
                                )
                            }
                        }
                    }
                    if (isGroupManager && !isGroupOwner && memberId != currentUser.userId) {
                        IconButton(
                            onClick = { onRemoveMember(memberId) },
                            modifier = Modifier.testTag("group_remove_member_$memberId")
                        ) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = "Remove $memberName", tint = VibeCoral)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onLeaveGroup,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeCoral),
                    border = BorderStroke(1.dp, VibeCoral.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("group_leave_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Leave Group", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterSheet(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit = {},
    onOpenRoomById: (String) -> Unit,
    onOpenConversationById: (String) -> Unit = {},
    onOpenUserById: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var respondingInviteId by remember { mutableStateOf<String?>(null) }
    var inviteResponseMessage by remember { mutableStateOf("") }
    val relationshipFirestore = remember { FirebaseFirestore.getInstance() }
    val hasUnread = notifications.any { !it.read }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (hasUnread) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("mark_all_notifications_read")
                        ) {
                            Text("Mark All Read")
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Text(
                        text = "You're all caught up! Followers, gifts, invites & messages appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (!item.read) VibeViolet.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (item.type == "RELATIONSHIP_INVITE") Modifier
                                else Modifier.clickable {
                                    onMarkRead(item.id)
                                    when (item.type) {
                                        "ROOM_INVITE", "GIFT" -> if (item.targetId.isNotBlank()) onOpenRoomById(item.targetId)
                                        "MESSAGE" -> if (item.targetId.isNotBlank()) onOpenConversationById(item.targetId)
                                        "FOLLOW" -> if (item.senderId.isNotBlank()) onOpenUserById(item.senderId)
                                    }
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (!item.read) {
                                    Text("NEW", style = MaterialTheme.typography.labelSmall, color = VibePink, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.type == "RELATIONSHIP_INVITE") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            respondingInviteId = item.targetId
                                            inviteResponseMessage = ""
                                            FirebaseFunctions.getInstance("asia-south1")
                                                .getHttpsCallable("respondRelationshipInvite")
                                                .call(mapOf("inviteId" to item.targetId, "accept" to true))
                                                .addOnSuccessListener { result ->
                                                    respondingInviteId = null
                                                    onMarkRead(item.id)
                                                    val response = result.data as? Map<*, *>
                                                    inviteResponseMessage = if (response?.get("expired") == true) {
                                                        "This invitation expired after 48 hours and was automatically rejected."
                                                    } else {
                                                        "Relationship invitation accepted."
                                                    }
                                                }
                                                .addOnFailureListener { error ->
                                                    respondingInviteId = null
                                                    inviteResponseMessage = error.localizedMessage ?: "Could not accept invitation."
                                                }
                                        },
                                        enabled = respondingInviteId == null && item.targetId.isNotBlank()
                                    ) {
                                        Text(if (respondingInviteId == item.targetId) "Please wait…" else "Accept")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            respondingInviteId = item.targetId
                                            inviteResponseMessage = ""
                                            FirebaseFunctions.getInstance("asia-south1")
                                                .getHttpsCallable("respondRelationshipInvite")
                                                .call(mapOf("inviteId" to item.targetId, "accept" to false))
                                                .addOnSuccessListener { result ->
                                                    respondingInviteId = null
                                                    onMarkRead(item.id)
                                                    val response = result.data as? Map<*, *>
                                                    inviteResponseMessage = if (response?.get("expired") == true) {
                                                        "This invitation expired after 48 hours and was automatically rejected."
                                                    } else {
                                                        "Relationship invitation declined."
                                                    }
                                                }
                                                .addOnFailureListener { error ->
                                                    respondingInviteId = null
                                                    inviteResponseMessage = error.localizedMessage ?: "Could not decline invitation."
                                                }
                                        },
                                        enabled = respondingInviteId == null && item.targetId.isNotBlank()
                                    ) {
                                        Text("Reject")
                                    }
                                }
                                if (inviteResponseMessage.isNotBlank() && respondingInviteId == null) {
                                    Text(
                                        inviteResponseMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
