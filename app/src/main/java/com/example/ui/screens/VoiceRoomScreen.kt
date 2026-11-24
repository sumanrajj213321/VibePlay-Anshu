package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import java.nio.ByteBuffer
import java.security.MessageDigest
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.agora.VoiceConnectionState
import com.example.data.model.GiftBurstEvent
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftTransaction
import com.example.data.model.RoomListener
import com.example.data.model.RoomMessage
import com.example.data.model.RoomMusicState
import com.example.data.model.RoomOnlineMember
import com.example.data.model.SpeakerRequest
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.example.ui.components.GiftBurstOverlay
import com.example.ui.components.ReportUserOrRoomDialog
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
import com.example.ui.components.VipShiningNameText
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
import java.util.Calendar
import java.util.TimeZone

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
    onBlockUser: (String) -> Unit,
    onUnblockUser: (String) -> Unit = {}
) {
    val room by roomViewModel.room.collectAsStateWithLifecycle()
    val stage by roomViewModel.voiceStage.collectAsStateWithLifecycle()
    val messages by roomViewModel.messages.collectAsStateWithLifecycle()
    val roomGiftTransactions by roomViewModel.roomGiftTransactions.collectAsStateWithLifecycle()
    val listeners by roomViewModel.listeners.collectAsStateWithLifecycle()
    val onlineUsers by roomViewModel.onlineUsers.collectAsStateWithLifecycle()
    val mySeatInvitation by roomViewModel.mySeatInvitation.collectAsStateWithLifecycle()
    val voiceConnection by roomViewModel.voiceConnectionState.collectAsStateWithLifecycle()
    val activeSpeakerUids by roomViewModel.activeSpeakerUids.collectAsStateWithLifecycle()
    val giftBurst by roomViewModel.giftBurst.collectAsStateWithLifecycle()
    val vipEntrance by roomViewModel.vipEntrance.collectAsStateWithLifecycle()
    val roomError by roomViewModel.roomError.collectAsStateWithLifecycle()
    val roomMusic by roomViewModel.roomMusic.collectAsStateWithLifecycle()
    val roomSessionRemoved by roomViewModel.roomSessionRemoved.collectAsStateWithLifecycle()

    LaunchedEffect(roomSessionRemoved) {
        if (roomSessionRemoved) onBack()
    }

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
        giftTransactions = roomGiftTransactions,
        listeners = listeners,
        onlineUsers = onlineUsers,
        voiceConnection = voiceConnection,
        activeSpeakerUids = activeSpeakerUids,
        giftBurst = giftBurst,
        vipEntrance = vipEntrance,
        roomEffectsEnabled = roomEffectsEnabled,
        roomError = roomError,
        roomMusic = roomMusic,
        onUpdateRoomMusic = { trackId, status, positionMs, queueTrackIds ->
            roomViewModel.updateRoomMusic(trackId, status, positionMs, queueTrackIds)
        },
        onAddTrackToQueue = { trackId -> roomViewModel.addTrackToRoomMusicQueue(trackId) },
        onAdvanceRoomMusicQueue = { trackId -> roomViewModel.advanceRoomMusicQueue(trackId) },
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
        onEnableMicrophone = { roomViewModel.enableMicrophone() },
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
        onBlockUser = onBlockUser,
        onUnblockUser = onUnblockUser,
        onKickMember = { targetId -> roomViewModel.kickRoomMember(targetId) }
    )
}

@Composable
internal fun VoiceRoomStageContent(
    currentRoom: VoiceRoom,
    currentStage: VoiceStage,
    currentUser: UserProfile,
    messages: List<RoomMessage>,
    giftTransactions: List<GiftTransaction> = emptyList(),
    listeners: List<RoomListener>,
    onlineUsers: List<RoomOnlineMember> = emptyList(),
    voiceConnection: VoiceConnectionState,
    activeSpeakerUids: Set<Int> = emptySet(),
    giftBurst: GiftBurstEvent?,
    vipEntrance: UserProfile? = null,
    roomEffectsEnabled: Boolean = true,
    roomError: String?,
    roomMusic: RoomMusicState? = null,
    onUpdateRoomMusic: (trackId: String, status: String, positionMs: Long, queueTrackIds: List<String>) -> Unit = { _, _, _, _ -> },
    onAddTrackToQueue: (trackId: String) -> Unit = {},
    onAdvanceRoomMusicQueue: (expectedTrackId: String) -> Unit = {},
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
    onEnableMicrophone: () -> Unit,
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
    onBlockUser: (String) -> Unit,
    onUnblockUser: (String) -> Unit = {},
    onKickMember: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatListState = rememberLazyListState()
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    var chatInput by remember { mutableStateOf("") }
    var isChatInputFocused by remember { mutableStateOf(false) }
    var showGiftSheet by remember { mutableStateOf(false) }
    var preselectedGiftRecipientId by remember { mutableStateOf<String?>(null) }
    var showHostSettings by remember { mutableStateOf(false) }
    var showRoomMusic by remember { mutableStateOf(false) }
    var showOnlineMembers by remember { mutableStateOf(false) }
    var showInvitationDialog by remember { mutableStateOf(false) }
    LaunchedEffect(mySeatInvitation?.id, mySeatInvitation?.status) {
        if (mySeatInvitation?.status == "INVITED") showInvitationDialog = true
    }
    var inviteTargetSeatIndex by remember { mutableStateOf<Int?>(null) }
    var activeSeatDialog by remember { mutableStateOf<VoiceSeat?>(null) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var pendingSeatAfterPermission by remember { mutableStateOf<Int?>(null) }
    var pendingMicEnable by remember { mutableStateOf(false) }

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
        if (granted) {
            val pendingSeat = pendingSeatAfterPermission
            pendingSeatAfterPermission = null
            if (pendingSeat != null) {
                onJoinSpeakerSeat(pendingSeat)
            } else if (pendingMicEnable) {
                pendingMicEnable = false
                onEnableMicrophone()
            }
        } else {
            pendingSeatAfterPermission = null
            pendingMicEnable = false
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
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
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
                                            if (!hasAudioPermission) {
                                                pendingMicEnable = true
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
                                        imageVector = if (isManager) Icons.Filled.People else Icons.Filled.MoreVert,
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
                            text = "ROOM ID · ${currentRoom.publicRoomId.ifBlank { currentRoom.id }}",
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
                            onClick = { showRoomMusic = true },
                            modifier = Modifier.testTag("room_music_button")
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = "Room music", tint = VibeCyan)
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

                RoomGiftRankingStrip(giftTransactions, onOpenUserProfile)

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
                            isSpeaking = currentRoom.effectiveOwnerOnline && !currentStage.hostSeat.muted &&
                                agoraUidForUser(currentStage.hostSeat.occupantId) in activeSpeakerUids,
                            roomMusic = roomMusic,
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
                                            isSpeaking = seat.isOccupied && !seat.muted &&
                                                agoraUidForUser(seat.occupantId) in activeSpeakerUids,
                                            roomMusic = roomMusic,
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
                                                    else -> {
                                                        if (!hasAudioPermission) {
                                                            pendingSeatAfterPermission = seat.seatIndex.toInt()
                                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        } else {
                                                            onJoinSpeakerSeat(seat.seatIndex.toInt())
                                                        }
                                                    }
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
                            msg.type != "GIFT" &&
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                if (msg.type != "SYSTEM") {
                                    VibeAvatar(
                                        avatarUrl = msg.senderAvatarUrl,
                                        username = msg.senderName,
                                        size = 42.dp,
                                        vipLevel = msg.senderVipLevel
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            VipShiningNameText(
                                                text = msg.senderName.ifBlank { "User" },
                                                vipLevel = msg.senderVipLevel,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (msg.senderVipLevel > 0) {
                                                VipBadgePill(msg.senderVipLevel, compact = true)
                                            }
                                        }
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (msg.type == "GIFT") VibeGold else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (msg.type == "GIFT") {
                                            Text(
                                                text = "Get ${msg.giftCoins} Gifts",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = VibeGold
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (msg.senderName.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                VipShiningNameText(
                                                    text = msg.senderName,
                                                    vipLevel = msg.senderVipLevel,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (msg.senderVipLevel > 0L) {
                                                    VipBadgePill(msg.senderVipLevel, compact = true)
                                                }
                                            }
                                        }
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (roomEffectsEnabled) {
                VipEntranceOverlay(
                    user = vipEntrance,
                    onFinished = onClearVipEntrance,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 46.dp)
                )
            }
            // Gift animations are independent of the optional VIP effects toggle.
            GiftBurstOverlay(event = giftBurst, onFinished = onClearGiftBurst, modifier = Modifier.fillMaxSize())
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
            blockedUserIds = blockedUserIds,
            onBlockListener = { listener ->
                onBlockUser(listener.userId)
            },
            onUnblockListener = { listener -> onUnblockUser(listener.userId) },
            onKickMember = onKickMember,
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

    RoomMusicPanel(
        visible = showRoomMusic,
        roomMusic = roomMusic,
        canControl = isManager || mySeat != null,
        canUpload = isManager || mySeat != null,
        onUpdateRoomMusic = onUpdateRoomMusic,
        onAddTrackToQueue = onAddTrackToQueue,
        onAdvanceQueue = onAdvanceRoomMusicQueue,
        onDismiss = { showRoomMusic = false }
    )

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

}

@Composable
private fun LiveKitStatusPill(
    state: VoiceConnectionState,
    hasAudioPermission: Boolean,
    onOpenGuide: () -> Unit
) {
    val (label, tint) = when (state) {
        is VoiceConnectionState.Connected ->
            "Agora Voice Connected · ${if (hasAudioPermission) "Mic Ready" else "Mic Permission Needed"}" to VibeEmerald
        is VoiceConnectionState.Connecting ->
            "Connecting Agora Audio..." to VibeCyan
        is VoiceConnectionState.Unconfigured ->
            "Voice service not configured yet" to VibeGold
        is VoiceConnectionState.Error ->
            "Agora Audio: ${state.message}" to VibeCoral
        VoiceConnectionState.Disconnected ->
            "Agora Ready" to VibeCyan
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
private fun MusicNotePulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "music_note_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), repeatMode = RepeatMode.Reverse),
        label = "music_note_alpha"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        listOf(
            Alignment.TopCenter,
            Alignment.CenterEnd,
            Alignment.BottomCenter,
            Alignment.CenterStart
        ).forEachIndexed { index, alignment ->
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = if (index == 0) "Music playing" else null,
                tint = if (index % 2 == 0) VibeCyan.copy(alpha = alpha) else VibePink.copy(alpha = alpha),
                modifier = Modifier
                    .align(alignment)
                    .size(13.dp)
            )
        }
    }
}

@Composable
private fun VoiceActivityRings(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voice_activity_rings")
    val firstProgress by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Restart),
        label = "voice_ring_one"
    )
    val secondProgress by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(900, delayMillis = 450), repeatMode = RepeatMode.Restart),
        label = "voice_ring_two"
    )
    Canvas(modifier = modifier) {
        // Start near the avatar edge and expand well beyond it like the old seat wavefront.
        val baseRadius = size.minDimension * 0.42f
        drawCircle(
            color = VibeCyan.copy(alpha = (1f - (firstProgress - 0.88f) / 0.67f) * 0.82f),
            radius = baseRadius * firstProgress,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = VibeViolet.copy(alpha = (1f - (secondProgress - 0.88f) / 0.67f) * 0.82f),
            radius = baseRadius * secondProgress,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun agoraUidForUser(userId: String): Int {
    if (userId.isBlank()) return -1
    val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray(Charsets.UTF_8))
    return (ByteBuffer.wrap(digest, 0, 4).int and 0x7fffffff).takeIf { it != 0 } ?: 1
}

@Composable
private fun HostSeatTile(
    seat: VoiceSeat,
    ownerOnline: Boolean,
    isSpeaking: Boolean,
    roomMusic: RoomMusicState? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("host_seat_0")
    ) {
        Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            val isMusicPlayingHere = roomMusic?.status == "PLAYING" &&
                roomMusic?.updatedBy == seat.occupantId
            if ((ownerOnline && isSpeaking) || isMusicPlayingHere) {
                VoiceActivityRings(modifier = Modifier.size(92.dp))
            }
            if (isMusicPlayingHere) {
                MusicNotePulse(modifier = Modifier.size(92.dp))
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                if (ownerOnline) {
                    VibeAvatar(
                        avatarUrl = seat.occupantAvatarUrl,
                        username = seat.occupantName,
                        size = 76.dp,
                        vipLevel = seat.occupantVipLevel,
                        speaking = isSpeaking,
                        frameColorArgb = seat.occupantFrameColorArgb
                    )
                } else {
                    Box(
                        modifier = Modifier.size(76.dp),
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
                            modifier = Modifier.padding(4.dp).size(14.dp)
                        )
                    }
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
                VipShiningNameText(
                    text = "HOST · ${seat.occupantName}",
                    vipLevel = seat.occupantVipLevel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
    isSpeaking: Boolean = false,
    roomMusic: RoomMusicState? = null,
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
            Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                val isMusicPlayingHere = roomMusic?.status == "PLAYING" &&
                    roomMusic?.updatedBy == seat.occupantId
                if (isSpeaking || isMusicPlayingHere) {
                    VoiceActivityRings(modifier = Modifier.size(70.dp))
                }
                if (isMusicPlayingHere) {
                    MusicNotePulse(modifier = Modifier.size(70.dp))
                }
                Box(contentAlignment = Alignment.BottomEnd) {
                    VibeAvatar(
                        avatarUrl = seat.occupantAvatarUrl,
                        username = seat.occupantName,
                        size = 54.dp,
                        vipLevel = seat.occupantVipLevel,
                        speaking = isSpeaking,
                        frameColorArgb = seat.occupantFrameColorArgb
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
                            modifier = Modifier.padding(3.dp).size(11.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            VipShiningNameText(
                text = seat.occupantName,
                vipLevel = seat.occupantVipLevel,
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

        }
    }
}


private data class RoomGiftRankingEntry(val userId: String, val username: String, val coins: Long)

private fun aggregateRoomGiftRanks(
    transactions: List<GiftTransaction>,
    receivers: Boolean,
    cutoffMillis: Long
): List<RoomGiftRankingEntry> = transactions
    .filter { (it.createdAt?.toDate()?.time ?: 0L) >= cutoffMillis }
    .groupBy { if (receivers) it.recipientId else it.senderId }
    .mapNotNull { (userId, rows) ->
        if (userId.isBlank()) null else {
            val first = rows.first()
            RoomGiftRankingEntry(
                userId,
                (if (receivers) first.recipientName else first.senderName).ifBlank { "Vibe User" },
                rows.sumOf { it.coinCost.coerceAtLeast(0L) }
            )
        }
    }
    .sortedByDescending { it.coins }
    .take(10)

@Composable
private fun RoomGiftRankingStrip(
    transactions: List<GiftTransaction>,
    onOpenUserProfile: ((String) -> Unit)?
) {
    var showRanking by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(0) }
    var selectedPeriod by remember { mutableStateOf(2) }
    val timezone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val cutoffMillis = remember(selectedPeriod) {
        when (selectedPeriod) {
            0 -> Calendar.getInstance(timezone).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            1 -> Calendar.getInstance(timezone).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            else -> 0L
        }
    }
    val topGifter = remember(transactions) { aggregateRoomGiftRanks(transactions, false, 0L).firstOrNull() }
    val topReceiver = remember(transactions) { aggregateRoomGiftRanks(transactions, true, 0L).firstOrNull() }
    val ranking = remember(transactions, selectedType, cutoffMillis) {
        aggregateRoomGiftRanks(transactions, selectedType == 1, cutoffMillis)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showRanking = true }.testTag("room_gift_rankings_entry"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.35f))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🏆", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Room Gift Rankings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Gifter: ${topGifter?.username ?: "—"}  ·  Receiver: ${topReceiver?.username ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("View ›", style = MaterialTheme.typography.labelMedium, color = VibeGold, fontWeight = FontWeight.Bold)
        }
    }

    if (showRanking) Dialog(onDismissRequest = { showRanking = false }) {
        Surface(
            Modifier.fillMaxWidth(0.96f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.5f))
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Room Gift Rankings", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showRanking = false }) { Text("Close") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Top Gifters", "Top Receivers").forEachIndexed { index, label ->
                        Surface(
                            Modifier.weight(1f).clickable { selectedType = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedType == index) VibeViolet.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(label, Modifier.padding(vertical = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Daily", "Weekly", "Total").forEachIndexed { index, label ->
                        TextButton(onClick = { selectedPeriod = index }) {
                            Text(label, color = if (selectedPeriod == index) VibeGold else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selectedPeriod == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                if (ranking.isEmpty()) {
                    Text("No room gifts for this period yet.", Modifier.fillMaxWidth().padding(vertical = 22.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(ranking, key = { _, row -> row.userId }) { index, row ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { onOpenUserProfile?.invoke(row.userId); showRanking = false },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("#${index + 1}", color = if (index == 0) VibeGold else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text(row.username, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text("%,d coins".format(row.coins), color = VibeGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
