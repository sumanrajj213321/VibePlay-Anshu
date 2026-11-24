package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FollowEdge
import com.example.data.model.CharmProgression
import com.example.data.model.GiftTransaction
import com.example.data.model.LevelProgression
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.ui.components.BackendIntegrationGuideSheet
import com.example.ui.components.BiologicalGenderBadge
import com.example.ui.components.LevelBadge
import com.example.ui.components.VibeCoinIcon
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
import com.example.ui.components.VipShiningNameText
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    profile: UserProfile,
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
    onSignOut: () -> Unit,
    onOpenShop: () -> Unit = {},
    onOpenRelationships: () -> Unit = {},
    onOpenCharmLevels: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val isOwnProfile = FirebaseAuth.getInstance().currentUser?.uid == profile.userId
    var showEditSheet by remember { mutableStateOf(false) }
    var showVipSheet by remember { mutableStateOf(false) }
    var showGiftHistory by remember { mutableStateOf(false) }
    var showSocialSheet by remember { mutableStateOf(false) }
    var socialInitialTab by remember { mutableStateOf(0) }
    var showSetupGuide by remember { mutableStateOf(false) }
    var coverLoadFailed by remember(profile.coverUrl) { mutableStateOf(false) }
    var firestoreCoverBitmap by remember(profile.userId, profile.coverUrl) { mutableStateOf<Bitmap?>(null) }
    val isFirestoreCover = profile.coverUrl.startsWith("firestore-cover://")

    DisposableEffect(profile.userId, profile.coverUrl) {
        if (!isFirestoreCover) {
            firestoreCoverBitmap = null
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(profile.userId)
                .collection("cover")
                .document("image")
                .addSnapshotListener { snapshot, _ ->
                    val encoded = snapshot?.getString("data")
                    firestoreCoverBitmap = try {
                        encoded?.let { value ->
                            val bytes = android.util.Base64.decode(value, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            onDispose { registration.remove() }
        }
    }

    val (xpInLevel, xpNeededForNextLevel) = LevelProgression.progressInLevel(profile.xp)
    val giftCoinsIn = giftHistory.filter { it.recipientId == profile.userId }.sumOf { it.coinCost }
    val giftCoinsOut = giftHistory.filter { it.senderId == profile.userId }.sumOf { it.coinCost }
    var showLinkedRelationship by remember(profile.userId) { mutableStateOf(true) }
    var linkedPartnerId by remember(profile.userId) { mutableStateOf("") }
    var linkedPartnerName by remember(profile.userId) { mutableStateOf("") }
    var linkedPartnerAvatar by remember(profile.userId) { mutableStateOf("") }
    var linkedTagType by remember(profile.userId) { mutableStateOf("") }
    var allRelationshipTags by remember(profile.userId) { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var showAllRelationshipTags by remember(profile.userId) { mutableStateOf(true) }
    var selectedFeaturedPartnerId by remember(profile.userId) { mutableStateOf("") }
    var relationshipFeatureStatus by remember(profile.userId) { mutableStateOf("") }

    val currentFeaturedPartnerId by rememberUpdatedState(selectedFeaturedPartnerId)
    DisposableEffect(profile.userId) {
        val selectionRegistration = FirebaseFirestore.getInstance()
            .collection("relationship_profile_selection")
            .document(profile.userId)
            .addSnapshotListener { snapshot, _ ->
                selectedFeaturedPartnerId = snapshot?.getString("partnerId").orEmpty()
            }
        val tagsRegistration = FirebaseFirestore.getInstance()
            .collection("relationship_public_tags")
            .whereArrayContains("userIds", profile.userId)
            .addSnapshotListener { snapshot, _ ->
                allRelationshipTags = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val ids = (doc.get("userIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    if (profile.userId !in ids) return@mapNotNull null
                    val isA = ids.sorted().firstOrNull() == profile.userId
                    mapOf(
                        "partnerId" to (ids.firstOrNull { it != profile.userId } ?: ""),
                        "partnerName" to (if (isA) doc.getString("userBName") else doc.getString("userAName")).orEmpty(),
                        "partnerAvatarUrl" to (if (isA) doc.getString("userBAvatarUrl") else doc.getString("userAAvatarUrl")).orEmpty(),
                        "tagType" to doc.getString("tagType").orEmpty(),
                        "pairId" to doc.id
                    )
                }.filter { it["tagType"].orEmpty().isNotBlank() }
            }
        var partnerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        val pairRegistration = FirebaseFirestore.getInstance()
            .collection("relationship_pairs")
            .whereArrayContains("userIds", profile.userId)
            .addSnapshotListener { snapshot, _ ->
                val activePairs = snapshot?.documents.orEmpty().filter {
                    it.getString("tagStatus") == "ACTIVE"
                }
                val activePair = activePairs.firstOrNull { pair ->
                    val ids = pair.get("userIds") as? List<*>
                    ids?.filterIsInstance<String>()?.any { it == currentFeaturedPartnerId } == true
                } ?: activePairs.firstOrNull()
                val ids = activePair?.get("userIds") as? List<*>
                val otherId = ids?.filterIsInstance<String>()?.firstOrNull { it != profile.userId }.orEmpty()
                linkedTagType = activePair?.getString("tagType").orEmpty()
                if (otherId.isBlank()) {
                    partnerRegistration?.remove()
                    partnerRegistration = null
                    linkedPartnerId = ""
                    linkedPartnerName = ""
                    linkedPartnerAvatar = ""
                } else if (otherId != linkedPartnerId) {
                    linkedPartnerId = otherId
                    partnerRegistration?.remove()
                    partnerRegistration = FirebaseFirestore.getInstance()
                        .collection("users").document(otherId)
                        .addSnapshotListener { userSnapshot, _ ->
                            linkedPartnerName = userSnapshot?.getString("username").orEmpty()
                            linkedPartnerAvatar = userSnapshot?.getString("avatarUrl").orEmpty()
                        }
                }
            }
        onDispose {
            selectionRegistration.remove()
            tagsRegistration.remove()
            pairRegistration.remove()
            partnerRegistration?.remove()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (linkedPartnerId.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = VibeGold.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.48f))
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Connected Relationship", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (showLinkedRelationship) "Hide" else "Show",
                                modifier = Modifier.clickable { showLinkedRelationship = !showLinkedRelationship }.padding(5.dp),
                                color = VibeCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (showLinkedRelationship) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                VibeAvatar(avatarUrl = profile.avatarUrl, username = profile.username, size = 54.dp)
                                Text("✦", modifier = Modifier.padding(horizontal = 12.dp), color = VibeGold, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                VibeAvatar(avatarUrl = linkedPartnerAvatar, username = linkedPartnerName, size = 54.dp)
                            }
                            Text(
                                "${linkedPartnerName.ifBlank { "VibePlay user" }} · ${linkedTagType.lowercase().replaceFirstChar { it.uppercase() }}",
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = VibeGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (allRelationshipTags.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = VibeGold.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.38f))
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("All Relationship Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (showAllRelationshipTags) "Hide" else "Show",
                                modifier = Modifier.clickable { showAllRelationshipTags = !showAllRelationshipTags }.padding(5.dp),
                                color = VibeCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (showAllRelationshipTags) {
                            allRelationshipTags.forEach { tag ->
                                val partnerId = tag["partnerId"].orEmpty()
                                val partnerName = tag["partnerName"].orEmpty().ifBlank { "VibePlay user" }
                                val isFeatured = partnerId.isNotBlank() && partnerId == selectedFeaturedPartnerId
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    VibeAvatar(
                                        avatarUrl = tag["partnerAvatarUrl"].orEmpty(),
                                        username = partnerName,
                                        size = 42.dp
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            partnerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            tag["tagType"].orEmpty().lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VibeGold
                                        )
                                    }
                                    if (isOwnProfile) {
                                    OutlinedButton(
                                        enabled = partnerId.isNotBlank() && !isFeatured,
                                        onClick = {
                                            relationshipFeatureStatus = "Updating featured partner…"
                                            val pairId = tag["pairId"].orEmpty()
                                            if (pairId.isBlank()) {
                                                relationshipFeatureStatus = "Relationship pair is unavailable."
                                            } else {
                                                FirebaseFirestore.getInstance()
                                                    .collection("relationship_profile_selection")
                                                    .document(profile.userId)
                                                    .set(
                                                        mapOf(
                                                            "userId" to profile.userId,
                                                            "partnerId" to partnerId,
                                                            "pairId" to pairId,
                                                            "updatedAt" to FieldValue.serverTimestamp()
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        selectedFeaturedPartnerId = partnerId
                                                        relationshipFeatureStatus = "$partnerName is now featured on your profile."
                                                    }
                                                    .addOnFailureListener { error ->
                                                        relationshipFeatureStatus = error.localizedMessage
                                                            ?: "Could not update featured partner."
                                                    }
                                            }
                                        }
                                    ) {
                                        Text(if (isFeatured) "Featured" else "Feature")
                                    }
                                    }
                                }
                            }
                        }
                        if (relationshipFeatureStatus.isNotBlank()) {
                            Text(
                                relationshipFeatureStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Cover + Avatar Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                if (isFirestoreCover && firestoreCoverBitmap != null) {
                    AsyncImage(
                        model = firestoreCoverBitmap,
                        contentDescription = "Profile cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop
                    )
                } else if (!isFirestoreCover && profile.coverUrl.isNotBlank() && !coverLoadFailed) {
                    AsyncImage(
                        model = profile.coverUrl,
                        contentDescription = "Profile cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop,
                        onError = { coverLoadFailed = true }
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

        // Premium identity: username/VIP, ID number, level and equipped title.
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VipShiningNameText(
                        text = profile.username,
                        vipLevel = if (profile.isVipActive) profile.vipLevel else 0L,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    BiologicalGenderBadge(
                        gender = profile.gender,
                        showGender = true,
                        compact = true
                    )
                    if (profile.isVipActive) {
                        VipBadgePill(profile.vipLevel)
                    }
                }

                if (profile.userId == "1fLIHgOyAJV6LNbHBXNeVg9JR6v1") {
                    val officialShimmer = rememberInfiniteTransition(label = "official_title_shimmer")
                    val shimmerOffset by officialShimmer.animateFloat(
                        initialValue = -260f,
                        targetValue = 520f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2300, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "official_title_shimmer_offset"
                    )
                    val officialGold = Color(0xFFFFD76A)
                    val officialShine = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF9B6A16),
                            Color(0xFFFFD76A),
                            Color(0xFFFFF4C2),
                            Color(0xFFFFC83D),
                            Color(0xFF9B6A16)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 260f, 0f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF080807),
                        border = BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF8A641F), Color(0xFFFFE6A0), Color(0xFF9C711F))
                            )
                        ),
                        shadowElevation = 7.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = officialGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "VibePlay Official",
                                modifier = Modifier,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    brush = officialShine,
                                    letterSpacing = 0.75f.sp
                                ),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = officialGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(profile.vibeId))
                                onCopyVibeId?.invoke(profile.vibeId)
                            }
                            .testTag("profile_id_age_line"),
                        shape = RoundedCornerShape(14.dp),
                        color = VibeCyan.copy(alpha = 0.09f),
                        border = BorderStroke(1.dp, VibeCyan.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ID: VP-${profile.vibeId.removePrefix("VP-")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Vibe ID",
                                tint = VibeCyan,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        color = VibeGold.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = "Level ${profile.level}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = VibeGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Neon Charms earned from received gifts (10 gift coins = 1 charm).
                val charms = CharmProgression.fromReceivedGiftCoins(profile.totalGiftCoinsReceived)
                val charmLevel = CharmProgression.levelFor(charms)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF102B3A).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF35F4E5), Color(0xFF8B72FF), Color(0xFFFF78D6)))),
                    modifier = Modifier.clickable(onClick = onOpenCharmLevels).testTag("profile_charms_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(if (charmLevel >= 2L) Icons.Filled.EmojiEvents else Icons.Filled.AutoAwesome, contentDescription = "Charm level", tint = if (charmLevel >= 2L) Color(0xFFFFD76A) else Color(0xFF65FFF0), modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("CHARMS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF65FFF0), fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                            Text("$charms  ·  Charm Level $charmLevel", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE9FFFF), fontWeight = FontWeight.ExtraBold)
                        }
                    }
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
                        VibeCoinIcon(modifier = Modifier.size(32.dp))
                        Text("${profile.coinBalance}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibeGold)
                        Text("10 coins = 1 charm", style = MaterialTheme.typography.labelSmall, color = VibeGold)
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
                            text = if (profile.isVipActive) "VIP ${profile.vipLevel.coerceAtMost(8L)} Active" else "Unlock VIP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibePink
                        )
                        Text("Badges & Room Effects", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Shop shortcut appears above Gifts & Achievements.
        item {
            ProfileActionRow(
                icon = Icons.Filled.AutoAwesome,
                title = "VibePlay Shop",
                subtitle = "Buy VIP and premium frames with coins",
                accentTint = VibeGold,
                onClick = onOpenShop
            )
        }

        // Relationship shortcut sits between Shop and Gifts & Achievements.
        item {
            ProfileActionRow(
                icon = Icons.Filled.Group,
                title = "Relationships",
                subtitle = "Buddies · Bro · Confidant · Bestie",
                accentTint = VibePink,
                onClick = onOpenRelationships
            )
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
                                if (profile.isVipActive) append(" · VIP ${profile.vipLevel.coerceAtMost(8L)}")
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

private fun formatGiftCount(value: Long): String {
    if (value < 1_000L) return value.toString()
    val scaled = when {
        value >= 1_000_000L -> value / 1_000_000.0
        else -> value / 1_000.0
    }
    val suffix = if (value >= 1_000_000L) "M" else "K"
    val digits = if (scaled >= 100.0 || scaled % 1.0 == 0.0) 0 else 1
    return "%.${digits}f%s".format(java.util.Locale.US, scaled, suffix)
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
