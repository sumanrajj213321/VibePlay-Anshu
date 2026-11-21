package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.data.repository.AdminRepository
import com.example.data.repository.InboxRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoomRepository
import com.example.data.repository.UserRepository
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.GiftRankingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.NotificationsCenterSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.PublicProfileSheet
import com.example.ui.screens.RoomsHubScreen
import com.example.ui.screens.VoiceRoomScreen
import com.example.ui.state.RoomViewModel
import com.example.ui.state.UiState
import com.example.ui.state.VibePlayViewModel
import com.example.ui.theme.VibeCoral
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
private const val ROUTE_ADMIN = "admin"

class MainActivity : ComponentActivity() {
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

            VibePlayTheme(darkTheme = darkTheme) {
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
                    adminRepository = AdminRepository(appContext),
                    preferencesRepository = PreferencesRepository(appContext)
                )
            }
        }
    )

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
            val showTopLevelNav = (currentRoute ?: MainTab.Home.route) in mainTabs.map { it.route }

            val switchTab: (MainTab) -> Unit = { tab ->
                if (tab != MainTab.Inbox) {
                    selectedConversationId = null
                }
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
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
                    contentWindowInsets = WindowInsets.systemBars,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showTopLevelNav && !useNavigationRail) {
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
                        if (showTopLevelNav && useNavigationRail) {
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
                                        onVerifyPasscode = { r, pin -> viewModel.verifyRoomPasscode(r, pin) }
                                    )
                                }

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
                                        isAdmin = viewModel.isAdmin(currentUser),
                                        darkTheme = darkTheme,
                                        roomEffectsEnabled = roomEffectsEnabled,
                                        followingList = followingList,
                                        followersList = followersList,
                                        giftHistory = giftHistory,
                                        blockedUsers = blockedProfiles,
                                        onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                                        onToggleRoomEffects = { viewModel.setRoomEffects(it) },
                                        onEditProfile = { name, bio, avatar, cover, gender, age, showAge, showGender, showOnline ->
                                            viewModel.updateProfile(name, bio, avatar, cover, gender, age, showAge, showGender, showOnline)
                                        },
                                        onUpgradeVip = { tier -> viewModel.upgradeVip(currentUser, tier) },
                                        onClaimDailyReward = { viewModel.claimDailyReward(currentUser) },
                                        onInspectUserById = inspectUserById,
                                        onCopyVibeId = { vibeId -> viewModel.showStatus("Copied Vibe ID: $vibeId") },
                                        onUnblockUser = { targetId -> viewModel.toggleBlockUser(targetId, block = false) },
                                        onOpenNotifications = { showNotificationsSheet = true },
                                        onOpenAdminPanel = {
                                            navController.navigate(ROUTE_ADMIN) { launchSingleTop = true }
                                        },
                                        onSignOut = signOutAction,
                                        onOpenShop = { navController.navigate("shop") }
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
                                        VoiceRoomScreen(
                                            roomViewModel = roomVm,
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
                                            onBack = { navController.popBackStack() },
                                            onSubmitReport = { type, id, name, reason, details ->
                                                viewModel.submitReport(currentUser, type, id, name, reason, details)
                                            },
                                            onBlockUser = { targetId -> viewModel.toggleBlockUser(targetId, block = true) }
                                        )
                                    }
                                }

                                composable(ROUTE_ADMIN) {
                                    if (viewModel.isAdmin(currentUser)) {
                                        AdminPanelScreen(
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { navController.popBackStack() }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            inspectedUser?.let { target ->
                val isFollowing = followingList.any { it.peerId == target.userId }
                val targetFollowers by remember(target.userId) {
                    viewModel.observeFollowersFor(target.userId)
                }.collectAsStateWithLifecycle(initialValue = emptyList())
                val targetFollowing by remember(target.userId) {
                    viewModel.observeFollowingFor(target.userId)
                }.collectAsStateWithLifecycle(initialValue = emptyList())

                PublicProfileSheet(
                    targetUser = target,
                    currentUserId = currentUser.userId,
                    isFollowing = isFollowing,
                    followersList = if (target.userId == currentUser.userId) followersList else targetFollowers,
                    followingList = if (target.userId == currentUser.userId) followingList else targetFollowing,
                    myHostedRoom = myHostedRoom,
                    onDismiss = { inspectedUser = null },
                    onToggleFollow = { viewModel.toggleFollow(currentUser, target, isFollowing) },
                    onStartChat = {
                        inspectedUser = null
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
