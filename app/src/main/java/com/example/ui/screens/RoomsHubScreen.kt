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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
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

    val liveRooms = (roomsState as? UiState.Success)?.data ?: emptyList()
    val filteredLiveRooms = remember(liveRooms, selectedListCategory) {
        if (selectedListCategory == RoomCategory.ALL) liveRooms
        else liveRooms.filter { it.category.equals(selectedListCategory.label, ignoreCase = true) }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (hasCreatedRoom) "My Room" else "Launch Voice Room",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasCreatedRoom) "Open your permanent room and continue where you left off." else "Every VibePlay room includes 1 Host seat + 8 Speaker seats (9 total).",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeCyan
                )
            }
        }

        if (hasCreatedRoom && myCreatedLiveRoom != null) {
            item {
                OutlinedButton(
                    onClick = { onOpenRoom(myCreatedLiveRoom) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_my_created_room_button")
                ) {
                    Text("My Room (${myCreatedLiveRoom.title})", fontWeight = FontWeight.Bold)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = VibeCyan)
                        Text(
                            text = "Active Voice Rooms (${filteredLiveRooms.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RoomCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedListCategory == cat,
                                onClick = { selectedListCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }
            items(filteredLiveRooms, key = { "hub_${it.id}" }) { room ->
                VoiceRoomCard(room = room, onClick = { onOpenRoom(room) })
            }
        }
    }
}
