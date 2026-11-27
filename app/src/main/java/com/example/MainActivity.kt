package com.example

import android.os.Bundle
import android.os.Build
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.data.model.GlobalGiftAnnouncement
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firebase.AuthHelper
import com.example.data.firebase.PreferencesRepository
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.data.repository.InboxRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoomRepository
import com.example.data.repository.UserRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FruitTreasureScreen
import com.example.ui.screens.GiftRankingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.NotificationsCenterSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.CharmLevelsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.RelationshipScreen
import com.example.ui.screens.PublicProfileSheet
import com.example.ui.screens.RoomsHubScreen
import com.example.ui.screens.RoomMusicPlayback
import com.example.ui.screens.VoiceRoomScreen
import com.example.ui.state.RoomViewModel
import com.example.ui.state.UiState
import com.example.ui.state.VibePlayViewModel
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePlayTheme
import com.example.ui.theme.VibeViolet
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

private sealed class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : MainTab("home", "Home", Icons.Filled.Home)
    data object Explore : MainTab("explore", "Explore", Icons.Filled.Explore)
    data object Rooms : MainTab("rooms", "Rooms", Icons.Filled.GraphicEq)
    data object Inbox : MainTab("inbox", "Inbox", Icons.Filled.ChatBubble)
    data object Profile : MainTab("profile", "Profile", Icons.Filled.Person)
}

private const val ROUTE_VOICE_ROOM = "room/{roomId}"

class MainActivity : ComponentActivity() {
    private var voiceRoomActive: Boolean = false

    fun setVoiceRoomActive(active: Boolean) {
        voiceRoomActive = active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setAutoEnterEnabled(active)
                    .build()
            )
        }
    }

    fun minimizeVoiceRoomToPictureInPicture(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !voiceRoomActive) return false
        return try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (_: IllegalStateException) {
            false
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (voiceRoomActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            minimizeVoiceRoomToPictureInPicture()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = PreferencesRepository(applicationContext)
        setContent {
            val darkThemeFlow = remember(prefs) { prefs.darkThemeFlow }
            val authFlow = remember { AuthHelper.authStateFlow() }
            val darkTheme by darkThemeFlow.collectAsStateWithLifecycle(initialValue = true)
            val currentUser by authFlow.collectAsStateWithLifecycle(
                initialValue = Firebase.auth.currentUser
            )

            VibePlayTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        AuthScreen(onAuthSuccess = {})
                    } else {
                        val user = currentUser!!
                        key(user.uid) {
                            AuthenticatedVibePlayApp(
                                uid = user.uid,
                                firebaseUser = user
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedVibePlayApp(
    uid: String,
    firebaseUser: FirebaseUser
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }


    val viewModel: VibePlayViewModel = viewModel(
        key = "vibe_$uid",
        factory = viewModelFactory {
            initializer {
                VibePlayViewModel(
                    currentUserId = uid,
                    firebaseUser = firebaseUser,
                    userRepository = UserRepository(appContext),
                    roomRepository = RoomRepository(appContext),
                    inboxRepository = InboxRepository(appContext),
                    notificationRepository = NotificationRepository(appContext),
                    preferencesRepository = PreferencesRepository(appContext)
                )
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startOnlineHeartbeat()
                Lifecycle.Event.ON_STOP -> viewModel.stopOnlineHeartbeat()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startOnlineHeartbeat()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopOnlineHeartbeat()
        }
    }

    val profileState by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val roomsState by viewModel.liveRooms.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val privateAccount by viewModel.privateAccount.collectAsStateWithLifecycle()
    val followingList by viewModel.followingList.collectAsStateWithLifecycle()
    val followersList by viewModel.followersList.collectAsStateWithLifecycle()
    val pinnedConversationIds by viewModel.pinnedConversationIds.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val giftHistory by viewModel.giftHistory.collectAsStateWithLifecycle()
    val giftRankings by viewModel.giftRankings.collectAsStateWithLifecycle()
    val activeGifts by viewModel.activeGiftsCatalog.collectAsStateWithLifecycle()
    val latestGlobalGiftAnnouncement by viewModel.latestGlobalGiftAnnouncement.collectAsStateWithLifecycle()
    val recentRoomIds by viewModel.recentRoomIds.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val roomEffectsEnabled by viewModel.roomEffectsEnabled.collectAsStateWithLifecycle()
    val roomCreationLock by viewModel.roomCreationLock.collectAsStateWithLifecycle()
    val hasCreatedRoom by viewModel.hasCreatedRoom.collectAsStateWithLifecycle()
    val isCreatingRoom by viewModel.isCreatingRoom.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val signOutAction = {
        AuthHelper.signOut(
            credentialManager = credentialManager,
            scope = scope,
            onBeforeSignOut = { viewModel.setOfflineBeforeSignOut() }
        )
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading your VibePlay stage...", style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = signOutAction,
                        modifier = Modifier.testTag("loading_sign_out_button")
                    ) {
                        Text("Switch Account / Sign Out")
                    }
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Unable to Load Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.retryProfileInit() },
                                colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                                modifier = Modifier.testTag("retry_profile_button")
                            ) {
                                Text("Retry")
                            }
                            Button(
                                onClick = signOutAction,
                                colors = ButtonDefaults.buttonColors(containerColor = VibeCoral),
                                modifier = Modifier.testTag("error_sign_out_button")
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
        }

        is UiState.Success -> {
            val currentUser = state.data
            if (currentUser.accountStatus == "BANNED" || currentUser.accountStatus == "SUSPENDED") {
                AccountRestrictedScreen(
                    status = currentUser.accountStatus,
                    onSignOut = signOutAction
                )
                return
            }

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            var selectedConversationId by remember { mutableStateOf<String?>(null) }
            var activeRoomId by remember { mutableStateOf<String?>(null) }
            var leaveActiveRoomForSwitch by remember { mutableStateOf<(() -> Unit)?>(null) }
            var inspectedUser by remember { mutableStateOf<UserProfile?>(null) }
            var showNotificationsSheet by remember { mutableStateOf(false) }
            var passwordPromptRoom by remember { mutableStateOf<VoiceRoom?>(null) }
            var enteredPin by remember { mutableStateOf("") }
            var pinError by remember { mutableStateOf<String?>(null) }

            val blockedIds = privateAccount?.blockedUserIds ?: emptyList()
            val liveRoomsList = (roomsState as? UiState.Success)?.data ?: emptyList()
            val myHostedRoom = liveRoomsList.firstOrNull { it.isOwner(currentUser.userId) }
            val unreadNotifications = notifications.count { !it.read }
            val unreadConversations = conversations.count { it.unreadBy.contains(currentUser.userId) }
            val inboxBadgeCount = unreadConversations + unreadNotifications
            val followingIds = remember(followingList) { followingList.map { it.peerId }.toSet() }

            val openRoomDirect: (VoiceRoom) -> Unit = { room ->
                val previousRoomId = activeRoomId
                if (!previousRoomId.isNullOrBlank() && previousRoomId != room.id) {
                    // A room kept in the back stack must not keep this user seated
                    // while they enter a different room from the floating mini-room.
                    leaveActiveRoomForSwitch?.invoke()
                    leaveActiveRoomForSwitch = null
                    navController.popBackStack("room/$previousRoomId", inclusive = true)
                }
                activeRoomId = room.id
                viewModel.recordRoomVisited(room.id)
                navController.navigate("room/${room.id}") {
                    launchSingleTop = true
                }
            }

            val tryOpenRoom: (VoiceRoom) -> Unit = { room ->
                if (room.hasPassword && !room.isOwner(currentUser.userId)) {
                    enteredPin = ""
                    pinError = null
                    passwordPromptRoom = room
                } else {
                    openRoomDirect(room)
                }
            }

            val mainTabs = remember {
                listOf(MainTab.Home, MainTab.Explore, MainTab.Rooms, MainTab.Inbox, MainTab.Profile)
            }
            val showTopLevelNav = (currentRoute ?: MainTab.Home.route) in mainTabs.map { it.route } || activeRoomId != null
            val isVoiceRoomScreen = currentRoute == ROUTE_VOICE_ROOM || currentRoute?.startsWith("room/") == true

            val switchTab: (MainTab) -> Unit = { tab ->
                if (tab != MainTab.Inbox) {
                    selectedConversationId = null
                }
                navController.navigate(tab.route) {
                    // Keep the active room destination on the back stack while
                    // the user browses other tabs. Its NavBackStackEntry-scoped
                    // RoomViewModel and realtime session therefore stay alive.
                    val roomId = activeRoomId
                    if (!roomId.isNullOrBlank()) {
                        popUpTo("room/$roomId") {
                            inclusive = false
                            saveState = true
                        }
                    } else {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            val blockedProfiles = remember(blockedIds, activeUsers) {
                blockedIds.map { blockedId ->
                    activeUsers.firstOrNull { it.userId == blockedId }
                        ?: UserProfile(userId = blockedId, username = "User ${blockedId.take(6)}")
                }
            }
            val inspectUserById: (String) -> Unit = { targetId ->
                if (targetId.isNotBlank()) {
                    if (targetId == currentUser.userId) {
                        inspectedUser = currentUser
                    } else {
                        val fallback = activeUsers.firstOrNull { it.userId == targetId }
                            ?: followingList.firstOrNull { it.peerId == targetId }?.let {
                                UserProfile(
                                    userId = it.peerId,
                                    username = it.peerName,
                                    avatarUrl = it.peerAvatarUrl,
                                    gender = it.peerGender
                                )
                            }
                            ?: followersList.firstOrNull { it.peerId == targetId }?.let {
                                UserProfile(
                                    userId = it.peerId,
                                    username = it.peerName,
                                    avatarUrl = it.peerAvatarUrl,
                                    gender = it.peerGender
                                )
                            }
                        viewModel.inspectUser(targetId, fallback) { loaded ->
                            inspectedUser = loaded
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = if (currentRoute == MainTab.Home.route || currentRoute == MainTab.Rooms.route) {
                        if (darkTheme) androidx.compose.ui.graphics.Color(0xFF08080A) else androidx.compose.ui.graphics.Color(0xFFFFFDF5)
                    } else MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.systemBars,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showTopLevelNav && !isVoiceRoomScreen && !useNavigationRail) {
                            NavigationBar {
                                mainTabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { switchTab(tab) },
                                        icon = {
                                            if (tab == MainTab.Inbox && inboxBadgeCount > 0) {
                                                BadgedBox(badge = { Badge { Text("$inboxBadgeCount") } }) {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = { Text(tab.label, maxLines = 1) },
                                        modifier = Modifier.testTag("nav_${tab.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        if (showTopLevelNav && !isVoiceRoomScreen && useNavigationRail) {
                            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                mainTabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { switchTab(tab) },
                                        icon = {
                                            if (tab == MainTab.Inbox && inboxBadgeCount > 0) {
                                                BadgedBox(badge = { Badge { Text("$inboxBadgeCount") } }) {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag("rail_${tab.route}")
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = MainTab.Home.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(MainTab.Home.route) {
                                    HomeScreen(
                                        currentUser = currentUser,
                                        roomsState = roomsState,
                                        activeUsers = activeUsers,
                                        recentRoomIds = recentRoomIds,
                                        unreadNotificationCount = unreadNotifications,
                                        blockedUserIds = blockedIds,
                                        followingIds = followingIds,
                                        onOpenRoom = tryOpenRoom,
                                        onCreateRoomClick = { switchTab(MainTab.Rooms) },
                                        onOpenNotifications = { showNotificationsSheet = true },
                                        onOpenGiftRanking = { navController.navigate("gift_rankings") },
                                        onClaimDailyReward = { viewModel.claimDailyReward(currentUser) },
                                        onOpenUserProfile = { target ->
                                            viewModel.inspectUser(target.userId, target) { inspectedUser = it }
                                        },
                                        onVerifyPasscode = { r, pin -> viewModel.verifyRoomPasscode(r, pin) },
                                        onOpenGame = { navController.navigate("fruit_treasure") },
                                        onOpenShop = { navController.navigate("shop") }
                                    )
                                }

                                composable("fruit_treasure") { FruitTreasureScreen(onBack = { navController.popBackStack() }) }

                                composable("gift_rankings") {
                                    GiftRankingScreen(
                                        users = giftRankings,
                                        onBack = { navController.popBackStack() },
                                        onOpenUserProfile = { target ->
                                            viewModel.inspectUser(target.userId, target) { inspectedUser = it }
                                        }
                                    )
                                }

                                composable(MainTab.Explore.route) {
                                    ExploreScreen(
                                        currentUser = currentUser,
                                        roomsState = roomsState,
                                        activeUsers = activeUsers,
                                        followingList = followingList,
                                        blockedUserIds = blockedIds,
                                        onOpenRoom = tryOpenRoom,
                                        onOpenUserProfile = { target ->
                                            viewModel.inspectUser(target.userId, target) { inspectedUser = it }
                                        },
                                        onToggleFollow = { target, currentlyFollowing ->
                                            viewModel.toggleFollow(currentUser, target, currentlyFollowing)
                                        }
                                    )
                                }

                                composable(MainTab.Rooms.route) {
                                    RoomsHubScreen(
                                        currentUser = currentUser,
                                        roomsState = roomsState,
                                        recentRoomIds = recentRoomIds,
                                        hasCreatedRoom = hasCreatedRoom || currentUser.hasCreatedRoom,
                                        createdRoomId = roomCreationLock?.roomId?.ifBlank { currentUser.createdRoomId }
                                            ?: currentUser.createdRoomId,
                                        isCreatingRoom = isCreatingRoom,
                                        onCreateRoom = { title, ann, cat, coverKey, locked, rawPwd ->
                                            viewModel.createVoiceRoom(
                                                host = currentUser,
                                                title = title,
                                                announcement = ann,
                                                category = cat,
                                                coverKey = coverKey,
                                                locked = locked,
                                                rawPassword = rawPwd,
                                                onCreated = { createdRoomId ->
                                                    activeRoomId = createdRoomId
                                                    navController.navigate("room/$createdRoomId") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                        },
                                        onOpenRoom = tryOpenRoom
                                    )
                                }

                                composable(MainTab.Inbox.route) {
                                    InboxScreen(
                                        currentUser = currentUser,
                                        conversations = conversations,
                                        activeUsers = activeUsers,
                                        followingList = followingList,
                                        notifications = notifications,
                                        blockedUserIds = blockedIds,
                                        pinnedConversationIds = pinnedConversationIds,
                                        selectedConversationId = selectedConversationId,
                                        onSelectConversation = { selectedConversationId = it },
                                        onOpenConversationWithUser = { peer ->
                                            viewModel.openConversation(currentUser, peer) { convId ->
                                                selectedConversationId = convId
                                            }
                                        },
                                        onCreateGroupChat = { groupName, groupAvatarUrl, members ->
                                            viewModel.createGroupChat(
                                                creator = currentUser,
                                                groupName = groupName,
                                                groupAvatarUrl = groupAvatarUrl,
                                                members = members
                                            ) { convId ->
                                                selectedConversationId = convId
                                            }
                                        },
                                        onAddGroupMembers = { conv, members ->
                                            viewModel.addGroupMembers(conv, currentUser, members)
                                        },
                                        onRemoveGroupMember = { conv, targetUid ->
                                            viewModel.removeGroupMember(conv, currentUser, targetUid)
                                        },
                                        onUpdateGroupDetails = { conv, name, avatar ->
                                            viewModel.updateGroupDetails(conv, name, avatar)
                                        },
                                        onLeaveGroup = { conv ->
                                            viewModel.leaveGroup(conv, currentUser) {
                                                selectedConversationId = null
                                            }
                                        },
                                        onTogglePinConversation = { convId, pin ->
                                            viewModel.togglePinConversation(convId, pin)
                                        },
                                        activeGifts = activeGifts,
                                        onSendGift = { conv, gift, quantity ->
                                            viewModel.sendInboxGift(conv, currentUser, gift, quantity)
                                        },
                                        observeMessages = { convId -> viewModel.observeDirectMessages(convId) },
                                        onSendMessage = { conv, text, imageUri ->
                                            viewModel.sendDirectMessage(conv, currentUser, text, imageUri)
                                        },
                                        onMarkConversationRead = { viewModel.markConversationRead(it) },
                                        onMarkNotificationRead = { viewModel.markNotificationRead(it) },
                                        onMarkAllNotificationsRead = { viewModel.markAllNotificationsRead(notifications) },
                                        onOpenRoomById = { roomId ->
                                            val target = liveRoomsList.firstOrNull { it.id == roomId }
                                            if (target != null) {
                                                tryOpenRoom(target)
                                            } else {
                                                viewModel.showStatus("That voice room has ended.")
                                            }
                                        },
                                        onOpenUserById = inspectUserById,
                                        onBlockUser = { targetId -> viewModel.toggleBlockUser(targetId, block = true) },
                                        onSubmitReport = { type, id, name, reason, details ->
                                            viewModel.submitReport(currentUser, type, id, name, reason, details)
                                        }
                                    )
                                }

                                composable(MainTab.Profile.route) {
                                    ProfileScreen(
                                        profile = currentUser,
                                        darkTheme = darkTheme,
                                        roomEffectsEnabled = roomEffectsEnabled,
                                        followingList = followingList,
                                        followersList = followersList,
                                        giftHistory = giftHistory,
                                        blockedUsers = blockedProfiles,
                                        onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                                        onToggleRoomEffects = { viewModel.setRoomEffects(it) },
                                        onEditProfile = { name, bio, avatar, cover, gender, age, showAge, showGender, showOnline, profileTags ->
                                            viewModel.updateProfile(name, bio, avatar, cover, gender, age, showAge, showGender, showOnline, profileTags)
                                        },
                                        onUpgradeVip = { tier -> viewModel.upgradeVip(currentUser, tier) },
                                        onClaimDailyReward = { viewModel.claimDailyReward(currentUser) },
                                        onInspectUserById = inspectUserById,
                                        onCopyVibeId = { vibeId -> viewModel.showStatus("Copied Vibe ID: $vibeId") },
                                        onUnblockUser = { targetId -> viewModel.toggleBlockUser(targetId, block = false) },
                                        onOpenNotifications = { showNotificationsSheet = true },
                                        onSignOut = signOutAction,
                                        onOpenShop = { navController.navigate("shop") },
                                        onOpenRelationships = { navController.navigate("relationships") },
                                        onOpenCharmLevels = { navController.navigate("charm-levels") }
                                    )
                                }

                                composable("charm-levels") {
                                    CharmLevelsScreen(
                                        profile = currentUser,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("shop") {
                                    ShopScreen(
                                        profile = currentUser,
                                        onBack = { navController.popBackStack() },
                                        onUpgradeVip = { tier -> viewModel.upgradeVip(currentUser, tier) },
                                        onPurchase = { category, itemId -> viewModel.purchaseShopCosmetic(category, itemId) },
                                        onEquip = { category, itemId -> viewModel.equipShopCosmetic(category, itemId) }
                                    )
                                }

                                composable("relationships") {
                                    RelationshipScreen(
                                        currentUserId = uid,
                                        followingList = followingList,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = ROUTE_VOICE_ROOM,
                                    arguments = listOf(navArgument("roomId") { type = NavType.StringType })
                                ) { entry ->
                                    val roomId = entry.arguments?.getString("roomId") ?: return@composable
                                    key(uid, roomId) {
                                        val roomVm: RoomViewModel = viewModel(
                                            key = "room_${uid}_$roomId",
                                            factory = viewModelFactory {
                                                initializer {
                                                    RoomViewModel(
                                                        roomId = roomId,
                                                        currentUserId = uid,
                                                        roomRepository = RoomRepository(appContext)
                                                    )
                                                }
                                            }
                                        )
                                        val followingProfiles = remember(activeUsers, followingList, blockedIds) {
                                            followingList
                                                .filter { it.peerId.isNotBlank() && it.peerId !in blockedIds }
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
                                        SideEffect {
                                            (context as? MainActivity)?.setVoiceRoomActive(true)
                                            leaveActiveRoomForSwitch = { roomVm.leaveRoom {} }
                                        }
                                        VoiceRoomScreen(
                                            roomViewModel = roomVm,
                                            onMinimizeToPip = {
                                                // Back/minimize inside VibePlay should reveal
                                                // the app tabs and keep this room on the back stack.
                                                switchTab(MainTab.Home)
                                            },
                                            currentUser = currentUser,
                                            activeGifts = activeGifts,
                                            blockedUserIds = blockedIds,
                                            followingProfiles = followingProfiles,
                                            followingIds = followingIds,
                                            roomEffectsEnabled = roomEffectsEnabled,
                                            onInviteUserToRoom = { targetId, targetRoom ->
                                                viewModel.inviteFriendToRoom(currentUser, targetId, targetRoom)
                                            },
                                            onOpenUserProfile = inspectUserById,
                                            onBack = {
                                                RoomMusicPlayback.release()
                                                (context as? MainActivity)?.setVoiceRoomActive(false)
                                                activeRoomId = null
                                                leaveActiveRoomForSwitch = null
                                                navController.popBackStack()
                                            },
                                            onSubmitReport = { type, id, name, reason, details ->
                                                viewModel.submitReport(currentUser, type, id, name, reason, details)
                                            },
                                            onBlockUser = { targetId -> viewModel.toggleBlockUser(targetId, block = true) },
                                            onUnblockUser = { targetId -> viewModel.toggleBlockUser(targetId, block = false) }
                                        )
                                    }
                                }
                            }
                            activeRoomId?.takeIf { roomId ->
                                currentRoute?.startsWith("room/") != true && currentRoute != "room/$roomId"
                            }?.let { roomId ->
                                Surface(
                                    onClick = {
                                        navController.popBackStack(route = "room/$roomId", inclusive = false)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 16.dp, end = 16.dp)
                                        .size(46.dp)
                                        .testTag("floating_return_to_room"),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = VibeViolet,
                                    contentColor = androidx.compose.ui.graphics.Color.White,
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = "Return to room",
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                GlobalGiftAnnouncementTicker(
                    announcement = latestGlobalGiftAnnouncement,
                    onJoinRoom = { roomId ->
                        liveRoomsList.firstOrNull { it.id == roomId }?.let { room ->
                            tryOpenRoom(room)
                        } ?: run {
                            activeRoomId = roomId
                                            navController.navigate("room/$roomId") {
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
                )
            }

            inspectedUser?.let { target ->
                val isFollowing = followingList.any { it.peerId == target.userId }
                PublicProfileSheet(
                    targetUser = target,
                    liveRooms = liveRoomsList,
                    onJoinUserRoom = { room ->
                        inspectedUser = null
                        viewModel.clearInspectedUser()
                        tryOpenRoom(room)
                    },
                    currentUserId = currentUser.userId,
                    isFollowing = isFollowing,
                    followersList = if (target.userId == currentUser.userId) followersList else emptyList(),
                    followingList = if (target.userId == currentUser.userId) followingList else emptyList(),
                    giftHistory = giftHistory,
                    myHostedRoom = myHostedRoom,
                    onDismiss = {
                        inspectedUser = null
                        viewModel.clearInspectedUser()
                    },
                    onToggleFollow = { viewModel.toggleFollow(currentUser, target, isFollowing) },
                    onStartChat = {
                        inspectedUser = null
                        viewModel.clearInspectedUser()
                        viewModel.openConversation(currentUser, target) { convId ->
                            selectedConversationId = convId
                            switchTab(MainTab.Inbox)
                        }
                    },
                    onInviteToRoom = { hosted ->
                        viewModel.inviteFriendToRoom(currentUser, target.userId, hosted)
                    },
                    onBlockUser = { viewModel.toggleBlockUser(target.userId, block = true) },
                    onInspectUserById = inspectUserById,
                    onSubmitReport = { reason, details ->
                        viewModel.submitReport(currentUser, "USER", target.userId, target.username, reason, details)
                    }
                )
            }

            if (showNotificationsSheet) {
                NotificationsCenterSheet(
                    notifications = notifications,
                    onMarkRead = { viewModel.markNotificationRead(it) },
                    onMarkAllRead = { viewModel.markAllNotificationsRead(notifications) },
                    onOpenRoomById = { roomId ->
                        showNotificationsSheet = false
                        val target = liveRoomsList.firstOrNull { it.id == roomId }
                        if (target != null) {
                            tryOpenRoom(target)
                        } else {
                            viewModel.showStatus("That voice room has ended.")
                        }
                    },
                    onOpenConversationById = { convId ->
                        showNotificationsSheet = false
                        selectedConversationId = convId
                        switchTab(MainTab.Inbox)
                    },
                    onOpenUserById = { targetId ->
                        showNotificationsSheet = false
                        inspectUserById(targetId)
                    },
                    onDismiss = { showNotificationsSheet = false }
                )
            }

            passwordPromptRoom?.let { targetRoom ->
                AlertDialog(
                    onDismissRequest = { passwordPromptRoom = null },
                    title = { Text("Enter Room Passcode") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "\"${targetRoom.title}\" is password-protected by ${targetRoom.hostName}.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = { enteredPin = it; pinError = null },
                                label = { Text("Room Passcode") },
                                singleLine = true,
                                isError = pinError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            pinError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (viewModel.verifyRoomPasscode(targetRoom, enteredPin)) {
                                passwordPromptRoom = null
                                openRoomDirect(targetRoom)
                            } else {
                                pinError = "Incorrect passcode. Try again."
                            }
                        }) {
                            Text("Join Room")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { passwordPromptRoom = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun GlobalGiftAnnouncementTicker(
    announcement: GlobalGiftAnnouncement?,
    onJoinRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayed by remember { mutableStateOf<GlobalGiftAnnouncement?>(null) }
    var lastId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(announcement?.id) {
        val item = announcement ?: return@LaunchedEffect
        val fresh = item.createdAt?.seconds?.let { it >= (System.currentTimeMillis() / 1000L) - 30L } == true
        if (lastId == null) {
            lastId = item.id
            if (fresh) {
                displayed = item
                delay(9_500L)
                if (displayed?.id == item.id) displayed = null
            }
        } else if (item.id != lastId) {
            lastId = item.id
            displayed = item
            delay(9_500L)
            if (displayed?.id == item.id) displayed = null
        }
    }
    BoxWithConstraints(modifier = modifier) {
        val item = displayed
        val x by animateDpAsState(if (item == null) maxWidth else -maxWidth, tween(9_000), label = "gift_ticker")
        if (item != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp).offset(x = x)
                    .fillMaxWidth(0.96f).height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, VibeGold.copy(alpha = 0.85f), RoundedCornerShape(10.dp)),
                color = androidx.compose.ui.graphics.Color(0xE91A1428),
                shape = RoundedCornerShape(10.dp), shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
                        VibeViolet.copy(alpha = 0.92f),
                        androidx.compose.ui.graphics.Color(0xEE241A38),
                        VibeGold.copy(alpha = 0.30f)
                    ))).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "★ ${item.senderName} sent ${item.giftName} to ${item.recipientName} • ${item.totalCoins} Coins • Go to see",
                            modifier = Modifier.weight(1f),
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            textAlign = TextAlign.Start
                        )
                        Button(
                            onClick = { onJoinRoom(item.roomId) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibeGold,
                                contentColor = androidx.compose.ui.graphics.Color(0xFF1A1428)
                            )
                        ) {
                            Text("Join", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRestrictedScreen(
    status: String,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, VibeCoral.copy(alpha = 0.5f)),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = VibeCoral,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = if (status == "BANNED") "Account Banned" else "Account Suspended",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your VibePlay account is currently $status due to community moderation guidelines. Please contact support or sign out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restricted_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
