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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
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
    var selectedListCategory by remember { mutableStateOf(RoomCategory.ALL) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var roomIdToJoin by remember { mutableStateOf("") }
    var joinRoomError by remember { mutableStateOf<String?>(null) }
    var roomSearch by remember { mutableStateOf("") }
    var showRoomCreateForm by remember { mutableStateOf(false) }

    val liveRooms = (roomsState as? UiState.Success)?.data ?: emptyList()
    val filteredLiveRooms = remember(liveRooms, selectedListCategory, roomSearch) {
        liveRooms.filter { room ->
            (selectedListCategory == RoomCategory.ALL || room.category.equals(selectedListCategory.label, ignoreCase = true)) &&
                (roomSearch.isBlank() || room.title.contains(roomSearch, ignoreCase = true) ||
                    room.id.contains(roomSearch, ignoreCase = true) ||
                    room.hostName.contains(roomSearch, ignoreCase = true))
        }
    }
    val recentlyJoinedRooms = remember(liveRooms, recentRoomIds, selectedListCategory, roomSearch) {
        recentRoomIds.mapNotNull { roomId -> liveRooms.firstOrNull { it.id == roomId } }
            .filter { room ->
                selectedListCategory == RoomCategory.ALL ||
                    room.category.equals(selectedListCategory.label, ignoreCase = true)
            }
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
        modifier = Modifier.fillMaxSize().background(Color(0xFF090811)),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF3A3026),
                border = BorderStroke(1.dp, Color(0xFFFFC66D).copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color(0xFFFFC66D), modifier = Modifier.size(26.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Daily Coins Claimed!", color = Color(0xFFFFD58A), fontWeight = FontWeight.Bold)
                                Text("(Unlock next at 12:00 AM IST)", color = Color(0xFFE5D7C5), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Claimed", color = Color(0xFFFFD58A), fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Claimed", tint = Color(0xFFFFD36E), modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = roomSearch,
                onValueChange = { roomSearch = it },
                modifier = Modifier.fillMaxWidth().testTag("rooms_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text("Search room ID, user ID or room name...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search rooms", tint = VibePink) },
                trailingIcon = { Icon(Icons.Filled.Mic, contentDescription = "Voice search", tint = VibeCyan) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibePink,
                    unfocusedBorderColor = Color(0xFF55515F)
                )
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RoomCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedListCategory == cat,
                        onClick = { selectedListCategory = cat },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                when (cat) {
                                    RoomCategory.ALL -> Unit
                                    RoomCategory.CHILL -> Text("☾")
                                    RoomCategory.MUSIC -> Text("♫")
                                    RoomCategory.GAMING -> Text("🎮")
                                    RoomCategory.DATING -> Text("♥")
                                    RoomCategory.KARAOKE -> Text("♪")
                                    RoomCategory.CHAT -> Text("?")
                                }
                                Text(cat.label)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibeViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF211D30),
                            labelColor = Color(0xFFD8C9E8)
                        )
                    )
                }
            }
        }

        if (recentlyJoinedRooms.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recently Joined", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${recentlyJoinedRooms.size} rooms", style = MaterialTheme.typography.labelMedium, color = VibeCyan)
                }
            }
            items(recentlyJoinedRooms.chunked(2), key = { row -> "hub_joined_${row.first().id}" }) { roomRow ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    roomRow.forEach { room ->
                        VoiceRoomCard(room = room, onClick = { onOpenRoom(room) }, modifier = Modifier.weight(1f))
                    }
                    if (roomRow.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (!hasCreatedRoom && showRoomCreateForm) {
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
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Live Hot Rooms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Live now", style = MaterialTheme.typography.labelMedium, color = VibeCyan)
                    }
                }
            }
            items(filteredLiveRooms.chunked(2), key = { row -> "hub_row_${row.first().id}" }) { roomRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    roomRow.forEach { room ->
                        VoiceRoomCard(
                            room = room,
                            onClick = { onOpenRoom(room) },
                            modifier = Modifier.weight(1f),
                            isLiveHot = true
                        )
                    }
                    if (roomRow.size == 1) Spacer(modifier = Modifier.weight(1f))
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
