package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.RoomCategory
import com.example.data.model.RoomCoverPreset
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.components.VoiceRoomCard
import com.example.ui.components.RoomCoverImage
import com.example.ui.components.coverDrawableRes
import com.example.ui.state.UiState
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet

@Composable
fun RoomsHubScreen(
    currentUser: UserProfile,
    roomsState: UiState<List<VoiceRoom>>,
    recentRoomIds: List<String> = emptyList(),
    hasCreatedRoom: Boolean = currentUser.hasCreatedRoom,
    createdRoomId: String = currentUser.createdRoomId,
    isCreatingRoom: Boolean = false,
    onCreateRoom: (
        title: String,
        announcement: String,
        category: String,
        coverKey: String,
        locked: Boolean,
        rawPassword: String
    ) -> Unit,
    onOpenRoom: (VoiceRoom) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var announcement by remember { mutableStateOf("Welcome! Respect everyone & have fun.") }
    var category by remember { mutableStateOf(RoomCategory.CHILL) }
    var coverPreset by remember { mutableStateOf(RoomCoverPreset.PARTY_STAGE) }
    var locked by remember { mutableStateOf(false) }
    var usePassword by remember { mutableStateOf(false) }
    var roomPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var selectedListCategory by remember { mutableStateOf("All") }
    var selectedTopTab by remember { mutableStateOf("Online") }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var roomIdToJoin by remember { mutableStateOf("") }
    var joinRoomError by remember { mutableStateOf<String?>(null) }

    val liveRooms = (roomsState as? UiState.Success)?.data ?: emptyList()
    val roomsForTopTab = remember(liveRooms, recentRoomIds, selectedTopTab) {
        when (selectedTopTab) {
            "Recents", "Joined" -> recentRoomIds.mapNotNull { id -> liveRooms.firstOrNull { it.id == id } }
            "Games" -> liveRooms.filter { room ->
                room.category.contains("game", ignoreCase = true) ||
                    room.tags.any { it.contains("game", ignoreCase = true) }
            }
            "Events" -> liveRooms.filter { room ->
                room.featured || room.tags.any { it.contains("event", ignoreCase = true) }
            }
            else -> liveRooms
        }
    }
    val filteredLiveRooms = remember(roomsForTopTab, selectedListCategory) {
        if (selectedListCategory == "All") roomsForTopTab
        else roomsForTopTab.filter { room ->
            when (selectedListCategory) {
                "Parties" -> room.category.contains("party", true) ||
                    room.title.contains("party", true) || room.tags.any { it.contains("party", true) }
                else -> room.category.contains(selectedListCategory, true) ||
                    room.title.contains(selectedListCategory, true) ||
                    room.tags.any { it.contains(selectedListCategory, true) }
            }
        }
    }
    val recentlyJoinedRooms = remember(liveRooms, recentRoomIds) {
        recentRoomIds.mapNotNull { roomId -> liveRooms.firstOrNull { it.id == roomId } }
    }
    val myCreatedLiveRoom = remember(liveRooms, createdRoomId, currentUser.userId) {
        liveRooms.firstOrNull {
            (createdRoomId.isNotBlank() && it.id == createdRoomId) ||
                it.creatorId == currentUser.userId ||
                it.ownerId == currentUser.userId ||
                it.hostId == currentUser.userId
        } ?: createdRoomId.takeIf { hasCreatedRoom && it.isNotBlank() }?.let { roomId ->
            // Keep the permanent-room entry available even if the public LIVE-room
            // feed has not loaded yet. Navigation opens the room by its document ID.
            VoiceRoom(
                id = roomId,
                creatorId = currentUser.userId,
                hostId = currentUser.userId,
                ownerId = currentUser.userId,
                hostName = currentUser.username,
                ownerName = currentUser.username
            )
        }
    }

    val roomsCanvasColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF08080A)
    } else {
        Color(0xFFFFFDF5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(roomsCanvasColor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rooms",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    modifier = Modifier.size(54.dp).clickable {
                        showJoinRoomDialog = true
                        roomIdToJoin = ""
                        joinRoomError = null
                    }.testTag("open_join_room_dialog"),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF292433)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Search, contentDescription = "Search rooms", tint = Color(0xFFF4F0FA), modifier = Modifier.size(27.dp))
                    }
                }
            }
        }

        item {
            val tabs = listOf("Recents", "Online", "Joined", "Games", "Events")
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        Column(
                            modifier = Modifier.weight(1f).clickable { selectedTopTab = tab }.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(
                                tab,
                                color = if (selectedTopTab == tab) Color(0xFFF5F0FA) else Color(0xFF9992A8),
                                fontWeight = if (selectedTopTab == tab) FontWeight.Bold else FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .width(if (selectedTopTab == tab) 38.dp else 12.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selectedTopTab == tab) Brush.horizontalGradient(listOf(Color(0xFFEC69C2), Color(0xFF9D70F4))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                            )
                        }
                    }
                }
            }
        }

        item {
            val chips = listOf("All", "Parties", "Carom", "Pool", "Ludo")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chips) { chip ->
                    val selected = selectedListCategory == chip
                    Surface(
                        modifier = Modifier.height(52.dp).clickable { selectedListCategory = chip },
                        shape = RoundedCornerShape(50),
                        color = if (selected) Color(0xFFDA72D2) else Color(0xFF211D2A),
                        border = if (selected) null else BorderStroke(1.dp, Color(0xFF373140))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 21.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chip,
                                color = if (selected) Color.White else Color(0xFFD0C9DC),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
                    when (selectedTopTab) {
                        "Recents" -> "Recently Visited"
                        "Joined" -> "Joined Rooms"
                        "Games" -> "Game Rooms"
                        "Events" -> "Events"
                        else -> "Online"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${filteredLiveRooms.size} rooms",
                    color = Color(0xFF00C5D9),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (hasCreatedRoom && myCreatedLiveRoom != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF26163F), Color(0xFF57366F), Color(0xFFB86E9D))
                            )
                        )
                        .clickable { onOpenRoom(myCreatedLiveRoom) }
                        .testTag("open_my_created_room_button")
                ) {
                    Text(
                        text = "❀",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        color = Color(0xFFFFC7E8).copy(alpha = 0.30f),
                        style = MaterialTheme.typography.displayLarge
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.66f)
                            .padding(start = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "YOUR ROOM",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.78f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            myCreatedLiveRoom.title.ifBlank { "${currentUser.username}'s Room" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (myCreatedLiveRoom.listenerCount + myCreatedLiveRoom.activeSpeakersCount == 0L)
                                "Nobody in right now"
                            else "${myCreatedLiveRoom.listenerCount + myCreatedLiveRoom.activeSpeakersCount} online",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1
                        )
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text("Enter", color = Color(0xFF7046E8), fontWeight = FontWeight.Bold)
                            Text("›", color = Color(0xFF7046E8), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        if (!hasCreatedRoom) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(60); validationError = null },
                        label = { Text("Room Title") },
                        placeholder = { Text("Late Night Lo-Fi & Chill") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_room_title_input")
                    )

                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it.take(200) },
                        label = { Text("Room Announcement") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_room_announcement_input")
                    )

                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RoomCategory.creatableCategories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VibeViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = "Stage Cover Art",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(RoomCoverPreset.entries) { preset ->
                            val selected = coverPreset == preset
                            Box(
                                modifier = Modifier
                                    .width(136.dp)
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) VibeCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { coverPreset = preset }
                            ) {
                                Image(
                                    painter = painterResource(id = coverDrawableRes(preset.key)),
                                    contentDescription = preset.label,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.62f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) VibeCyan else Color.White,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = VibePink, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Lock Speaker Seats", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Require host approval to speak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = locked, onCheckedChange = { locked = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Password, contentDescription = null, tint = VibeCyan, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Room Passcode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Optional PIN required to enter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = usePassword, onCheckedChange = { usePassword = it })
                    }

                    if (usePassword) {
                        OutlinedTextField(
                            value = roomPassword,
                            onValueChange = { roomPassword = it.take(16) },
                            label = { Text("Enter Room Passcode") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    validationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            if (hasCreatedRoom || isCreatingRoom) return@Button
                            if (title.trim().length < 2) {
                                validationError = "Please enter a room title (at least 2 characters)."
                            } else if (usePassword && roomPassword.trim().length < 3) {
                                validationError = "Passcode must be at least 3 characters."
                            } else {
                                onCreateRoom(
                                    title.trim(),
                                    announcement.trim(),
                                    category.label,
                                    coverPreset.key,
                                    locked,
                                    if (usePassword) roomPassword.trim() else ""
                                )
                            }
                        },
                        enabled = !hasCreatedRoom && !isCreatingRoom,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("launch_voice_room_button")
                    ) {
                        if (isCreatingRoom) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating Stage...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Launch 9-Seat Stage as ${currentUser.username}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        }

        if (liveRooms.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                }
            }
            items(filteredLiveRooms, key = { "hub_room_${it.id}" }) { room ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clickable { onOpenRoom(room) }
                        .testTag("room_list_item_${room.id}"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RoomCoverImage(
                            roomId = room.id,
                            coverUrl = room.coverUrl,
                            fallbackCoverKey = room.coverKey,
                            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                room.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFF20D889))
                                )
                                Text(
                                    "${room.listenerCount + room.activeSpeakersCount} online",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF20D889),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                room.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showJoinRoomDialog) {
        AlertDialog(
            onDismissRequest = { showJoinRoomDialog = false },
            title = { Text("Join Voice Room") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the room ID to join an active room.")
                    OutlinedTextField(
                        value = roomIdToJoin,
                        onValueChange = { value ->
                            roomIdToJoin = value.filter { it.isLetterOrDigit() || it == '-' }.take(40)
                            joinRoomError = null
                        },
                        label = { Text("Room ID") },
                        singleLine = true,
                        isError = joinRoomError != null,
                        modifier = Modifier.fillMaxWidth().testTag("join_room_id_input")
                    )
                    joinRoomError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = liveRooms.firstOrNull { it.id.equals(roomIdToJoin.trim(), ignoreCase = true) }
                        when {
                            roomIdToJoin.isBlank() -> joinRoomError = "Enter a room ID."
                            target == null -> joinRoomError = "Room not found or it is no longer live."
                            else -> { showJoinRoomDialog = false; onOpenRoom(target) }
                        }
                    },
                    modifier = Modifier.testTag("confirm_join_room")
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinRoomDialog = false }) { Text("Cancel") }
            }
        )
    }
}
