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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FollowEdge
import com.example.data.model.GiftTransaction
import com.example.data.model.LevelProgression
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.ui.components.BackendIntegrationGuideSheet
import com.example.ui.components.BiologicalGenderBadge
import com.example.ui.components.LevelBadge
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet

@Composable
fun ProfileScreen(
    profile: UserProfile,
    isAdmin: Boolean,
    darkTheme: Boolean,
    roomEffectsEnabled: Boolean = true,
    followingList: List<FollowEdge>,
    followersList: List<FollowEdge>,
    giftHistory: List<GiftTransaction>,
    blockedUsers: List<UserProfile>,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleRoomEffects: (Boolean) -> Unit = {},
    onEditProfile: (String, String, String, String, String, Long, Boolean, Boolean, Boolean) -> Unit,
    onUpgradeVip: (Int) -> Unit,
    onClaimDailyReward: () -> Unit,
    onInspectUserById: ((String) -> Unit)? = null,
    onCopyVibeId: ((String) -> Unit)? = null,
    onUnblockUser: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    onOpenShop: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var showEditSheet by remember { mutableStateOf(false) }
    var showVipSheet by remember { mutableStateOf(false) }
    var showGiftHistory by remember { mutableStateOf(false) }
    var showSocialSheet by remember { mutableStateOf(false) }
    var socialInitialTab by remember { mutableStateOf(0) }
    var showSetupGuide by remember { mutableStateOf(false) }

    val (xpInLevel, xpNeededForNextLevel) = LevelProgression.progressInLevel(profile.xp)
    val giftCoinsIn = giftHistory.filter { it.recipientId == profile.userId }.sumOf { it.coinCost }
    val giftCoinsOut = giftHistory.filter { it.senderId == profile.userId }.sumOf { it.coinCost }
    val xpProgress = (xpInLevel.toFloat() / xpNeededForNextLevel.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cover + Avatar Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                if (profile.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = profile.coverUrl,
                        contentDescription = "Profile cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_party_stage),
                        contentDescription = "Default profile cover",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.25f), MaterialTheme.colorScheme.background)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    VibeAvatar(
                        avatarUrl = profile.avatarUrl,
                        username = profile.username,
                        size = 86.dp,
                        vipLevel = if (profile.isVipActive) profile.vipLevel else 0L,
                        online = profile.online && profile.showOnlineStatus,
                        frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == profile.activeFrameId && profile.isFrameActive(it.id) }?.colorArgb
                    )
                    OutlinedButton(
                        onClick = { showEditSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }

        // Identity & Stats
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    VibeShopCatalog.titles.firstOrNull { it.id == profile.activeTitleId && profile.isTitleActive(it.id) }?.let { title ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(title.colorArgb).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(title.colorArgb).copy(alpha = 0.75f))
                        ) {
                            Text(
                                text = title.title,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(title.colorArgb),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    if (profile.userId == "1fLIHgOyAJV6LNbHBXNeVg9JR6v1") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VibeCyan.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "VibePlay Official",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = VibeCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    BiologicalGenderBadge(
                        gender = profile.gender,
                        showGender = profile.showGender
                    )
                    LevelBadge(profile.level)
                    if (profile.isVipActive) VipBadgePill(profile.vipLevel)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(profile.vibeId))
                        onCopyVibeId?.invoke(profile.vibeId)
                    }
                ) {
                    val ageSuffix = if (profile.showAge && profile.formattedAge != null) " · ${profile.formattedAge}" else ""
                    Text(
                        text = "ID: ${profile.vibeId}$ageSuffix",
                        style = MaterialTheme.typography.labelMedium,
                        color = VibeCyan,
                        modifier = Modifier.testTag("profile_id_age_line")
                    )
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy Vibe ID",
                        tint = VibeCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Level ${profile.level} XP", style = MaterialTheme.typography.labelSmall)
                        Text("$xpInLevel/$xpNeededForNextLevel XP", style = MaterialTheme.typography.labelSmall, color = VibeCyan)
                    }
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = VibeViolet
                    )
                }

                // Stats Row
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(
                            value = "${profile.followersCount}",
                            label = "Followers",
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    socialInitialTab = 0
                                    showSocialSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("profile_followers_stat")
                        )
                        ProfileStat(
                            value = "${profile.followingCount}",
                            label = "Following",
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    socialInitialTab = 1
                                    showSocialSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("profile_following_stat")
                        )
                        ProfileStat(
                            value = "${profile.totalGiftCoinsReceived.takeIf { it > 0L } ?: giftCoinsIn}",
                            label = "Total Gifts",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Coin Wallet & VIP Quick Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VibeGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClaimDailyReward)
                        .testTag("profile_wallet_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = VibeGold)
                        Text("${profile.coinBalance} Coins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibeGold)
                        Text("Tap to claim daily +150", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VibePink.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, VibePink.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showVipSheet = true }
                        .testTag("profile_vip_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = VibePink)
                        Text(
                            text = if (profile.isVipActive) "VIP ${profile.vipLevel} Active" else "Unlock VIP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibePink
                        )
                        Text("Badges & Room Effects", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Achievements Strip
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = VibeGold, modifier = Modifier.size(28.dp))
                    Column {
                        Text("Gifts & Achievements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = buildString {
                                append("Level ${profile.level} Host")
                                if (profile.isVipActive) append(" · VIP ${profile.vipLevel}")
                                if (profile.totalGiftsSent > 0) append(" · Generous Gifter")
                                if (profile.totalGiftsReceived > 0) append(" · Stage Star")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Menu List
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileActionRow(
                    icon = Icons.Filled.Group,
                    title = "Followers, Following & Blocked",
                    subtitle = "${profile.followersCount} followers · ${profile.followingCount} following",
                    onClick = {
                        socialInitialTab = 0
                        showSocialSheet = true
                    }
                )
                ProfileActionRow(
                    icon = Icons.Filled.CardGiftcard,
                    title = "Gift In: $giftCoinsIn coins · Gift Out: +$giftCoinsOut coins",
                    subtitle = "${giftHistory.size} transactions recorded",
                    onClick = { showGiftHistory = true }
                )
                ProfileActionRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications Center",
                    subtitle = "Followers, invites, gifts & alerts",
                    onClick = onOpenNotifications
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = VibeViolet)
                            Column {
                                Text("Dark Aurora Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Switch between dark & light mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("theme_toggle_switch")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VibePink)
                            Column {
                                Text("Stage Gift & VIP Animations", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Show floating gift & VIP entrance banners", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = roomEffectsEnabled,
                            onCheckedChange = onToggleRoomEffects,
                            modifier = Modifier.testTag("room_effects_toggle_switch")
                        )
                    }
                }

                ProfileActionRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = "VibePlay Shop",
                    subtitle = "Buy VIP, premium frames and profile titles with coins",
                    accentTint = VibeGold,
                    onClick = onOpenShop
                )

                ProfileActionRow(
                    icon = Icons.Filled.CloudQueue,
                    title = "LiveKit & Backend Setup Guide",
                    subtitle = "Check Secrets & Firebase Console integration status",
                    onClick = { showSetupGuide = true }
                )

                if (isAdmin) {
                    ProfileActionRow(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "Admin Moderation Panel",
                        subtitle = "Manage users, rooms, reports, gifts & VIP",
                        accentTint = VibeGold,
                        onClick = onOpenAdminPanel
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeCoral.copy(alpha = 0.18f), contentColor = VibeCoral),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Logout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showEditSheet) {
        EditProfileSheet(
            profile = profile,
            onDismiss = { showEditSheet = false },
            onSave = onEditProfile
        )
    }

    if (showVipSheet) {
        VipMembershipSheet(
            profile = profile,
            onDismiss = { showVipSheet = false },
            onUpgradeVip = onUpgradeVip
        )
    }

    if (showGiftHistory) {
        GiftHistorySheet(
            transactions = giftHistory,
            currentUserId = profile.userId,
            onDismiss = { showGiftHistory = false }
        )
    }

    if (showSocialSheet) {
        SocialGraphSheet(
            title = "Followers & Following",
            initialTab = socialInitialTab,
            following = followingList,
            followers = followersList,
            blockedUsers = blockedUsers,
            showBlockedTab = true,
            onInspectUserById = onInspectUserById,
            onUnblockUser = onUnblockUser,
            onDismiss = { showSocialSheet = false }
        )
    }

    if (showSetupGuide) {
        BackendIntegrationGuideSheet(onDismiss = { showSetupGuide = false })
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentTint: Color = VibeCyan,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentTint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
