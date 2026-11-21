package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FollowEdge
import com.example.data.model.GiftTransaction
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.ui.components.BiologicalGenderBadge
import com.example.ui.components.LevelBadge
import com.example.ui.components.ReportUserOrRoomDialog
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (
        username: String,
        bio: String,
        avatarUrl: String,
        coverUrl: String,
        gender: String,
        age: Long,
        showAge: Boolean,
        showGender: Boolean,
        showOnlineStatus: Boolean
    ) -> Unit
) {
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var avatarUrl by remember(profile.avatarUrl) { mutableStateOf(profile.avatarUrl) }
    var coverUrl by remember(profile.coverUrl) { mutableStateOf(profile.coverUrl) }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var ageText by remember(profile.age) {
        mutableStateOf(if (profile.age > 0L) profile.age.toString() else "")
    }
    var ageError by remember { mutableStateOf<String?>(null) }
    var showAge by remember(profile.showAge) { mutableStateOf(profile.showAge) }
    var showGender by remember(profile.showGender) { mutableStateOf(profile.showGender) }
    var showOnline by remember(profile.showOnlineStatus) { mutableStateOf(profile.showOnlineStatus) }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { avatarUrl = it.toString() } }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { coverUrl = it.toString() } }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit Profile & Privacy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Avatar Photo")
                }
                OutlinedButton(
                    onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cover Photo")
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(30) },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(200) },
                label = { Text("Bio") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Gender", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("UNSPECIFIED", "FEMALE", "MALE", "NON_BINARY").forEach { g ->
                    val label = when (g) {
                        "MALE" -> "\u2642\uFE0F Male"
                        "FEMALE" -> "\u2640\uFE0F Female"
                        "NON_BINARY" -> "Non-binary"
                        else -> "Unspecified"
                    }
                    FilterChip(
                        selected = gender == g,
                        onClick = { gender = g },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = ageText,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(3)
                    ageText = digits
                    ageError = null
                },
                label = { Text("Exact Age (13–120 years)") },
                placeholder = { Text("e.g. 21") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = ageError != null,
                supportingText = ageError?.let { err -> { Text(err) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_profile_age_input")
            )

            PrivacySwitchRow("Show Gender Badge on Public Profile", showGender) { showGender = it }
            PrivacySwitchRow("Show Exact Age on Public Profile", showAge) { showAge = it }
            PrivacySwitchRow("Show Online Presence", showOnline) { showOnline = it }

            Button(
                onClick = {
                    val parsedAge = if (ageText.isBlank()) {
                        0L
                    } else {
                        ageText.toLongOrNull() ?: -1L
                    }
                    if (parsedAge != 0L && parsedAge !in 13L..120L) {
                        ageError = "Enter an exact age between 13 and 120, or clear the field."
                    } else {
                        onSave(username, bio, avatarUrl, coverUrl, gender, parsedAge, showAge, showGender, showOnline)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_profile_button")
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PrivacySwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipMembershipSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onUpgradeVip: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Image(
                    painter = painterResource(id = R.drawable.img_cover_vip_crown),
                    contentDescription = "VIP Crown Showcase",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VIP Membership", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Balance: ${profile.coinBalance} Coins", style = MaterialTheme.typography.labelLarge, color = VibeGold, fontWeight = FontWeight.Bold)
                }
            }
            item {
                val activeTier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == profile.vipLevel.toInt() }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeTier != null) VibeGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (activeTier != null) "Current Status: ${activeTier.title}" else "Current Status: Standard Member",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTier != null) VibeGold else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activeTier?.perks?.joinToString(" · ")
                                ?: "Upgrade to unlock VIP badges, stage entrance banners, and XP boosts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(VibeCatalogDefaults.vipTiers, key = { it.level }) { tier ->
                val accent = Color(tier.badgeColorHex)
                val owned = profile.vipLevel >= tier.level
                val canAfford = profile.coinBalance >= tier.coinPrice
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Text(tier.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                            }
                            Text("${tier.coinPrice} Coins", style = MaterialTheme.typography.labelMedium, color = VibeGold, fontWeight = FontWeight.Bold)
                        }
                        Text("Entrance: ${tier.entranceEffect}", style = MaterialTheme.typography.labelSmall, color = VibeCyan)
                        Text(tier.perks.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { onUpgradeVip(tier.level) },
                            enabled = !owned && canAfford,
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    owned -> "Unlocked"
                                    canAfford -> "Activate VIP ${tier.level}"
                                    else -> "Need ${tier.coinPrice - profile.coinBalance} more coins"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftHistorySheet(
    transactions: List<GiftTransaction>,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Virtual Gift History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No virtual gifts sent or received yet. Join a voice room to send gifts!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val sentByMe = tx.senderId == currentUserId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (sentByMe) "Sent ${tx.giftName} to ${tx.recipientName}" else "Received ${tx.giftName} from ${tx.senderName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Qty: ${tx.quantity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${if (sentByMe) "-" else "+"}${tx.coinCost}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (sentByMe) VibePink else VibeEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialGraphSheet(
    title: String = "Followers & Following",
    initialTab: Int = 0,
    following: List<FollowEdge>,
    followers: List<FollowEdge>,
    blockedUsers: List<UserProfile> = emptyList(),
    showBlockedTab: Boolean = true,
    onInspectUserById: ((String) -> Unit)? = null,
    onUnblockUser: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val maxTab = if (showBlockedTab) 2 else 1
    var selectedTab by remember(initialTab, showBlockedTab) {
        mutableIntStateOf(initialTab.coerceIn(0, maxTab))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Followers (${followers.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Following (${following.size})") }
                )
                if (showBlockedTab) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Blocked (${blockedUsers.size})") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (followers.isEmpty()) {
                            item {
                                Text(
                                    text = "No followers yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(followers, key = { "fwer_${it.peerId}" }) { edge ->
                                FollowEdgeRow(
                                    edge = edge,
                                    onClick = if (onInspectUserById != null) {
                                        {
                                            onDismiss()
                                            onInspectUserById(edge.peerId)
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        if (following.isEmpty()) {
                            item {
                                Text(
                                    text = "Not following anyone yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(following, key = { "fwing_${it.peerId}" }) { edge ->
                                FollowEdgeRow(
                                    edge = edge,
                                    onClick = if (onInspectUserById != null) {
                                        {
                                            onDismiss()
                                            onInspectUserById(edge.peerId)
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        if (blockedUsers.isEmpty()) {
                            item {
                                Text(
                                    text = "No blocked users.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(blockedUsers, key = { "blk_${it.userId}" }) { blk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        VibeAvatar(blk.avatarUrl, blk.username, size = 38.dp)
                                        Text(blk.username, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    TextButton(onClick = { onUnblockUser(blk.userId) }) { Text("Unblock") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowEdgeRow(
    edge: FollowEdge,
    onClick: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(vertical = 4.dp)
            .testTag("follow_edge_row_${edge.peerId}")
    ) {
        VibeAvatar(edge.peerAvatarUrl, edge.peerName, size = 40.dp)
        Text(
            text = edge.peerName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileSheet(
    targetUser: UserProfile,
    currentUserId: String = "",
    isFollowing: Boolean,
    followersList: List<FollowEdge> = emptyList(),
    followingList: List<FollowEdge> = emptyList(),
    myHostedRoom: VoiceRoom?,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onStartChat: () -> Unit,
    onInviteToRoom: (VoiceRoom) -> Unit,
    onBlockUser: () -> Unit,
    onInspectUserById: ((String) -> Unit)? = null,
    onSubmitReport: (reason: String, details: String) -> Unit
) {
    var showReport by remember { mutableStateOf(false) }
    var followSheetTab by remember { mutableStateOf<Int?>(null) }
    val isSelf = currentUserId.isNotBlank() && targetUser.userId == currentUserId

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSelf) "Your Profile" else "User Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                if (targetUser.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = targetUser.coverUrl,
                        contentDescription = "Profile cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_party_stage),
                        contentDescription = "Default profile cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VibeAvatar(
                    avatarUrl = targetUser.avatarUrl,
                    username = targetUser.username,
                    size = 68.dp,
                    vipLevel = if (targetUser.isVipActive) targetUser.vipLevel else 0L,
                    online = targetUser.online && targetUser.showOnlineStatus,
                    frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == targetUser.activeFrameId && targetUser.isFrameActive(it.id) }?.colorArgb
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(targetUser.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        VibeShopCatalog.titles.firstOrNull { it.id == targetUser.activeTitleId && targetUser.isTitleActive(it.id) }?.let { title ->
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(title.colorArgb).copy(alpha = 0.16f),
                                border = BorderStroke(
                                    1.dp,
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            Color(title.colorArgb).copy(alpha = 0.95f),
                                            Color.White.copy(alpha = 0.48f),
                                            Color(title.colorArgb).copy(alpha = 0.78f)
                                        )
                                    )
                                ),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = title.title.uppercase(),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(title.colorArgb),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        if (targetUser.userId == "1fLIHgOyAJV6LNbHBXNeVg9JR6v1") {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF080807),
                                border = BorderStroke(
                                    1.2.dp,
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(Color(0xFF8A641F), Color(0xFFFFE6A0), Color(0xFF9C711F))
                                    )
                                ),
                                shadowElevation = 5.dp
                            ) {
                                Text(
                                    text = "★ VibePlay Official ★",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFD76A),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        BiologicalGenderBadge(
                            gender = targetUser.gender,
                            showGender = targetUser.showGender,
                            compact = true
                        )
                        LevelBadge(targetUser.level)
                        if (targetUser.isVipActive) VipBadgePill(targetUser.vipLevel)
                    }
                    val ageSuffix = if (targetUser.showAge && targetUser.formattedAge != null) " · ${targetUser.formattedAge}" else ""
                    Text(
                        text = "${targetUser.vibeId}$ageSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeCyan,
                        modifier = Modifier.testTag("public_profile_id_age")
                    )
                }
            }

            if (targetUser.bio.isNotBlank()) {
                Text(targetUser.bio, style = MaterialTheme.typography.bodyMedium)
            }

            // Clickable Followers / Following summary for this user
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { followSheetTab = 0 }
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .testTag("public_profile_followers_stat")
                    ) {
                        Text("${targetUser.followersCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Followers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { followSheetTab = 1 }
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .testTag("public_profile_following_stat")
                    ) {
                        Text("${targetUser.followingCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Following", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${targetUser.totalGiftCoinsReceived}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Total Gifts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!isSelf) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onToggleFollow,
                        colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("public_profile_follow_button")
                    ) {
                        Text(if (isFollowing) "Unfollow" else "Follow")
                    }
                    OutlinedButton(
                        onClick = onStartChat,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("public_profile_message_button")
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message")
                    }
                }

                if (myHostedRoom != null) {
                    OutlinedButton(
                        onClick = { onInviteToRoom(myHostedRoom) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Invite to Room")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showReport = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report")
                    }
                    OutlinedButton(
                        onClick = { onBlockUser(); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeCoral),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Block")
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibeCyan.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This is your public profile card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeCyan,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
        }
    }

    followSheetTab?.let { tab ->
        SocialGraphSheet(
            title = "${targetUser.username}'s Network",
            initialTab = tab,
            following = followingList,
            followers = followersList,
            showBlockedTab = false,
            onInspectUserById = { peerId ->
                followSheetTab = null
                onDismiss()
                onInspectUserById?.invoke(peerId)
            },
            onDismiss = { followSheetTab = null }
        )
    }

    if (showReport) {
        ReportUserOrRoomDialog(
            targetName = targetUser.username,
            onDismiss = { showReport = false },
            onSubmit = { reason, details ->
                onSubmitReport(reason, details)
                showReport = false
            }
        )
    }
}
