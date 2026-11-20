package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.data.livekit.VoiceConnectionState
import com.example.data.model.Conversation
import com.example.data.model.FollowEdge
import com.example.data.model.ModerationReport
import com.example.data.model.RoomListener
import com.example.data.model.RoomMessage
import com.example.data.model.SpeakerRequest
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.example.data.repository.RoomRepository
import com.example.ui.screens.AdminPanelContent
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HostSettingsSheet
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.InviteSpeakerSheet
import com.example.ui.screens.PendingRequestStatusDialog
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PublicProfileSheet
import com.example.ui.screens.RoomGiftStoreSheet
import com.example.ui.screens.RoomsHubScreen
import com.example.ui.screens.SpeakerRequestsAndListenersSheet
import com.example.ui.screens.VoiceRoomScreen
import com.example.ui.screens.VoiceRoomStageContent
import com.example.ui.state.RoomViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.VibePlayTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
class UiRenderRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleUser = UserProfile(
        userId = "u1",
        vibeId = "VP-123456",
        username = "DJ Nova",
        gender = "MALE",
        age = 24L,
        showAge = true,
        showGender = true,
        coinBalance = 500L
    )

    private val sampleRooms = listOf(
        VoiceRoom(
            id = "r1",
            hostId = "u2",
            hostName = "Luna",
            ownerId = "u2",
            ownerName = "Luna",
            ownerOnline = true,
            title = "Midnight Lo-Fi Chill",
            category = "Chill",
            listenerCount = 14L,
            activeSpeakersCount = 3L
        )
    )

    @Before
    fun initFirebaseForRobolectric() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("gen-lang-client-0380050083")
                    .setApplicationId("1:79195404627:android:6069060282e1916049c9a6")
                    .setApiKey("AIzaSyB5DZp_jnhXG5A5rj56_eQFCstrSDtKP7I")
                    .build()
            )
        }
    }

    @Test
    fun mainActivity_launchesWithoutCrashing_andHasNonZeroDecorView() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val rootView = activity.window.decorView
        println("MainActivity decorView size=${rootView.width}x${rootView.height} visibility=${rootView.visibility}")
        assertTrue(rootView.width > 0 && rootView.height > 0)
    }

    @Test
    fun authScreen_rendersTitleAndSignInButton() {
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeRule.onNodeWithText("VibePlay").assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        val bounds = composeRule.onNodeWithTag("google_sign_in_button").fetchSemanticsNode().boundsInRoot
        println("AuthScreen sign-in button bounds=$bounds")
        assertTrue(bounds.width > 0f && bounds.height > 0f)
    }

    @Test
    fun homeScreen_insideMainScaffoldNavHost_rendersHeaderBannerAndRooms() {
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                val navController = rememberNavController()
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.systemBars,
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = true,
                                    onClick = {},
                                    icon = {},
                                    label = { Text("Home") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = "home",
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable("home") {
                                        HomeScreen(
                                            currentUser = sampleUser,
                                            roomsState = UiState.Success(sampleRooms),
                                            activeUsers = listOf(sampleUser),
                                            recentRoomIds = emptyList(),
                                            unreadNotificationCount = 1,
                                            blockedUserIds = emptyList(),
                                            followingIds = emptySet(),
                                            onOpenRoom = {},
                                            onCreateRoomClick = {},
                                            onOpenNotifications = {},
                                            onClaimDailyReward = {},
                                            onOpenUserProfile = {},
                                            onVerifyPasscode = { _, _ -> true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("VibePlay").assertIsDisplayed()
        composeRule.onNodeWithTag("home_hero_create_room").assertIsDisplayed()
        composeRule.onNodeWithText("Popular Voice Rooms").assertIsDisplayed()
        composeRule.onAllNodesWithTag("room_card_r1")[0].assertIsDisplayed()
    }

    @Test
    fun allSecondaryTabs_renderWithoutCrashing() {
        val tabState = androidx.compose.runtime.mutableIntStateOf(0)
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                when (tabState.intValue) {
                    0 -> ExploreScreen(
                        currentUser = sampleUser,
                        roomsState = UiState.Success(sampleRooms),
                        activeUsers = listOf(sampleUser),
                        followingList = emptyList(),
                        blockedUserIds = emptyList(),
                        onOpenRoom = {},
                        onOpenUserProfile = {},
                        onToggleFollow = { _, _ -> }
                    )
                    1 -> RoomsHubScreen(
                        currentUser = sampleUser,
                        roomsState = UiState.Success(sampleRooms),
                        onCreateRoom = { _, _, _, _, _, _ -> },
                        onOpenRoom = {}
                    )
                    2 -> InboxScreen(
                        currentUser = sampleUser,
                        conversations = emptyList(),
                        activeUsers = listOf(sampleUser),
                        notifications = emptyList(),
                        blockedUserIds = emptyList(),
                        selectedConversationId = null,
                        onSelectConversation = {},
                        onOpenConversationWithUser = {},
                        observeMessages = { emptyFlow() },
                        onSendMessage = { _, _, _ -> },
                        onMarkConversationRead = {},
                        onMarkNotificationRead = {},
                        onOpenRoomById = {},
                        onBlockUser = {},
                        onSubmitReport = { _, _, _, _, _ -> }
                    )
                    else -> ProfileScreen(
                        profile = sampleUser,
                        isAdmin = false,
                        darkTheme = true,
                        followingList = emptyList(),
                        followersList = emptyList(),
                        giftHistory = emptyList(),
                        blockedUsers = emptyList(),
                        onToggleDarkTheme = {},
                        onEditProfile = { _, _, _, _, _, _, _, _, _ -> },
                        onUpgradeVip = {},
                        onClaimDailyReward = {},
                        onUnblockUser = {},
                        onOpenNotifications = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeRule.onNodeWithText("Explore VibePlay").assertIsDisplayed()

        tabState.intValue = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Launch Voice Room").assertIsDisplayed()

        tabState.intValue = 2
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Inbox").assertIsDisplayed()

        tabState.intValue = 3
        composeRule.waitForIdle()
        composeRule.onNodeWithText("DJ Nova").assertIsDisplayed()
    }

    @Test
    fun voiceRoomScreen_rendersWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = RoomViewModel(
            roomId = "r1",
            currentUserId = "u1",
            roomRepository = RoomRepository(context)
        )
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                VoiceRoomScreen(
                    roomViewModel = vm,
                    currentUser = sampleUser,
                    activeGifts = VibeCatalogDefaults.defaultGifts,
                    blockedUserIds = emptyList(),
                    followingProfiles = emptyList(),
                    onInviteUserToRoom = { _, _ -> },
                    onBack = {},
                    onSubmitReport = { _, _, _, _, _ -> },
                    onBlockUser = {}
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun voiceRoomStageContent_listenerView_renders9Seats_honestLiveKit_andBottomControls() {
        val room = sampleRooms.first()
        val stage = VoiceStage.buildStrictNineSeatStage(room, emptyList())
        val duplicateBlankMessages = listOf(
            RoomMessage(id = "", senderId = "u2", senderName = "Luna", senderGender = "FEMALE", text = "Welcome everyone!"),
            RoomMessage(id = "", senderId = "u2", senderName = "Luna", senderGender = "FEMALE", text = "Welcome everyone!"),
            RoomMessage(id = "m3", senderId = "u1", senderName = "DJ Nova", senderGender = "MALE", text = "Great vibe!")
        )
        var requestedSeat = -1
        var openedProfileId = ""

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                VoiceRoomStageContent(
                    currentRoom = room,
                    currentStage = stage,
                    currentUser = sampleUser,
                    messages = duplicateBlankMessages,
                    listeners = listOf(RoomListener(userId = "u1", username = "DJ Nova", gender = "MALE")),
                    speakerRequests = emptyList(),
                    myPendingRequest = null,
                    voiceConnection = VoiceConnectionState.Unconfigured(hasServerUrl = false, hasTokenEndpoint = false),
                    giftBurst = null,
                    roomError = null,
                    onClearError = {},
                    activeGifts = VibeCatalogDefaults.defaultGifts,
                    blockedUserIds = emptyList(),
                    followingProfiles = emptyList(),
                    onOpenUserProfile = { openedProfileId = it },
                    onBack = {},
                    onLeaveRoom = {},
                    onSendRoomMessage = {},
                    onToggleSeatMute = {},
                    onRequestToSpeak = { preferredSeat -> requestedSeat = preferredSeat },
                    onCancelSpeakerRequest = {},
                    onClearGiftBurst = {},
                    onSendGift = { _, _, _, _ -> },
                    onUpdateRoomSettings = { _, _, _, _, _ -> },
                    onTransferOwnership = {},
                    onEndRoom = {},
                    onHostAssignUserToSeat = { _, _, _, _, _, _ -> },
                    onResolveSpeakerRequest = { _, _ -> },
                    onVacateSpeakerSeat = {},
                    onHostRemoveSpeaker = {},
                    onSubmitReport = { _, _, _, _, _ -> },
                    onBlockUser = {}
                )
            }
        }

        composeRule.onNodeWithTag("livekit_status_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Voice service not configured yet").assertIsDisplayed()
        composeRule.onNodeWithTag("host_seat_0").assertIsDisplayed()
        for (seatIdx in 1..8) {
            composeRule.onNodeWithTag("speaker_seat_$seatIdx").assertIsDisplayed()
        }

        // Verify all 4 bottom bar controls are visible and do not overlap horizontally
        val inputBounds = composeRule.onNodeWithTag("room_chat_input").fetchSemanticsNode().boundsInRoot
        val micBounds = composeRule.onNodeWithTag("room_mic_toggle_button").fetchSemanticsNode().boundsInRoot
        val handBounds = composeRule.onNodeWithTag("request_to_speak_button").fetchSemanticsNode().boundsInRoot
        val giftBounds = composeRule.onNodeWithTag("room_open_gifts_button").fetchSemanticsNode().boundsInRoot

        assertTrue(inputBounds.width > 0f && inputBounds.height > 0f)
        assertTrue(micBounds.width >= 40f && handBounds.width >= 40f && giftBounds.width >= 40f)
        assertTrue(inputBounds.right <= micBounds.left + 1f)
        assertTrue(micBounds.right <= handBounds.left + 1f)
        assertTrue(handBounds.right <= giftBounds.left + 1f)

        // Gender icons MUST NOT appear inside room chat or room seats
        assertTrue(composeRule.onAllNodesWithTag("gender_badge_male", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("gender_badge_female", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

        // Typing in room chat displays logged-in user's DP and name near the input & opens own profile on tap
        composeRule.onNodeWithTag("room_chat_input").performTextInput("Hello stage")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("room_chat_input_user_identity").assertIsDisplayed()
        composeRule.onNodeWithTag("room_chat_input_user_identity").performClick()
        assertEquals("u1", openedProfileId)

        // All 3 action buttons remain visible above the input bar while typing
        composeRule.onNodeWithTag("room_mic_toggle_button").assertIsDisplayed()
        composeRule.onNodeWithTag("request_to_speak_button").assertIsDisplayed()
        composeRule.onNodeWithTag("room_open_gifts_button").assertIsDisplayed()

        // Clicking empty speaker seat 3 as listener sends request for seat 3
        composeRule.onNodeWithTag("speaker_seat_3").performClick()
        assertEquals(3, requestedSeat)

        // Clicking own chat message opens own profile ID u1
        openedProfileId = ""
        composeRule.onNodeWithTag("room_chat_msg_m3").performClick()
        assertEquals("u1", openedProfileId)
    }

    @Test
    fun voiceRoomStageContent_ownerExitedWithActiveAdmin_hidesHostAvatarShowsOffline_andDeduplicatesOnlineCount() {
        val ownerOfflineRoom = VoiceRoom(
            id = "r_admin",
            hostId = "u2",
            hostName = "Luna",
            hostAvatarUrl = "https://example.com/luna.jpg",
            ownerId = "u2",
            ownerName = "Luna",
            ownerOnline = false,
            adminIds = listOf("u1"),
            activeAdminIds = listOf("u1"),
            title = "Late Night Studio",
            category = "Music",
            status = "LIVE"
        )
        val speakerSeat = VoiceSeat(
            seatIndex = 1L,
            role = "SPEAKER",
            occupantId = "u1",
            occupantName = "DJ Nova",
            occupantGender = "MALE",
            muted = false
        )
        val stage = VoiceStage.buildStrictNineSeatStage(ownerOfflineRoom, listOf(speakerSeat))
        // Intentional duplicate listener entry for u1 plus u3 to verify deduplication
        val listeners = listOf(
            RoomListener(userId = "u1", username = "DJ Nova", gender = "MALE"),
            RoomListener(userId = "u3", username = "Kai", gender = "FEMALE")
        )
        val onlineMembers = VoiceStage.buildOnlineMembersList(ownerOfflineRoom, stage, listeners)
        assertEquals(2, onlineMembers.size)
        assertEquals("", stage.hostSeat.occupantAvatarUrl)

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                VoiceRoomStageContent(
                    currentRoom = ownerOfflineRoom,
                    currentStage = stage,
                    currentUser = sampleUser,
                    messages = emptyList(),
                    listeners = listeners,
                    onlineUsers = onlineMembers,
                    speakerRequests = emptyList(),
                    myPendingRequest = null,
                    voiceConnection = VoiceConnectionState.Unconfigured(hasServerUrl = false, hasTokenEndpoint = false),
                    giftBurst = null,
                    roomError = null,
                    onClearError = {},
                    activeGifts = VibeCatalogDefaults.defaultGifts,
                    blockedUserIds = emptyList(),
                    followingProfiles = emptyList(),
                    onBack = {},
                    onLeaveRoom = {},
                    onSendRoomMessage = {},
                    onToggleSeatMute = {},
                    onRequestToSpeak = {},
                    onCancelSpeakerRequest = {},
                    onClearGiftBurst = {},
                    onSendGift = { _, _, _, _ -> },
                    onUpdateRoomSettings = { _, _, _, _, _ -> },
                    onTransferOwnership = {},
                    onEndRoom = {},
                    onHostAssignUserToSeat = { _, _, _, _, _, _ -> },
                    onResolveSpeakerRequest = { _, _ -> },
                    onVacateSpeakerSeat = {},
                    onHostRemoveSpeaker = {},
                    onSubmitReport = { _, _, _, _, _ -> },
                    onBlockUser = {}
                )
            }
        }

        // Host seat preserves name, shows OFFLINE badge, hides avatar
        composeRule.onNodeWithText("HOST · Luna").assertIsDisplayed()
        composeRule.onNodeWithTag("host_seat_offline_placeholder", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("host_offline_badge", useUnmergedTree = true).assertIsDisplayed()
        // Deduplicated online count is 2
        composeRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun voiceRoomStageContent_hostView_clickingEmptySeatOpensInviteSheet_andApprovesRequest() {
        val hostUser = UserProfile(userId = "u2", vibeId = "VP-999999", username = "Luna", gender = "FEMALE")
        val room = sampleRooms.first()
        val stage = VoiceStage.buildStrictNineSeatStage(room, emptyList())
        val pendingReq = SpeakerRequest(
            id = "u1",
            userId = "u1",
            hostId = "u2",
            username = "DJ Nova",
            preferredSeatIndex = 2L
        )
        var approvedReq: SpeakerRequest? = null
        var assignedSeat = -1

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                VoiceRoomStageContent(
                    currentRoom = room,
                    currentStage = stage,
                    currentUser = hostUser,
                    messages = emptyList(),
                    listeners = listOf(RoomListener(userId = "u3", username = "Kai", gender = "FEMALE")),
                    speakerRequests = listOf(pendingReq),
                    myPendingRequest = null,
                    voiceConnection = VoiceConnectionState.Unconfigured(hasServerUrl = false, hasTokenEndpoint = false),
                    giftBurst = null,
                    roomError = null,
                    onClearError = {},
                    activeGifts = VibeCatalogDefaults.defaultGifts,
                    blockedUserIds = emptyList(),
                    followingProfiles = listOf(UserProfile(userId = "u4", username = "Aria", gender = "FEMALE", online = true)),
                    onBack = {},
                    onLeaveRoom = {},
                    onSendRoomMessage = {},
                    onToggleSeatMute = {},
                    onRequestToSpeak = {},
                    onCancelSpeakerRequest = {},
                    onClearGiftBurst = {},
                    onSendGift = { _, _, _, _ -> },
                    onUpdateRoomSettings = { _, _, _, _, _ -> },
                    onTransferOwnership = {},
                    onEndRoom = {},
                    onHostAssignUserToSeat = { seatIndex, _, _, _, _, _ -> assignedSeat = seatIndex },
                    onResolveSpeakerRequest = { req, approve -> if (approve) approvedReq = req },
                    onVacateSpeakerSeat = {},
                    onHostRemoveSpeaker = {},
                    onSubmitReport = { _, _, _, _, _ -> },
                    onBlockUser = {}
                )
            }
        }

        composeRule.onNodeWithTag("speaker_seat_2").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("invite_speaker_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Invite Speaker · Seat 2").assertIsDisplayed()
        composeRule.onNodeWithTag("invite_sheet_approve_u1").assertIsDisplayed()
        composeRule.onNodeWithTag("invite_sheet_reject_u1").assertIsDisplayed()
        composeRule.onNodeWithTag("invite_speaker_sheet").performScrollToNode(hasTestTag("invite_sheet_assign_listener_u3"))
        composeRule.onNodeWithTag("invite_sheet_assign_listener_u3").assertIsDisplayed()

        composeRule.onNodeWithTag("invite_speaker_sheet").performScrollToNode(hasTestTag("invite_sheet_approve_u1"))
        composeRule.onNodeWithTag("invite_sheet_approve_u1").performClick()
        assertEquals("u1", approvedReq?.userId)
        assertEquals(-1, assignedSeat)
    }

    @Test
    fun pendingRequestStatusDialog_rendersAndCancelsRequest() {
        var cancelled = false
        val req = SpeakerRequest(id = "u1", userId = "u1", username = "DJ Nova", preferredSeatIndex = 4L)
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                PendingRequestStatusDialog(
                    request = req,
                    onCancelRequest = { cancelled = true },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Speak Request Pending").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_pending_request_button").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun profileScreen_displaysExactAge_genderBadge_andClickableFollowersFollowing_withoutAddFriend() {
        val femaleUser = UserProfile(
            userId = "u_female",
            vibeId = "VP-212121",
            username = "Aria",
            gender = "FEMALE",
            age = 21L,
            showAge = true,
            showGender = true,
            followersCount = 1L,
            followingCount = 1L
        )
        val followerEdge = FollowEdge(peerId = "u_f1", peerName = "Milo", peerGender = "MALE")
        val followingEdge = FollowEdge(peerId = "u_f2", peerName = "Zoe", peerGender = "FEMALE")
        var inspectedId = ""

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                ProfileScreen(
                    profile = femaleUser,
                    isAdmin = false,
                    darkTheme = true,
                    followingList = listOf(followingEdge),
                    followersList = listOf(followerEdge),
                    giftHistory = emptyList(),
                    blockedUsers = emptyList(),
                    onToggleDarkTheme = {},
                    onEditProfile = { _, _, _, _, _, _, _, _, _ -> },
                    onUpgradeVip = {},
                    onClaimDailyReward = {},
                    onInspectUserById = { inspectedId = it },
                    onUnblockUser = {},
                    onOpenNotifications = {},
                    onOpenAdminPanel = {},
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithText("ID: VP-212121 · 21 years").assertIsDisplayed()
        composeRule.onNodeWithTag("gender_badge_female").assertIsDisplayed()
        composeRule.onNodeWithText("♀️ Female").assertIsDisplayed()
        assertTrue(composeRule.onAllNodes(androidx.compose.ui.test.hasText("Add Friend", substring = true)).fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("profile_followers_stat").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("follow_edge_row_u_f1").performClick()
        assertEquals("u_f1", inspectedId)
    }

    @Test
    fun exploreScreen_displaysFollowingToggleWithoutGenderIcons_andWithoutFriendRequests() {
        val followedUser = UserProfile(userId = "u_followed", vibeId = "VP-200001", username = "Aria", gender = "FEMALE", age = 22L, showAge = true)
        val openUser = UserProfile(userId = "u_open", vibeId = "VP-200003", username = "Leo", gender = "MALE", age = 25L, showAge = true)
        var toggledId = ""

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                ExploreScreen(
                    currentUser = sampleUser,
                    roomsState = UiState.Success(sampleRooms),
                    activeUsers = listOf(sampleUser, followedUser, openUser),
                    followingList = listOf(FollowEdge(peerId = "u_followed", peerName = "Aria", peerGender = "FEMALE")),
                    blockedUserIds = emptyList(),
                    onOpenRoom = {},
                    onOpenUserProfile = {},
                    onToggleFollow = { target, _ -> toggledId = target.userId }
                )
            }
        }

        composeRule.onNodeWithText("Following").assertIsDisplayed()
        composeRule.onNodeWithText("Follow").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("gender_badge_female", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("gender_badge_male", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodes(androidx.compose.ui.test.hasText("Add Friend", substring = true)).fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText("Follow").performClick()
        assertEquals("u_open", toggledId)
    }

    @Test
    fun inboxScreen_pinsSortedFirst_andOpensGroupCreationSheet() {
        val olderPinned = Conversation(
            id = "c_pinned",
            conversationType = "GROUP",
            groupName = "Pinned VIP Lounge",
            participantIds = listOf("u1", "u2"),
            lastMessage = "Welcome",
            updatedAt = Timestamp(Date(1000L))
        )
        val newerUnpinned = Conversation(
            id = "c_regular",
            conversationType = "DIRECT",
            participantIds = listOf("u1", "u3"),
            participantNames = mapOf("u3" to "Kai"),
            lastMessage = "Latest message",
            updatedAt = Timestamp(Date(5000L))
        )
        var pinToggledConv = ""
        var pinToggledVal: Boolean? = null

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                InboxScreen(
                    currentUser = sampleUser,
                    conversations = listOf(newerUnpinned, olderPinned),
                    activeUsers = listOf(sampleUser, UserProfile(userId = "u3", username = "Kai", gender = "FEMALE")),
                    followingList = listOf(FollowEdge(peerId = "u3", peerName = "Kai", peerGender = "FEMALE")),
                    notifications = emptyList(),
                    blockedUserIds = emptyList(),
                    pinnedConversationIds = listOf("c_pinned"),
                    selectedConversationId = null,
                    onSelectConversation = {},
                    onOpenConversationWithUser = {},
                    onTogglePinConversation = { id, pin ->
                        pinToggledConv = id
                        pinToggledVal = pin
                    },
                    observeMessages = { emptyFlow() },
                    onSendMessage = { _, _, _ -> },
                    onMarkConversationRead = {},
                    onMarkNotificationRead = {},
                    onOpenRoomById = {},
                    onBlockUser = {},
                    onSubmitReport = { _, _, _, _, _ -> }
                )
            }
        }

        val pinnedTop = composeRule.onNodeWithTag("conversation_c_pinned").fetchSemanticsNode().boundsInRoot.top
        val unpinnedTop = composeRule.onNodeWithTag("conversation_c_regular").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Pinned conversation must sort above newer unpinned chat", pinnedTop < unpinnedTop)

        composeRule.onNodeWithTag("pin_conversation_c_regular").performClick()
        assertEquals("c_regular", pinToggledConv)
        assertEquals(true, pinToggledVal)

        composeRule.onNodeWithTag("new_group_chat_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_group_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("create_group_member_u3").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w840dp-h1000dp-420dpi")
    fun tabletLayout_rendersNavigationRailAndHomeScreen() {
        composeRule.setContent {
            VibePlayTheme(darkTheme = false) {
                val navController = rememberNavController()
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= androidx.compose.ui.unit.Dp(600f)
                    Scaffold(contentWindowInsets = WindowInsets.systemBars) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                    NavigationRailItem(
                                        selected = true,
                                        onClick = {},
                                        icon = {},
                                        label = { Text("Home") }
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                NavHost(navController = navController, startDestination = "home") {
                                    composable("home") {
                                        HomeScreen(
                                            currentUser = sampleUser,
                                            roomsState = UiState.Success(sampleRooms),
                                            activeUsers = listOf(sampleUser),
                                            recentRoomIds = emptyList(),
                                            unreadNotificationCount = 0,
                                            blockedUserIds = emptyList(),
                                            followingIds = emptySet(),
                                            onOpenRoom = {},
                                            onCreateRoomClick = {},
                                            onOpenNotifications = {},
                                            onClaimDailyReward = {},
                                            onOpenUserProfile = {},
                                            onVerifyPasscode = { _, _ -> true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("VibePlay").assertIsDisplayed()
        composeRule.onAllNodesWithTag("room_card_r1")[0].assertIsDisplayed()
    }

    @Test
    fun roomGiftStoreSheet_filtersCategory_andSendsComboQuantity() {
        val wealthyUser = sampleUser.copy(coinBalance = 5000L)
        val room = sampleRooms.first()
        val stage = VoiceStage.buildStrictNineSeatStage(room, emptyList())
        var sentQty = 0L
        var sentGiftId = ""

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                RoomGiftStoreSheet(
                    currentUser = wealthyUser,
                    stage = stage,
                    gifts = VibeCatalogDefaults.defaultGifts,
                    onDismiss = {},
                    onSendGift = { _, _, gift, qty ->
                        sentGiftId = gift.id
                        sentQty = qty
                    }
                )
            }
        }

        composeRule.onNodeWithTag("gift_cat_popular").assertIsDisplayed()
        composeRule.onNodeWithTag("gift_cat_luxury").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gift_item_gift_crown").performClick()
        composeRule.onNodeWithTag("gift_qty_5").performClick()
        composeRule.onNodeWithTag("confirm_send_gift_button").performClick()

        assertEquals("gift_crown", sentGiftId)
        assertEquals(5L, sentQty)
    }

    @Test
    fun adminPanelContent_displaysKpis_andTriggersSeedDefaults() {
        var seeded = false
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                AdminPanelContent(
                    users = listOf(sampleUser),
                    rooms = sampleRooms,
                    reports = listOf(ModerationReport(id = "rep1", reason = "Spam", status = "OPEN")),
                    gifts = VibeCatalogDefaults.defaultGifts,
                    onBack = {},
                    onUpdateStatus = { _, _ -> },
                    onGrantVipCoins = { _, _, _ -> },
                    onToggleFeatured = { _, _ -> },
                    onCloseRoom = {},
                    onResolveReport = { _, _ -> },
                    onSaveGift = { _, _, _, _, _, _ -> },
                    onSeedDefaultGifts = { seeded = true }
                )
            }
        }

        composeRule.onNodeWithText("Admin Moderation Console").assertIsDisplayed()
        composeRule.onNodeWithText("Open Reports").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_tab_gifts").performClick()
        composeRule.onNodeWithTag("admin_seed_gifts_button").performClick()
        assertTrue(seeded)
    }

    @Test
    fun hostSettingsSheet_muteAllSpeakers_triggersCallback() {
        val room = sampleRooms.first()
        val speakerSeat = VoiceSeat(
            seatIndex = 1L,
            role = "SPEAKER",
            occupantId = "u1",
            occupantName = "DJ Nova"
        )
        val stageWithSpeaker = VoiceStage.buildStrictNineSeatStage(room, listOf(speakerSeat))
        var mutedAll = false

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                HostSettingsSheet(
                    room = room,
                    stage = stageWithSpeaker,
                    onDismiss = {},
                    onSaveSettings = { _, _, _, _, _ -> },
                    onMuteAllSpeakers = { mutedAll = true },
                    onTransferOwnership = {},
                    onEndRoom = {}
                )
            }
        }
        composeRule.onNodeWithTag("host_mute_all_button").performClick()
        assertTrue(mutedAll)
    }

    @Test
    fun inviteSpeakerSheet_seatLockToggle_triggersCallback() {
        val room = sampleRooms.first()
        val stage = VoiceStage.buildStrictNineSeatStage(room, emptyList())
        var lockStateChanged: Boolean? = null

        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                InviteSpeakerSheet(
                    targetSeatIndex = 2,
                    room = room,
                    stage = stage,
                    requests = emptyList(),
                    listeners = emptyList(),
                    followingProfiles = emptyList(),
                    onToggleSeatLock = { lockStateChanged = it },
                    onAssignToSeat = { _, _, _, _, _ -> },
                    onInviteUserToRoom = {},
                    onApproveRequest = {},
                    onRejectRequest = {},
                    onDismiss = {}
                )
            }
        }
        composeRule.onNodeWithTag("invite_sheet_lock_seat_toggle").performClick()
        assertEquals(true, lockStateChanged)
    }

    @Test
    fun roomsHubScreen_showsPermanentLimitBannerAndDisablesLaunch_whenAlreadyCreated() {
        val myRoom = VoiceRoom(
            id = "r_mine",
            creatorId = "u1",
            hostId = "u1",
            hostName = "DJ Nova",
            ownerId = "u1",
            ownerName = "DJ Nova",
            title = "Nova Permanent Stage",
            category = "Music",
            status = "LIVE"
        )
        var openedRoomId = ""
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                RoomsHubScreen(
                    currentUser = sampleUser.copy(hasCreatedRoom = true, createdRoomId = "r_mine"),
                    roomsState = UiState.Success(listOf(myRoom)),
                    hasCreatedRoom = true,
                    createdRoomId = "r_mine",
                    isCreatingRoom = false,
                    onCreateRoom = { _, _, _, _, _, _ -> },
                    onOpenRoom = { openedRoomId = it.id }
                )
            }
        }

        composeRule.onNodeWithTag("one_room_limit_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_voice_room_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("open_my_created_room_button").performClick()
        assertEquals("r_mine", openedRoomId)
    }

    @Test
    fun genderBadge_notInRoomOnlineMembersSheet() {
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                SpeakerRequestsAndListenersSheet(
                    isHost = false,
                    requests = emptyList(),
                    listeners = listOf(RoomListener(userId = "u_m", username = "Marcus", gender = "MALE")),
                    onApprove = {},
                    onReject = {},
                    onReportListener = {},
                    onBlockListener = {},
                    onDismiss = {}
                )
            }
        }
        composeRule.onNodeWithText("Marcus").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("gender_badge_male", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun genderBadge_showsInsidePublicProfileSheet() {
        val targetMale = UserProfile(userId = "u_m", vibeId = "VP-333333", username = "Marcus", gender = "MALE", showGender = true)
        composeRule.setContent {
            VibePlayTheme(darkTheme = true) {
                PublicProfileSheet(
                    targetUser = targetMale,
                    currentUserId = "u1",
                    isFollowing = false,
                    myHostedRoom = null,
                    onDismiss = {},
                    onToggleFollow = {},
                    onStartChat = {},
                    onInviteToRoom = {},
                    onBlockUser = {},
                    onSubmitReport = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithTag("gender_badge_male", useUnmergedTree = true).assertIsDisplayed()
    }
}
