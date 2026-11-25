package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RoomCategory
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.data.model.VoiceRoom
import com.example.ui.components.VibeCoinIcon
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VoiceRoomCard
import com.example.ui.state.UiState
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    currentUser: UserProfile,
    roomsState: UiState<List<VoiceRoom>>,
    activeUsers: List<UserProfile>,
    recentRoomIds: List<String>,
    unreadNotificationCount: Int,
    blockedUserIds: List<String>,
    followingIds: Set<String> = emptySet(),
    onOpenRoom: (VoiceRoom) -> Unit,
    onCreateRoomClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenGiftRanking: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onOpenUserProfile: (UserProfile) -> Unit,
    onVerifyPasscode: (VoiceRoom, String) -> Boolean,
    onOpenGame: () -> Unit,
    onOpenShop: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RoomCategory.ALL) }
    var passwordPromptRoom by remember { mutableStateOf<VoiceRoom?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val istTimeZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    var todayStr by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istTimeZone }.format(Date())
        )
    }
    LaunchedEffect(istTimeZone) {
        while (true) {
            val nextMidnight = Calendar.getInstance(istTimeZone).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
            todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = istTimeZone }
                .format(Date())
        }
    }
    val hasClaimedDaily = currentUser.lastDailyClaimDay == todayStr
    val canClaimDaily = !hasClaimedDaily

    val onlineUsers = remember(activeUsers, currentUser.userId, blockedUserIds, followingIds) {
        activeUsers.filter {
            it.userId != currentUser.userId &&
                it.userId !in blockedUserIds &&
                it.online &&
                it.showOnlineStatus
        }.sortedByDescending { it.userId in followingIds }.take(15)
    }

    // The same home search supports room IDs and public Vibe IDs (VP-xxxxxx),
    // including users who are not currently online.
    val matchingUsers = remember(activeUsers, searchQuery, currentUser.userId, blockedUserIds) {
        val query = searchQuery.trim()
        if (query.isBlank()) emptyList() else {
            val normalizedId = query.removePrefix("ID-").removePrefix("id-")
            activeUsers.filter { user ->
                user.userId != currentUser.userId &&
                    user.userId !in blockedUserIds &&
                    (user.userId.equals(query, ignoreCase = true) ||
                     user.vibeId.equals(normalizedId, ignoreCase = true) ||
                     user.vibeId.substringAfterLast("-").equals(normalizedId, ignoreCase = true))
            }.take(8)
        }
    }

    val handleRoomTap: (VoiceRoom) -> Unit = { room ->
        if (room.hasPassword && !room.isOwner(currentUser.userId)) {
            enteredPin = ""
            pinError = null
            passwordPromptRoom = room
        } else {
            onOpenRoom(room)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VibePlay",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hey ${currentUser.username}, jump into a live stage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VibeGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            VibeCoinIcon(modifier = Modifier.size(24.dp))
                            Text(
                                text = "${currentUser.coinBalance}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = VibeGold
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text("$unreadNotificationCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                }
            }
        }

        // Equal-size Home shortcuts; Profile Shop remains unchanged.
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeShortcutCard("VibePlay Shop", "Discover items", VibePink, "SHOP", Modifier.weight(1f).height(112.dp), onOpenShop)
                HomeShortcutCard("Game Zone", "Fruit Treasure", VibeCyan, "PLAY", Modifier.weight(1f).height(112.dp), onOpenGame)
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onOpenGiftRanking)
                    .testTag("home_total_ranking"),
                shape = RoundedCornerShape(20.dp),
                color = VibeViolet.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, VibeViolet.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VibeCoinIcon(modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ranking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Top Gifters · Top Receivers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("View", color = VibeCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Hero Party Banner + Daily Login Reward
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(152.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onCreateRoomClick)
                    .testTag("home_hero_create_room")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_party_stage),
                    contentDescription = "Host a 9-seat voice room",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.82f),
                                    Color.Black.copy(alpha = 0.28f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VibeCyan.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, VibeCyan.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = VibeCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "9-SEAT VOICE STAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibeCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Host Your Party Room",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1 Host + 8 Speaker Seats · Gifts & Chat",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VibePink
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Go Live", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Daily Login Reward Card
        if (canClaimDaily) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = VibeViolet.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, VibeViolet.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = VibeGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Daily Login Reward Ready!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Claim +100,000 Coins & +50 XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onClaimDailyReward,
                            colors = ButtonDefaults.buttonColors(containerColor = VibeGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("claim_daily_reward_button")
                        ) {
                            Text("Claim", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = VibeGold.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = VibeGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Today's Daily Coins Already Claimed!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your next reward unlocks at 12:00 AM IST.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Search & Category Filters
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search room ID, user ID or room name...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("home_clear_search_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RoomCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibeViolet,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("home_cat_${cat.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Exact user-ID results from the shared search box.
        if (matchingUsers.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Matching Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(matchingUsers, key = { "search_user_${it.userId}" }) { user ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenUserProfile(user) }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VibeAvatar(
                                    avatarUrl = user.avatarUrl,
                                    username = user.username,
                                    size = 42.dp,
                                    vipLevel = if (user.isVipActive) user.vipLevel else 0L
                                )
                                Column {
                                    Text(user.username, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Text("ID-${user.vibeId}", style = MaterialTheme.typography.labelSmall, color = VibeCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Online Following & Active Creators Strip
        if (onlineUsers.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Following & Online",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${onlineUsers.size} active",
                            style = MaterialTheme.typography.labelMedium,
                            color = VibeEmerald
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(onlineUsers, key = { it.userId }) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable { onOpenUserProfile(user) }
                                    .testTag("online_user_${user.userId}")
                            ) {
                                VibeAvatar(
                                    avatarUrl = user.avatarUrl,
                                    username = user.username,
                                    size = 54.dp,
                                    vipLevel = if (user.isVipActive) user.vipLevel else 0L,
                                    online = user.online && user.showOnlineStatus,
                                    frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == user.activeFrameId && user.isFrameActive(it.id) }?.colorArgb
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rooms Content
        when (roomsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = roomsState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val filteredRooms = roomsState.data.filter { room ->
                    room.hostId !in blockedUserIds &&
                        (selectedCategory == RoomCategory.ALL || room.category.equals(selectedCategory.label, ignoreCase = true)) &&
                        (searchQuery.isBlank() ||
                            room.title.contains(searchQuery, ignoreCase = true) ||
                            room.hostName.contains(searchQuery, ignoreCase = true) ||
                            room.category.contains(searchQuery, ignoreCase = true) ||
                            room.id.equals(searchQuery.trim(), ignoreCase = true) ||
                            room.publicRoomId.equals(searchQuery.trim(), ignoreCase = true))
                }

                val recentlyJoined = filteredRooms.filter { it.id in recentRoomIds }
                val popularRooms = filteredRooms.sortedByDescending { it.listenerCount + it.activeSpeakersCount }
                val trendingRooms = filteredRooms.sortedByDescending { (if (it.featured) 50 else 0) + it.activeSpeakersCount }
                val recommendedRooms = filteredRooms.filter { it.hostId != currentUser.userId }.ifEmpty { filteredRooms }

                if (filteredRooms.isEmpty() && matchingUsers.isEmpty()) {
                    item {
                        EmptyRoomsPrompt(onCreateRoomClick = onCreateRoomClick)
                    }
                } else if (filteredRooms.isEmpty()) {
                    item {
                        Text(
                            "No live rooms match this search.",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    if (recentlyJoined.isNotEmpty()) {
                        item {
                            SectionTitle("Recently Joined", "${recentlyJoined.size} rooms")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recentlyJoined, key = { "recent_${it.id}" }) { room ->
                                    VoiceRoomCard(
                                        room = room,
                                        onClick = { handleRoomTap(room) },
                                        modifier = Modifier.width(248.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionTitle("Popular Voice Rooms", "Live now")
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularRooms, key = { "pop_${it.id}" }) { room ->
                                VoiceRoomCard(
                                    room = room,
                                    onClick = { handleRoomTap(room) },
                                    modifier = Modifier.width(260.dp)
                                )
                            }
                        }
                    }

                    item {
                        SectionTitle("Recommended & Trending", "${recommendedRooms.size} stages")
                    }
                    items(trendingRooms, key = { "trend_${it.id}" }) { room ->
                        VoiceRoomCard(
                            room = room,
                            onClick = { handleRoomTap(room) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
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
                    if (onVerifyPasscode(targetRoom, enteredPin)) {
                        passwordPromptRoom = null
                        onOpenRoom(targetRoom)
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

@Composable
private fun SectionTitle(title: String, badge: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = badge,
            style = MaterialTheme.typography.labelMedium,
            color = VibeCyan
        )
    }
}

@Composable
private fun EmptyRoomsPrompt(onCreateRoomClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VibeViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = VibeCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "No Live Voice Rooms Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Be the first to launch a 9-seat voice party stage and invite friends!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCreateRoomClick,
                colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                modifier = Modifier.testTag("empty_create_room_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create 9-Seat Room")
            }
        }
    }
}


@Composable
private fun HomeShortcutCard(title: String, subtitle: String, accent: Color, symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
 Surface(modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = accent.copy(alpha = 0.13f), border = BorderStroke(1.4.dp, accent.copy(alpha = 0.85f))) {
  Row(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha = 0.24f), Color.Transparent))).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
   Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.22f)) { Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) { Text(symbol, color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge) } }
   Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
  }
 }
}
