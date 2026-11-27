package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Add
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.InputStream
import android.content.Intent
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.CharmEmblem
import com.example.ui.components.GiftCatalogArtwork
import com.example.data.model.GiftTransaction
import com.example.data.model.GiftCatalogItem
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.LevelProgression
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.ui.components.BackendIntegrationGuideSheet
import com.example.ui.components.BiologicalGenderBadge
import com.example.ui.components.LevelBadge
import com.example.ui.components.VibeCoinIcon
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipMiniBadge
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
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    onEditProfile: (String, String, String, String, String, Long, Boolean, Boolean, Boolean, List<String>) -> Unit,
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
    var showLevelInfo by remember { mutableStateOf(false) }
    var selectedGiftPrice by remember { mutableStateOf<GiftCatalogItem?>(null) }
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
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
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
                    .height(190.dp)
            ) {
                if (isFirestoreCover && firestoreCoverBitmap != null) {
                    AsyncImage(
                        model = firestoreCoverBitmap,
                        contentDescription = "Profile cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (!isFirestoreCover && profile.coverUrl.isNotBlank() && !coverLoadFailed) {
                    AsyncImage(
                        model = profile.coverUrl,
                        contentDescription = "Profile cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Fit,
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
                        size = 78.dp,
                        vipLevel = if (profile.isVipActive) profile.vipLevel else 0L,
                        online = profile.online && profile.showOnlineStatus,
                        frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == profile.activeFrameId && profile.isFrameActive(it.id) }?.colorArgb
                    )
                    if (isOwnProfile) {
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
        }

        // Premium identity: username/VIP, ID number, level and equipped title.
        item {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
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
                        VipMiniBadge(profile.vipLevel)
                    }
                }
                if (profile.profileTags.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        profile.profileTags.take(4).forEach { tag ->
                            Surface(shape = RoundedCornerShape(50), color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))) {
                                Text(tag, modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_level_card"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        LevelBadge(
                            level = LevelProgression.levelForXp(profile.xp),
                            modifier = Modifier
                                .clickable { showLevelInfo = true }
                                .padding(2.dp)
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
                        CharmEmblem(level = charmLevel.toInt().coerceAtLeast(1), size = 22.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("CHARMS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF65FFF0), fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                            Text("$charms  ·  ${CharmProgression.nameForLevel(charmLevel)}", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE9FFFF), fontWeight = FontWeight.ExtraBold)
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
                                .then(
                                    if (isOwnProfile) Modifier.clickable {
                                        socialInitialTab = 0
                                        showSocialSheet = true
                                    } else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("profile_followers_stat")
                        )
                        ProfileStat(
                            value = "${profile.followingCount}",
                            label = "Following",
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isOwnProfile) Modifier.clickable {
                                        socialInitialTab = 1
                                        showSocialSheet = true
                                    } else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("profile_following_stat")
                        )
                    }
                }
            }
        }

        // Gift Wall: distinct gifts received by this profile with their combined quantities.
        item {
            val receivedGiftWall = remember(giftHistory, profile.userId) {
                giftHistory
                    .filter {
                        it.recipientId == profile.userId &&
                            it.giftName.isNotBlank() &&
                            it.giftId in setOf(
                                "gift_sunset_love",
                                "gift_royal_vows",
                                "gift_sunlit_dream",
                                "gift_prism_muse"
                            )
                    }
                    .groupBy { it.giftId.ifBlank { it.giftName.trim().lowercase() } }
                    .map { (_, transactions) ->
                        val first = transactions.first()
                        GiftCatalogItem(id = first.giftId, name = first.giftName, coinCost = first.coinCost) to
                            transactions.sumOf { it.quantity.coerceAtLeast(1L) }
                    }
                    .sortedByDescending { it.second }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gift Wall", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("${receivedGiftWall.size} gifts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (receivedGiftWall.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text("Gifts received will appear here", modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        receivedGiftWall.forEach { (gift, quantity) ->
                            Surface(
                                modifier = Modifier.width(112.dp).clickable {
                                    selectedGiftPrice = gift
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                                border = BorderStroke(1.dp, VibeCyan.copy(alpha = 0.20f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(66.dp).background(
                                            Brush.linearGradient(listOf(VibeViolet.copy(alpha = 0.18f), VibeCyan.copy(alpha = 0.12f))),
                                            RoundedCornerShape(15.dp)
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GiftCatalogArtwork(gift = gift, modifier = Modifier.size(58.dp))
                                    }
                                    Text(gift.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("×$quantity", style = MaterialTheme.typography.labelSmall, color = VibeCyan, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Moments: multi-photo and MP4 posts with captions and engagement.
        item(key = "profile_moments") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var moments by remember(profile.userId) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
            var uploadingMoment by remember(profile.userId) { mutableStateOf(false) }
            var momentError by remember(profile.userId) { mutableStateOf("") }
            var pendingMomentUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
            var pendingIsVideo by remember { mutableStateOf(false) }
            var momentQuote by remember { mutableStateOf("") }
            var showMomentComposer by remember { mutableStateOf(false) }

            val photoPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
             ) { uri: Uri? ->
                if (uri != null && isOwnProfile) {
                    // Keep the Photo Picker read grant while the user writes the caption.
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val addingToExistingComposer = showMomentComposer
                    pendingMomentUris = (pendingMomentUris + uri).distinct().take(2)
                    pendingIsVideo = false
                    if (!addingToExistingComposer) momentQuote = ""
                    momentError = ""
                    showMomentComposer = true
                }
            }
            val videoPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null && isOwnProfile) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    pendingMomentUris = listOf(uri)
                    pendingIsVideo = true
                    momentQuote = ""
                    momentError = ""
                    showMomentComposer = true
                }
            }

            DisposableEffect(profile.userId) {
                val registration = FirebaseFirestore.getInstance().collection("users")
                    .document(profile.userId).collection("moments")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(30)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            momentError = error.localizedMessage ?: "Could not load Moments"
                        } else {
                            moments = snapshot?.documents.orEmpty()
                                .filter { doc ->
                                    val type = doc.getString("mediaType")
                                    val mime = doc.getString("contentType").orEmpty()
                                    (type == "image" && mime == "image/jpeg") ||
                                        (type == "video" && mime == "video/mp4")
                                }
                                .map { doc -> doc.data.orEmpty() + mapOf("id" to doc.id) }
                        }
                    }
                onDispose { registration.remove() }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Moments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("1 or 2 photos, or 1 MP4 video · 1 MB per file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isOwnProfile) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !uploadingMoment,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_profile_moment_photos")
                            ) { Text("Photos") }
                            OutlinedButton(
                                onClick = {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                enabled = !uploadingMoment,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_profile_moment_video")
                            ) { Text("Video") }
                        }
                    }
                }
                if (momentError.isNotBlank()) {
                    Text(momentError, color = VibeCoral, style = MaterialTheme.typography.labelSmall)
                }
                if (moments.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    ) {
                        Text(
                            if (isOwnProfile) "Share your first photo or MP4 video" else "No moments shared yet",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        moments.forEach { moment ->
                            val momentId = moment["id"] as? String ?: return@forEach
                            val urls = (moment["mediaUrls"] as? List<*>)?.filterIsInstance<String>()
                                ?.filter { it.isNotBlank() }
                                ?: listOfNotNull(moment["mediaUrl"] as? String)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val quote = moment["quote"] as? String ?: ""
                                    if (quote.isNotBlank()) {
                                        Text("“$quote”", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        urls.forEach { url ->
                                            val isVideo = moment["mediaType"] == "video"
                                            Box(
                                                modifier = Modifier.size(width = 142.dp, height = 164.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .clickable(enabled = url.isNotBlank()) {
                                                        val mediaUri = Uri.parse(url)
                                                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(mediaUri, if (isVideo) "video/mp4" else "image/jpeg")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        })
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isVideo) {
                                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play MP4 video", tint = VibePink, modifier = Modifier.size(48.dp))
                                                    Text("MP4 VIDEO", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                } else {
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = "Moment photo",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    MomentEngagement(ownerId = profile.userId, momentId = momentId)
                                }
                            }
                        }
                    }
                }
            }

            if (showMomentComposer) {
                AlertDialog(
                    onDismissRequest = {
                        if (!uploadingMoment) {
                            showMomentComposer = false
                            pendingMomentUris = emptyList()
                        }
                    },
                    title = { Text(if (pendingIsVideo) "Add MP4 Moment" else "Add Photos Moment") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (pendingIsVideo) "1 MP4 video selected · max 1 MB" else "${pendingMomentUris.size} photo(s) selected · max 1 MB each")
                            if (!pendingIsVideo && pendingMomentUris.size < 2) {
                                OutlinedButton(
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    enabled = !uploadingMoment,
                                    modifier = Modifier.fillMaxWidth().testTag("add_second_profile_moment_photo"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Text(" Add another photo")
                                }
                            }
                            if (!pendingIsVideo && pendingMomentUris.isNotEmpty()) {
                                Text(
                                    if (pendingMomentUris.size == 1) "1 photo selected · you can add one more" else "2 photos selected · maximum reached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = momentQuote,
                                onValueChange = { if (it.length <= 300) momentQuote = it },
                                label = { Text("Write a quote or caption") },
                                placeholder = { Text("Add something above your photos…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            Text("${momentQuote.length}/300", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (uploadingMoment) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            if (momentError.isNotBlank()) Text(momentError, color = VibeCoral, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !uploadingMoment && pendingMomentUris.isNotEmpty(),
                            onClick = {
                                uploadingMoment = true
                                momentError = ""
                                scope.launch {
                                    try {
                                        val mimeTypes = pendingMomentUris.map { uri ->
                                            context.contentResolver.getType(uri)?.lowercase().orEmpty()
                                        }
                                        if (pendingIsVideo && (mimeTypes.size != 1 || mimeTypes.first() != "video/mp4")) {
                                            throw IllegalArgumentException("Only MP4 videos are supported")
                                        }
                                        if (!pendingIsVideo && mimeTypes.any { !it.startsWith("image/") }) {
                                            throw IllegalArgumentException("Please select photos only")
                                        }

                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            ?: throw IllegalStateException("Please sign in again")
                                        val firestore = FirebaseFirestore.getInstance()
                                        val momentRef = firestore.collection("users").document(uid)
                                            .collection("moments").document()
                                        val uploadedUrls = mutableListOf<String>()
                                        val uploadedSizes = mutableListOf<Int>()

                                        pendingMomentUris.forEachIndexed { index, uri ->
                                            val isImage = !pendingIsVideo
                                            val bytes = if (isImage) {
                                                withContext(Dispatchers.IO) { compressMomentPhoto(context, uri) }
                                            } else null
                                            val sizeBytes = bytes?.size?.toLong()
                                                ?: withContext(Dispatchers.IO) { getMomentFileSize(context, uri) }
                                            if (sizeBytes <= 0L || sizeBytes > 1024L * 1024L) {
                                                throw IllegalArgumentException("Each photo/video must be between 1 byte and 1 MB")
                                            }
                                            val extension = if (isImage) "jpg" else "mp4"
                                            val mime = if (isImage) "image/jpeg" else "video/mp4"
                                            val mediaRef = FirebaseStorage.getInstance().reference
                                                .child("moments/$uid/${momentRef.id}_$index.$extension")
                                            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                                                .setContentType(mime)
                                                .build()
                                            val uploadTask = if (bytes != null) mediaRef.putBytes(bytes, metadata)
                                                else mediaRef.putFile(uri, metadata)
                                            val downloadTask = uploadTask.continueWithTask { task ->
                                                if (!task.isSuccessful) throw (task.exception ?: Exception("Storage upload failed"))
                                                mediaRef.downloadUrl
                                            }
                                            val downloadUri = awaitMomentTask(downloadTask)
                                            uploadedUrls.add(downloadUri.toString())
                                            uploadedSizes.add(sizeBytes.toInt())
                                        }

                                        val mediaType = if (pendingIsVideo) "video" else "image"
                                        val contentType = if (pendingIsVideo) "video/mp4" else "image/jpeg"
                                        val postData = mapOf(
                                            "ownerId" to uid,
                                            "mediaUrl" to uploadedUrls.first(),
                                            "mediaUrls" to uploadedUrls,
                                            "mediaSizes" to uploadedSizes,
                                            "mediaType" to mediaType,
                                            "contentType" to contentType,
                                            "sizeBytes" to uploadedSizes.sum(),
                                            "quote" to momentQuote.trim(),
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                        awaitMomentTask(momentRef.set(postData))
                                        uploadingMoment = false
                                        showMomentComposer = false
                                        pendingMomentUris = emptyList()
                                        momentQuote = ""
                                    } catch (error: Exception) {
                                        uploadingMoment = false
                                        momentError = error.localizedMessage ?: "Moment upload failed. Please try again."
                                    }
                                }
                            }
                        ) { Text(if (uploadingMoment) "Uploading…" else "Share Moment") }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !uploadingMoment,
                            onClick = {
                                showMomentComposer = false
                                pendingMomentUris = emptyList()
                            }
                        ) { Text("Cancel") }
                    }
                )
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
                        .testTag("profile_wallet_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        VibeCoinIcon(modifier = Modifier.size(32.dp))
                        Text("${profile.coinBalance}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibeGold)
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

    if (showLevelInfo) {
        val currentLevel = LevelProgression.levelForXp(profile.xp)
        val xpProgress = LevelProgression.progressInLevel(profile.xp)
        val xpForNext = xpProgress.second
        val remainingXp = (xpForNext - xpProgress.first).coerceAtLeast(0L)
        val giftCoinsNeeded = remainingXp * 5L
        AlertDialog(
            onDismissRequest = { showLevelInfo = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("LEVEL PROGRESSION", color = Color(0xFF9B78FF), fontWeight = FontWeight.ExtraBold)
                    Text("Level $currentLevel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total XP: ${profile.xp}")
                    Text("This level: ${xpProgress.first} / $xpForNext XP")
                    LinearProgressIndicator(
                        progress = { (xpProgress.first.toFloat() / xpForNext.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                        color = Color(0xFF9B78FF),
                        trackColor = Color(0xFFE9E1FF)
                    )
                    Text("How XP is earned", fontWeight = FontWeight.Bold, color = Color(0xFF8C63E8))
                    Text("• Send a gift: every 5 gift coins spent = 1 XP")
                    Text("• Receive a gift: every 5 gift coins received = 1 XP")
                    Text("• Daily reward: +50 XP")
                    Text("Gift XP uses whole-number calculation per transaction (gift coin value ÷ 5).")
                    Text("Next level calculation", fontWeight = FontWeight.Bold, color = Color(0xFF8C63E8))
                    Text("You need $remainingXp more XP.")
                    Text("At 5 gift coins per XP, that is $giftCoinsNeeded gift coins sent or received.")
                    Text("Level $currentLevel → ${currentLevel + 1}: $xpForNext XP = ${xpForNext * 5L} gift coins.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelInfo = false }) { Text("Got it") }
            }
        )
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

    if (showSocialSheet && isOwnProfile) {
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

    selectedGiftPrice?.let { gift ->
        AlertDialog(
            onDismissRequest = { selectedGiftPrice = null },
            title = { Text(gift.name) },
            text = { Text("${gift.coinCost} coins") },
            confirmButton = {
                TextButton(onClick = { selectedGiftPrice = null }) { Text("OK") }
            }
        )
    }
}


private fun openMomentUriStream(context: android.content.Context, uri: Uri): InputStream? {
    val resolver = context.contentResolver

    // Providers may expose picker content through different descriptor APIs.
    runCatching { resolver.openInputStream(uri) }.getOrNull()?.let { return it }
    runCatching { resolver.openAssetFileDescriptor(uri, "r")?.createInputStream() }
        .getOrNull()?.let { return it }
    runCatching { resolver.openTypedAssetFileDescriptor(uri, "image/*", null)?.createInputStream() }
        .getOrNull()?.let { return it }
    return runCatching {
        resolver.openFileDescriptor(uri, "r")?.let { ParcelFileDescriptor.AutoCloseInputStream(it) }
    }.getOrNull()
}

private fun getMomentFileSize(context: android.content.Context, uri: Uri): Long {
    val descriptorSize = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
    }.getOrNull() ?: -1L
    if (descriptorSize > 0L) return descriptorSize

    var totalBytes = 0L
    openMomentUriStream(context, uri)?.use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            totalBytes += count
            if (totalBytes > 1024L * 1024L) return totalBytes
        }
    } ?: throw IllegalArgumentException("Could not open selected video. Please choose another file.")
    return totalBytes
}

private fun compressMomentPhoto(context: android.content.Context, uri: Uri): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openMomentUriStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IllegalArgumentException("Could not open selected photo. Please choose another photo.")
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IllegalArgumentException("Unsupported or unreadable photo format. Please choose a JPG or PNG photo.")
    }

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    var bitmap = openMomentUriStream(context, uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: throw IllegalArgumentException("Could not decode selected photo. Please choose another photo.")
    try {
        while (true) {
            for (quality in listOf(90, 85, 75, 65, 55, 45, 35, 25, 15)) {
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                val bytes = output.toByteArray()
                if (bytes.size <= 1024 * 1024) return bytes
            }
            val nextWidth = (bitmap.width * 0.8f).toInt().coerceAtLeast(1)
            val nextHeight = (bitmap.height * 0.8f).toInt().coerceAtLeast(1)
            if (nextWidth == bitmap.width || nextHeight == bitmap.height) {
                throw IllegalStateException("Maximum upload size is 1 MB")
            }
            val smaller = Bitmap.createScaledBitmap(bitmap, nextWidth, nextHeight, true)
            if (smaller !== bitmap) bitmap.recycle()
            bitmap = smaller
        }
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
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


@Composable
private fun MomentEngagement(ownerId: String, momentId: String) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseFirestore.getInstance()
    val momentRef = remember(ownerId, momentId) {
        db.collection("users").document(ownerId).collection("moments").document(momentId)
    }
    var liked by remember(uid, momentId) { mutableStateOf(false) }
    var likeCount by remember(momentId) { mutableStateOf(0) }
    var comments by remember(momentId) { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var commentText by remember(momentId) { mutableStateOf("") }
    var sending by remember(momentId) { mutableStateOf(false) }

    DisposableEffect(uid, ownerId, momentId) {
        val likesListener = momentRef.collection("likes")
            .addSnapshotListener { snapshot, _ ->
                likeCount = snapshot?.size() ?: 0
                liked = uid.isNotBlank() && snapshot?.documents?.any { it.id == uid } == true
            }
        val commentsListener = momentRef.collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, _ ->
                comments = snapshot?.documents.orEmpty()
            }
        onDispose {
            likesListener.remove()
            commentsListener.remove()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (liked) "Unlike moment" else "Like moment",
                tint = if (liked) VibePink else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp).clickable(enabled = uid.isNotBlank()) {
                    val likeRef = momentRef.collection("likes").document(uid)
                    if (liked) likeRef.delete()
                    else likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp()))
                }
            )
            Text(" $likeCount", style = MaterialTheme.typography.labelMedium)
        }
        Text("💬 ${comments.size} comments", style = MaterialTheme.typography.labelMedium)
    }

    if (comments.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            comments.forEach { comment ->
                MomentCommentItem(
                    momentRef = momentRef,
                    comment = comment,
                    currentUserId = uid
                )
            }
        }
    }

    if (uid.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { if (it.length <= 500) commentText = it },
                placeholder = { Text("Write a comment…") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                singleLine = false
            )
            Icon(
                Icons.Filled.Send,
                contentDescription = "Post comment",
                tint = VibeCyan,
                modifier = Modifier.size(25.dp).clickable(enabled = commentText.isNotBlank() && !sending) {
                    sending = true
                    val value = commentText.trim()
                    val data = mapOf(
                        "authorId" to uid,
                        "text" to value,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    momentRef.collection("comments").add(data)
                        .addOnSuccessListener {
                            commentText = ""
                            sending = false
                        }
                        .addOnFailureListener { sending = false }
                }
            )
        }
    }
}

@Composable
private fun MomentCommentItem(
    momentRef: com.google.firebase.firestore.DocumentReference,
    comment: DocumentSnapshot,
    currentUserId: String
) {
    val commentId = comment.id
    val commentRef = remember(momentRef.path, commentId) {
        momentRef.collection("comments").document(commentId)
    }
    var liked by remember(commentId, currentUserId) { mutableStateOf(false) }
    var count by remember(commentId) { mutableStateOf(0) }
    DisposableEffect(commentId, currentUserId) {
        val listener = commentRef.collection("likes").addSnapshotListener { snapshot, _ ->
            count = snapshot?.size() ?: 0
            liked = currentUserId.isNotBlank() && snapshot?.documents?.any { it.id == currentUserId } == true
        }
        onDispose { listener.remove() }
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            RoundedCornerShape(10.dp)
        ).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(comment.getString("text").orEmpty(), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Like comment",
                tint = if (liked) VibePink else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp).clickable(enabled = currentUserId.isNotBlank()) {
                    val likeRef = commentRef.collection("likes").document(currentUserId)
                    if (liked) likeRef.delete()
                    else likeRef.set(mapOf("userId" to currentUserId, "createdAt" to FieldValue.serverTimestamp()))
                }
            )
            Text(" $count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private suspend fun <T> awaitMomentTask(task: Task<T>): T =
    suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }.addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }.addOnCanceledListener {
            continuation.cancel()
        }
    }
