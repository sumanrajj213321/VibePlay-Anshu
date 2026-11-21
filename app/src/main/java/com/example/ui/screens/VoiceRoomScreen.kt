package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.livekit.VoiceConnectionState
import com.example.data.model.GiftBurstEvent
import com.example.data.model.GiftCatalogItem
import com.example.data.model.RoomListener
import com.example.data.model.RoomMessage
import com.example.data.model.RoomOnlineMember
import com.example.data.model.SpeakerRequest
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.example.ui.components.BackendIntegrationGuideSheet
import com.example.ui.components.GiftBurstOverlay
import com.example.ui.components.ReportUserOrRoomDialog
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
import com.example.ui.components.VipEntranceOverlay
import com.example.ui.components.coverDrawableRes
import com.example.ui.state.RoomViewModel
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet
import kotlinx.coroutines.launch

@Composable
fun VoiceRoomScreen(
    roomViewModel: RoomViewModel,
    currentUser: UserProfile,
    activeGifts: List<GiftCatalogItem>,
    blockedUserIds: List<String>,
    followingProfiles: List<UserProfile> = emptyList(),
    followingIds: Set<String> = emptySet(),
    roomEffectsEnabled: Boolean = true,
    onInviteUserToRoom: (userId: String, room: VoiceRoom) -> Unit = { _, _ -> },
    onOpenUserProfile: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    onSubmitReport: (targetType: String, targetId: String, targetName: String, reason: String, details: String) -> Unit,
    onBlockUser: (String) -> Unit
) {
    val room by roomViewModel.room.collectAsStateWithLifecycle()
    val stage by roomViewModel.voiceStage.collectAsStateWithLifecycle()
    val messages by roomViewModel.messages.collectAsStateWithLifecycle()
    val listeners by roomViewModel.listeners.collectAsStateWithLifecycle()
    val onlineUsers by roomViewModel.onlineUsers.collectAsStateWithLifecycle()
    val mySeatInvitation by roomViewModel.mySeatInvitation.collectAsStateWithLifecycle()
    val voiceConnection by roomViewModel.voiceConnectionState.collectAsStateWithLifecycle()
    val giftBurst by roomViewModel.giftBurst.collectAsStateWithLifecycle()
    val vipEntrance by roomViewModel.vipEntrance.collectAsStateWithLifecycle()
    val roomError by roomViewModel.roomError.collectAsStateWithLifecycle()

    BackHandler {
        roomViewModel.leaveRoom(onBack)
    }

    LaunchedEffect(activeGifts) {
        roomViewModel.updateGiftsCatalog(activeGifts)
    }

    LaunchedEffect(room?.id, room?.status) {
        val loaded = room
        if (loaded != null) {
            roomViewModel.onRoomLoaded(loaded, currentUser)
            if (loaded.status == "ENDED") {
                onBack()
            }
        }
    }

    val currentRoom = room
    val currentStage = stage
    if (currentRoom == null || currentStage == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    VoiceRoomStageContent(
        currentRoom = currentRoom,
        currentStage = currentStage,
        currentUser = currentUser,
        messages = messages,
        listeners = listeners,
        onlineUsers = onlineUsers,
        voiceConnection = voiceConnection,
        giftBurst = giftBurst,
        vipEntrance = vipEntrance,
        roomEffectsEnabled = roomEffectsEnabled,
        roomError = roomError,
        onClearError = { roomViewModel.clearError() },
        activeGifts = activeGifts,
        blockedUserIds = blockedUserIds,
        followingProfiles = followingProfiles,
        followingIds = followingIds,
        onInviteUserToRoom = onInviteUserToRoom,
        onOpenUserProfile = onOpenUserProfile,
        onBack = onBack,
        onLeaveRoom = { roomViewModel.leaveRoom(onBack) },
        onSendRoomMessage = { text -> roomViewModel.sendRoomMessage(currentUser, text) },
        onToggleSeatMute = { seat -> roomViewModel.toggleSeatMute(seat) },
        onToggleSeatLock = { seat, locked -> roomViewModel.toggleSeatLock(seat, locked) },
        onMuteAllSpeakers = { roomViewModel.muteAllSpeakers() },
        onJoinSpeakerSeat = { seatIndex -> roomViewModel.joinSpeakerSeat(currentRoom, seatIndex, currentUser) },
        mySeatInvitation = mySeatInvitation,
        onAcceptSeatInvitation = { req -> roomViewModel.respondToSeatInvitation(req, true) },
        onRejectSeatInvitation = { req -> roomViewModel.respondToSeatInvitation(req, false) },
        onClearGiftBurst = { roomViewModel.clearGiftBurst() },
        onClearVipEntrance = { roomViewModel.clearVipEntrance() },
        onSendGift = { recId, recName, gift, qty ->
            roomViewModel.sendGift(currentUser, recId, recName, gift, qty)
        },
        onUpdateRoomSettings = { t, ann, cat, cover, keepCustom, lock, pwd ->
            roomViewModel.updateRoomSettings(t, ann, cat, cover, keepCustom, lock, pwd)
        },
        onUploadRoomCover = { uri -> roomViewModel.uploadRoomCover(uri) },
        onTransferOwnership = { targetSeat ->
            roomViewModel.transferOwnership(currentRoom, targetSeat)
        },
        onPromoteOrDemoteAdmin = { targetUserId, targetUsername, promote ->
            roomViewModel.promoteOrDemoteAdmin(targetUserId, targetUsername, promote)
        },
        onEndRoom = { roomViewModel.endRoom(onBack) },
        onHostAssignUserToSeat = { seatIndex, targetUserId, username, avatarUrl, vipLevel, gender ->
            roomViewModel.hostAssignUserToSeat(
                room = currentRoom,
                seatIndex = seatIndex,
                userId = targetUserId,
                username = username,
                avatarUrl = avatarUrl,
                vipLevel = vipLevel,
                gender = gender
            )
        },
        onVacateSpeakerSeat = { seatIndex ->
            roomViewModel.vacateSpeakerSeat(seatIndex, currentUser)
        },
        onHostRemoveSpeaker = { seatIndex ->
            roomViewModel.hostRemoveSpeaker(seatIndex)
        },
        onSubmitReport = onSubmitReport,
        onBlockUser = onBlockUser
    )
}

@Composable
internal fun VoiceRoomStageContent(
    currentRoom: VoiceRoom,
    currentStage: VoiceStage,
    currentUser: UserProfile,
    messages: List<RoomMessage>,
    listeners: List<RoomListener>,
    onlineUsers: List<RoomOnlineMember> = emptyList(),
    voiceConnection: VoiceConnectionState,
    giftBurst: GiftBurstEvent?,
    vipEntrance: UserProfile? = null,
    roomEffectsEnabled: Boolean = true,
    roomError: String?,
    onClearError: () -> Unit,
    activeGifts: List<GiftCatalogItem>,
    blockedUserIds: List<String>,
    followingProfiles: List<UserProfile> = emptyList(),
    followingIds: Set<String> = emptySet(),
    onInviteUserToRoom: (userId: String, room: VoiceRoom) -> Unit = { _, _ -> },
    onOpenUserProfile: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    onLeaveRoom: () -> Unit,
    onSendRoomMessage: (String) -> Unit,
    onToggleSeatMute: (VoiceSeat) -> Unit,
    onToggleSeatLock: (VoiceSeat, Boolean) -> Unit = { _, _ -> },
    onMuteAllSpeakers: () -> Unit = {},
    onJoinSpeakerSeat: (seatIndex: Int) -> Unit,
    mySeatInvitation: SpeakerRequest? = null,
    onAcceptSeatInvitation: (SpeakerRequest) -> Unit = {},
    onRejectSeatInvitation: (SpeakerRequest) -> Unit = {},
    onClearGiftBurst: () -> Unit,
    onClearVipEntrance: () -> Unit = {},
    onSendGift: (recipientId: String, recipientName: String, gift: GiftCatalogItem, quantity: Long) -> Unit,
    onUpdateRoomSettings: (title: String, announcement: String, category: String, coverKey: String, keepCustomCover: Boolean, locked: Boolean, passcode: String?) -> Unit,
    onUploadRoomCover: (Uri) -> Unit,
    onTransferOwnership: (VoiceSeat) -> Unit,
    onPromoteOrDemoteAdmin: (userId: String, username: String, promote: Boolean) -> Unit = { _, _, _ -> },
    onEndRoom: () -> Unit,
    onHostAssignUserToSeat: (seatIndex: Int, userId: String, username: String, avatarUrl: String, vipLevel: Long, gender: String) -> Unit,
    onVacateSpeakerSeat: (seatIndex: Int) -> Unit,
    onHostRemoveSpeaker: (seatIndex: Int) -> Unit,
    onSubmitReport: (targetType: String, targetId: String, targetName: String, reason: String, details: String) -> Unit,
    onBlockUser: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatListState = rememberLazyListState()
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    var chatInput by remember { mutableStateOf("") }
    var isChatInputFocused by remember { mutableStateOf(false) }
    var showGiftSheet by remember { mutableStateOf(false) }
    var preselectedGiftRecipientId by remember { mutableStateOf<String?>(null) }
    var showHostSettings by remember { mutableStateOf(false) }
    var showOnlineMembers by remember { mutableStateOf(false) }
    var showInvitationDialog by remember { mutableStateOf(false) }
    LaunchedEffect(mySeatInvitation?.id, mySeatInvitation?.status) {
        if (mySeatInvitation?.status == "INVITED") showInvitationDialog = true
    }
    var showSetupGuide by remember { mutableStateOf(false) }
    var inviteTargetSeatIndex by remember { mutableStateOf<Int?>(null) }
    var activeSeatDialog by remember { mutableStateOf<VoiceSeat?>(null) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required to speak on stage.")
            }
        }
    }

    LaunchedEffect(roomError) {
        roomError?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    LaunchedEffect(messages.size, isImeVisible) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.lastIndex)
        }
    }

    val isOwner = currentRoom.isOwner(currentUser.userId)
    val isRoomAdmin = currentRoom.isRoomAdmin(currentUser.userId)
    val isManager = currentRoom.isManager(currentUser.userId)
    val allNineSeats = listOf(currentStage.hostSeat) + currentStage.speakerSeats
    val mySeat = allNineSeats.firstOrNull { it.occupantId == currentUser.userId }
    val visibleMessages = messages.filter { it.senderId !in blockedUserIds }
    val computedOnlineUsers = remember(onlineUsers, currentRoom, currentStage, listeners) {
        if (onlineUsers.isNotEmpty()) {
            onlineUsers
        } else {
            VoiceStage.buildOnlineMembersList(currentRoom, currentStage, listeners)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .testTag("room_bottom_control_bar")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it.take(400) },
                            placeholder = { Text("Say hi...", maxLines = 1) },
                            singleLine = false,
                            minLines = 1,
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = VibeViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            trailingIcon = {
                                if (chatInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onSendRoomMessage(chatInput)
                                            chatInput = ""
                                        },
                                        modifier = Modifier.testTag("room_send_chat_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send chat message",
                                            tint = VibeViolet
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isChatInputFocused = it.isFocused }
                                .testTag("room_chat_input")
                        )

                        // Button 1: Microphone (always visible)
                        val micBg = when {
                            mySeat == null -> MaterialTheme.colorScheme.surfaceVariant
                            mySeat.muted -> VibeCoral
                            else -> VibeEmerald
                        }
                        val micTint = if (mySeat == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.White
                        }
                        Surface(
                            shape = CircleShape,
                            color = micBg,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    when {
                                        mySeat != null -> {
                                            if (!hasAudioPermission && mySeat.muted) {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                onToggleSeatMute(mySeat)
                                            }
                                        }
                                        else -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Tap an open seat to join the stage.")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("room_mic_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (mySeat == null || mySeat.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = when {
                                        mySeat == null -> "Microphone unavailable as listener"
                                        mySeat.muted -> "Unmute mic"
                                        else -> "Mute mic"
                                    },
                                    tint = micTint
                                )
                            }
                        }

                        // Room controls: managers can open the online member list; seated users can manage their seat.
                        if (isManager || mySeat != null) {
                            val controlTint = if (isManager) VibeGold else Color.White
                            Surface(
                                shape = CircleShape,
                                color = if (isManager) MaterialTheme.colorScheme.surfaceVariant else VibeEmerald,
                                modifier = Modifier.size(48.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isManager) showOnlineMembers = true else activeSeatDialog = mySeat
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag(if (isManager) "host_online_members_button" else "my_speaker_seat_button")
                                ) {
                                    Icon(
                                        imageVector = if (isManager) Icons.Filled.People else Icons.Filled.Mic,
                                        contentDescription = if (isManager) "Online room members" else "Manage my speaker seat",
                                        tint = controlTint
                                    )
                                }
                            }
                        }

                        // Button 3: Virtual Gift (always visible, opaque solid color)
                        Surface(
                            shape = CircleShape,
                            color = VibePink,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    preselectedGiftRecipientId = null
                                    showGiftSheet = true
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("room_open_gifts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CardGiftcard,
                                    contentDescription = "Send gift",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle atmospheric cover backdrop
            if (currentRoom.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = currentRoom.coverUrl,
                    contentDescription = "Room cover",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.18f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = coverDrawableRes(currentRoom.coverKey)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.18f),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                VibeViolet.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Top Room Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentRoom.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentRoom.locked) {
                                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = VibePink, modifier = Modifier.size(16.dp))
                            }
                        }
                        val roleTag = when {
                            isOwner -> " · Host"
                            isRoomAdmin -> " · Admin"
                            else -> ""
                        }
                        Text(
                            text = "${currentRoom.category} · 9-Seat Stage$roleTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeCyan
                        )
                        Text(
                            text = "ROOM ID · ${currentRoom.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("room_id_text")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clickable { showOnlineMembers = true }
                                .testTag("room_listeners_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Headphones, contentDescription = "Online users", tint = VibeCyan, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "${computedOnlineUsers.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("room_online_count_text")
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join my VibePlay voice room \"${currentRoom.title}\" (Room ID: ${currentRoom.id})!"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Voice Room"))
                            },
                            modifier = Modifier.testTag("room_share_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share room")
                        }

                        if (isManager) {
                            IconButton(
                                onClick = { showHostSettings = true },
                                modifier = Modifier.testTag("host_settings_button")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Stage controls", tint = VibeGold)
                            }
                        } else {
                            IconButton(
                                onClick = { reportTarget = Triple("ROOM", currentRoom.id, currentRoom.title) },
                                modifier = Modifier.testTag("report_room_button")
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = "Report room")
                            }
                        }

                        IconButton(
                            onClick = onLeaveRoom,
                            modifier = Modifier.testTag("leave_room_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave room", tint = VibeCoral)
                        }
                    }
                }

                // 2. Honest LiveKit Connection Status Banner + Announcement
                LiveKitStatusPill(
                    state = voiceConnection,
                    hasAudioPermission = hasAudioPermission,
                    onOpenGuide = { showSetupGuide = true }
                )

                if (currentRoom.announcement.isNotBlank() && !isImeVisible) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\uD83D\uDCE2 ${currentRoom.announcement}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                // 3. THE STRICT 9-SEAT STAGE (1 Host Seat + 8 Regular Speaker Seats)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nine_seat_stage")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dedicated Host/Owner Seat (Seat 0)
                        HostSeatTile(
                            seat = currentStage.hostSeat,
                            ownerOnline = currentRoom.effectiveOwnerOnline,
                            onClick = { activeSeatDialog = currentStage.hostSeat }
                        )

                        // Exactly 8 Regular Speaker Seats (Seats 1..8) in 2 rows of 4
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentStage.speakerSeats.chunked(4).forEach { rowSeats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowSeats.forEach { seat ->
                                        SpeakerSeatTile(
                                            seat = seat,
                                            isRoomAdmin = seat.isOccupied && currentRoom.isRoomAdmin(seat.occupantId),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                when {
                                                    seat.isOccupied -> activeSeatDialog = seat
                                                    isManager -> inviteTargetSeatIndex = seat.seatIndex.toInt()
                                                    mySeat != null -> {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("You are already speaking on Seat ${mySeat.seatIndex}.")
                                                        }
                                                    }
                                                    seat.seatLocked -> {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Seat ${seat.seatIndex} is locked.")
                                                        }
                                                    }
                                                    else -> onJoinSpeakerSeat(seat.seatIndex.toInt())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Live Text Chat Stream
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("room_chat_messages_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = visibleMessages,
                        key = { index, msg ->
                            msg.id.ifBlank { "room_msg_${index}_${msg.senderId}_${msg.text.hashCode()}" }
                        }
                    ) { _, msg ->
                        val bgColor = when (msg.type) {
                            "GIFT" -> VibePink.copy(alpha = 0.2f)
                            "SYSTEM" -> VibeCyan.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        val canOpenProfile = msg.type != "SYSTEM" &&
                            msg.senderId.isNotBlank() &&
                            onOpenUserProfile != null
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bgColor,
                            modifier = if (canOpenProfile) {
                                Modifier
                                    .clickable { onOpenUserProfile!!(msg.senderId) }
                                    .testTag("room_chat_msg_${msg.id}")
                            } else {
                                Modifier
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (msg.type != "SYSTEM") {
                                    VibeAvatar(
                                        avatarUrl = msg.senderAvatarUrl,
                                        username = msg.senderName,
                                        size = 24.dp,
                                        vipLevel = msg.senderVipLevel
                                    )
                                }
                                if (msg.senderVipLevel > 0) {
                                    VipBadgePill(msg.senderVipLevel)
                                }
                                Text(
                                    text = if (msg.type == "SYSTEM") msg.text else "${msg.senderName}: ${msg.text}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (msg.type == "GIFT") VibeGold else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (roomEffectsEnabled) {
                // VIP Stage Entrance Banner
                VipEntranceOverlay(
                    user = vipEntrance,
                    onFinished = onClearVipEntrance,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 46.dp)
                )

                // Floating Animated Virtual Gift Burst Overlay
                GiftBurstOverlay(
                    event = giftBurst,
                    onFinished = onClearGiftBurst,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 84.dp)
                )
            }
        }
    }

    if (showGiftSheet) {
        RoomGiftStoreSheet(
            currentUser = currentUser,
            stage = currentStage,
            gifts = activeGifts,
            initialRecipientId = preselectedGiftRecipientId,
            onDismiss = {
                showGiftSheet = false
                preselectedGiftRecipientId = null
            },
            onSendGift = { recId, recName, gift, qty ->
                onSendGift(recId, recName, gift, qty)
            }
        )
    }

    if (showHostSettings && isManager) {
        HostSettingsSheet(
            room = currentRoom,
            stage = currentStage,
            isOwner = isOwner,
            onDismiss = { showHostSettings = false },
            onSaveSettings = { t, ann, cat, cover, keepCustom, lock, pwd ->
                onUpdateRoomSettings(t, ann, cat, cover, keepCustom, lock, pwd)
            },
            onUploadCover = onUploadRoomCover,
            onMuteAllSpeakers = onMuteAllSpeakers,
            onTransferOwnership = { targetSeat ->
                onTransferOwnership(targetSeat)
            },
            onEndRoom = {
                onEndRoom()
            }
        )
    }

    inviteTargetSeatIndex?.let { seatIndex ->
        if (isManager) {
            val targetSeat = currentStage.speakerSeats.firstOrNull { it.seatIndex.toInt() == seatIndex }
            InviteSpeakerSheet(
                targetSeatIndex = seatIndex,
                room = currentRoom,
                stage = currentStage,
                listeners = listeners,
                followingProfiles = followingProfiles,
                followingIds = followingIds,
                onToggleSeatLock = targetSeat?.let { s ->
                    { locked -> onToggleSeatLock(s, locked) }
                },
                onOpenUserProfile = onOpenUserProfile,
                onAssignToSeat = { targetUserId, username, avatarUrl, vipLevel, gender ->
                    onHostAssignUserToSeat(
                        seatIndex,
                        targetUserId,
                        username,
                        avatarUrl,
                        vipLevel,
                        gender
                    )
                },
                onInviteUserToRoom = { targetId ->
                    onInviteUserToRoom(targetId, currentRoom)
                    scope.launch {
                        snackbarHostState.showSnackbar("Room invitation sent!")
                    }
                },
                onDismiss = { inviteTargetSeatIndex = null }
            )
        }
    }

    if (showInvitationDialog) {
        mySeatInvitation?.takeIf { it.status == "INVITED" }?.let { invitation ->
            SeatInvitationDialog(
                request = invitation,
                onAcceptInvitation = { onAcceptSeatInvitation(invitation) },
                onRejectInvitation = { onRejectSeatInvitation(invitation) },
                onDismiss = { showInvitationDialog = false }
            )
        }
    }

    if (showOnlineMembers) {
        val firstOpenSeat = VoiceStage.findAvailableSpeakerSeat(currentStage)
        RoomOnlineMembersSheet(
            isHost = isManager,
            isOwner = isOwner,
            currentUserId = currentUser.userId,
            onlineUsers = computedOnlineUsers,
            listeners = listeners,
            hasOpenSpeakerSeat = firstOpenSeat != null,
            onAssignListenerToOpenSeat = { listener ->
                firstOpenSeat?.let { openSeat ->
                    onHostAssignUserToSeat(
                        openSeat.seatIndex.toInt(),
                        listener.userId,
                        listener.username,
                        listener.avatarUrl,
                        listener.vipLevel,
                        listener.gender
                    )
                    showOnlineMembers = false
                }
            },
            onOpenUserProfile = onOpenUserProfile,
            onToggleRoomAdmin = onPromoteOrDemoteAdmin,
            onReportListener = { listener: RoomListener ->
                showOnlineMembers = false
                reportTarget = Triple("USER", listener.userId, listener.username)
            },
            onBlockListener = { listener ->
                onBlockUser(listener.userId)
            },
            onDismiss = { showOnlineMembers = false }
        )
    }

    activeSeatDialog?.let { seat ->
        SpeakerSeatActionDialog(
            seat = seat,
            isCurrentUserHost = isManager,
            isCurrentUserOwner = isOwner,
            isOccupantRoomAdmin = !seat.isHostSeat && currentRoom.isRoomAdmin(seat.occupantId),
            ownerOnline = if (seat.isHostSeat) currentRoom.effectiveOwnerOnline else true,
            isMySeat = seat.occupantId == currentUser.userId,
            onDismiss = { activeSeatDialog = null },
            onToggleMute = { onToggleSeatMute(seat) },
            onRemoveFromSeat = {
                if (seat.occupantId == currentUser.userId) {
                    onVacateSpeakerSeat(seat.seatIndex.toInt())
                } else {
                    onHostRemoveSpeaker(seat.seatIndex.toInt())
                }
            },
            onToggleRoomAdmin = { promote ->
                onPromoteOrDemoteAdmin(seat.occupantId, seat.occupantName, promote)
            },
            onQuickGift = {
                preselectedGiftRecipientId = seat.occupantId
                showGiftSheet = true
            },
            onViewProfile = onOpenUserProfile?.let { openProfile ->
                { openProfile(seat.occupantId) }
            },
            onReportUser = {
                reportTarget = Triple("USER", seat.occupantId, seat.occupantName)
            },
            onBlockUser = {
                onBlockUser(seat.occupantId)
            }
        )
    }

    reportTarget?.let { (type, id, name) ->
        ReportUserOrRoomDialog(
            targetName = name,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                onSubmitReport(type, id, name, reason, details)
                reportTarget = null
            }
        )
    }

    if (showSetupGuide) {
        BackendIntegrationGuideSheet(onDismiss = { showSetupGuide = false })
    }
}

@Composable
private fun LiveKitStatusPill(
    state: VoiceConnectionState,
    hasAudioPermission: Boolean,
    onOpenGuide: () -> Unit
) {
    val (label, tint) = when (state) {
        is VoiceConnectionState.Connected ->
            "LiveKit Voice Connected · ${if (hasAudioPermission) "Mic Ready" else "Mic Permission Needed"}" to VibeEmerald
        is VoiceConnectionState.Connecting ->
            "Connecting LiveKit Audio..." to VibeCyan
        is VoiceConnectionState.Unconfigured ->
            "Voice service not configured yet" to VibeGold
        is VoiceConnectionState.Error ->
            "LiveKit Audio: ${state.message}" to VibeCoral
        VoiceConnectionState.Disconnected ->
            "LiveKit Ready" to VibeCyan
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGuide)
            .testTag("livekit_status_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HostSeatTile(
    seat: VoiceSeat,
    ownerOnline: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("host_seat_0")
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            if (ownerOnline) {
                VibeAvatar(
                    avatarUrl = seat.occupantAvatarUrl,
                    username = seat.occupantName,
                    size = 76.dp,
                    vipLevel = seat.occupantVipLevel,
                    speaking = !seat.muted
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("host_seat_offline_placeholder"),
                    contentAlignment = Alignment.Center
                ) {
                    VibeAvatar(
                        avatarUrl = seat.occupantAvatarUrl,
                        username = seat.occupantName,
                        size = 76.dp,
                        vipLevel = seat.occupantVipLevel,
                        modifier = Modifier.alpha(0.38f),
                        frameColorArgb = seat.occupantFrameColorArgb
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.62f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            if (ownerOnline) {
                Surface(
                    shape = CircleShape,
                    color = if (seat.muted) VibeCoral else VibeEmerald,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = if (seat.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (seat.muted) "Host muted" else "Host mic active",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = VibeCoral,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier.testTag("host_offline_badge")
                ) {
                    Text(
                        text = "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = VibeGold.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = VibeGold, modifier = Modifier.size(12.dp))
                Text(
                    text = "HOST · ${seat.occupantName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpeakerSeatTile(
    seat: VoiceSeat,
    isRoomAdmin: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .clickable(onClick = onClick)
            .testTag("speaker_seat_${seat.seatIndex}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (seat.isOccupied) {
            Box(contentAlignment = Alignment.BottomEnd) {
                VibeAvatar(
                    avatarUrl = seat.occupantAvatarUrl,
                    username = seat.occupantName,
                    size = 54.dp,
                    vipLevel = seat.occupantVipLevel,
                    speaking = !seat.muted
                )
                Surface(
                    shape = CircleShape,
                    color = if (seat.muted) VibeCoral else VibeEmerald,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = if (seat.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (seat.muted) "Muted" else "Unmuted",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(11.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = seat.occupantName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (seat.seatLocked) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = VibePink.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, VibePink.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Seat locked",
                            tint = VibePink,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibePink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (isRoomAdmin) {
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeGold,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (seat.seatLocked) VibePink.copy(alpha = 0.55f) else VibeCyan.copy(alpha = 0.55f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (seat.seatLocked) Icons.Filled.Lock else Icons.Filled.Add,
                        contentDescription = if (seat.seatLocked) "Locked speaker seat ${seat.seatIndex}" else "Open speaker seat ${seat.seatIndex} · Tap to join",
                        tint = if (seat.seatLocked) VibePink else VibeCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Seat ${seat.seatIndex}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (seat.seatLocked) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = VibePink.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, VibePink.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Seat locked",
                            tint = VibePink,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibePink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
