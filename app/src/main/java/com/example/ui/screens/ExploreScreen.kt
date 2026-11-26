package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FollowEdge
import com.example.data.model.RoomCategory
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.data.model.VoiceRoom
import com.example.ui.components.LevelBadge
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipMiniBadge
import com.example.ui.components.VipShiningNameText
import com.example.ui.components.VoiceRoomCard
import com.example.ui.state.UiState
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet

@Composable
fun ExploreScreen(
    currentUser: UserProfile,
    roomsState: UiState<List<VoiceRoom>>,
    activeUsers: List<UserProfile>,
    followingList: List<FollowEdge>,
    blockedUserIds: List<String>,
    onOpenRoom: (VoiceRoom) -> Unit,
    onOpenUserProfile: (UserProfile) -> Unit,
    onToggleFollow: (UserProfile, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RoomCategory.ALL) }

    val followingIds = remember(followingList) { followingList.map { it.peerId }.toSet() }
    val exploreCanvasColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF08080A)
    } else {
        MaterialTheme.colorScheme.background
    }

    val filteredUsers = remember(activeUsers, query, currentUser.userId, blockedUserIds) {
        activeUsers.filter { user ->
            user.userId != currentUser.userId &&
                user.userId !in blockedUserIds &&
                (query.isBlank() ||
                    user.username.contains(query, ignoreCase = true) ||
                    user.vibeId.contains(query, ignoreCase = true) ||
                    user.bio.contains(query, ignoreCase = true))
        }
    }

    val rooms = remember(roomsState, query, selectedCategory, blockedUserIds) {
        val list = (roomsState as? UiState.Success)?.data ?: emptyList()
        list.filter { room ->
            room.hostId !in blockedUserIds &&
                (selectedCategory == RoomCategory.ALL || room.category.equals(selectedCategory.label, ignoreCase = true)) &&
                (query.isBlank() ||
                    room.title.contains(query, ignoreCase = true) ||
                    room.hostName.contains(query, ignoreCase = true))
        }
    }

    val newRooms = remember(rooms) {
        rooms.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
    val popularRooms = remember(rooms) {
        rooms.sortedByDescending { it.listenerCount + it.activeSpeakersCount }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(exploreCanvasColor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Explore VibePlay",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search users, Vibe ID (VP-...), or rooms...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.testTag("explore_clear_search_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RoomCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }
        }

        // Trending Users
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = VibePink, modifier = Modifier.size(20.dp))
                Text(
                    text = "Trending Creators & Users",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (filteredUsers.isEmpty()) {
            item {
                Text(
                    text = "No matching users found yet. Invite friends to join VibePlay!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filteredUsers.take(12), key = { it.userId }) { user ->
                val isFollowing = user.userId in followingIds
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUserProfile(user) }
                        .testTag("explore_user_${user.userId}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VibeAvatar(
                            avatarUrl = user.avatarUrl,
                            username = user.username,
                            size = 52.dp,
                            vipLevel = if (user.isVipActive) user.vipLevel else 0L,
                            online = user.online && user.showOnlineStatus,
                            frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == user.activeFrameId && user.isFrameActive(it.id) }?.colorArgb
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VipShiningNameText(
                                    text = user.username,
                                    vipLevel = if (user.isVipActive) user.vipLevel else 0L,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                LevelBadge(user.level)
                                if (user.isVipActive) VipMiniBadge(user.vipLevel)
                            }
                            val ageTag = if (user.showAge && user.formattedAge != null) " · ${user.formattedAge}" else ""
                            Text(
                                text = "${user.vibeId}$ageTag · ${user.followersCount} followers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onToggleFollow(user, isFollowing) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else VibeViolet,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("explore_follow_${user.userId}")
                        ) {
                            Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // New Rooms
        if (newRooms.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.NewReleases, contentDescription = null, tint = VibeCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = "New Rooms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(newRooms, key = { "new_${it.id}" }) { room ->
                        VoiceRoomCard(
                            room = room,
                            onClick = { onOpenRoom(room) },
                            modifier = Modifier.width(252.dp)
                        )
                    }
                }
            }
        }

        // Popular Rooms
        if (popularRooms.isNotEmpty()) {
            item {
                Text(
                    text = "Popular & Recommended",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(popularRooms.chunked(2), key = { row -> "exp_pop_${row.first().id}" }) { roomRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    roomRow.forEach { room ->
                        VoiceRoomCard(
                            room = room,
                            onClick = { onOpenRoom(room) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (roomRow.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
