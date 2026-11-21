package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Base64
import com.example.BuildConfig
import com.example.R
import com.example.data.model.GiftBurstEvent
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibeMaleBlue
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet
import com.example.ui.theme.VibeVioletBright
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@DrawableRes
fun coverDrawableRes(coverKey: String): Int = when (coverKey) {
    "chill_lounge" -> R.drawable.img_cover_chill_lounge
    "music_arena" -> R.drawable.img_cover_music_arena
    "vip_crown" -> R.drawable.img_cover_vip_crown
    else -> R.drawable.img_hero_party_stage
}

@Composable
fun VibeAvatar(
    avatarUrl: String,
    username: String,
    size: Dp = 48.dp,
    vipLevel: Long = 0L,
    online: Boolean = false,
    speaking: Boolean = false,
    modifier: Modifier = Modifier,
    frameColorArgb: Long? = null
) {
    val firestoreAvatar = avatarUrl.startsWith("firestore-avatar://")
    val firestoreAvatarUid = if (firestoreAvatar) avatarUrl.removePrefix("firestore-avatar://") else ""
    var firestoreAvatarBytes by remember(avatarUrl) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarUrl) {
        firestoreAvatarBytes = null
        if (firestoreAvatar && firestoreAvatarUid.isNotBlank()) {
            firestoreAvatarBytes = runCatching {
                val snapshot = FirebaseFirestore.getInstance().collection("users").document(firestoreAvatarUid).collection("avatar").document("image").get().await()
                val encoded = snapshot.getString("data") ?: return@runCatching null
                Base64.decode(encoded, Base64.DEFAULT)
            }.getOrNull()
        }
    }
    val vipColor = remember(vipLevel) {
        val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == vipLevel.toInt() }
        if (tier != null) Color(tier.badgeColorHex) else null
    }
    val infinite = rememberInfiniteTransition(label = "speaking_pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Equipped frames animate with a soft shimmer on every avatar instance
    // (profile, room seats, chat and other places using VibeAvatar).
    val borderBrush = when {
        frameColorArgb != null -> Brush.linearGradient(
            listOf(
                Color(frameColorArgb).copy(alpha = pulseAlpha),
                Color(frameColorArgb).copy(alpha = 0.38f + (pulseAlpha * 0.42f)),
                Color.White.copy(alpha = pulseAlpha)
            )
        )
        speaking -> Brush.linearGradient(listOf(VibeCyan, VibePink.copy(alpha = pulseAlpha)))
        vipColor != null -> Brush.linearGradient(listOf(vipColor, VibeGold))
        else -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = if (frameColorArgb != null || speaking || vipLevel > 0) 3.5.dp else 1.5.dp,
                    brush = borderBrush,
                    shape = CircleShape
                )
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            VibeViolet.copy(alpha = 0.85f),
                            VibeCyan.copy(alpha = 0.75f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val avatarImageModel: Any? = if (firestoreAvatar) firestoreAvatarBytes else avatarUrl.takeIf { it.isNotBlank() }
            if (avatarImageModel != null) {
                AsyncImage(
                    model = avatarImageModel,
                    contentDescription = "$username avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = username.trim().take(1).uppercase().ifBlank { "V" },
                    style = if (size < 36.dp) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(VibeEmerald)
            )
        }
    }
}

@Composable
fun VipBadgePill(vipLevel: Long, modifier: Modifier = Modifier) {
    if (vipLevel <= 0L) return
    val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == vipLevel.toInt() }
    val accent = tier?.let { Color(it.badgeColorHex) } ?: VibeGold
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "VIP $vipLevel",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LevelBadge(level: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = VibeVioletBright.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, VibeVioletBright.copy(alpha = 0.55f))
    ) {
        Text(
            text = "Lv.$level",
            style = MaterialTheme.typography.labelSmall,
            color = VibeVioletBright,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun BiologicalGenderBadge(
    gender: String,
    showGender: Boolean = true,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!showGender) return
    val normalized = gender.trim().uppercase()
    val symbol: String
    val tint: Color
    val desc: String
    when (normalized) {
        "MALE" -> {
            symbol = "\u2642\uFE0F"
            tint = VibeMaleBlue
            desc = "Male"
        }
        "FEMALE" -> {
            symbol = "\u2640\uFE0F"
            tint = VibePink
            desc = "Female"
        }
        else -> return
    }
    if (compact) {
        Text(
            text = symbol,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier.testTag("gender_badge_${normalized.lowercase()}")
        )
    } else {
        Surface(
            modifier = modifier.testTag("gender_badge_${normalized.lowercase()}"),
            shape = RoundedCornerShape(50),
            color = tint.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, tint.copy(alpha = 0.6f))
        ) {
            Text(
                text = "$symbol $desc",
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun VoiceRoomCard(
    room: VoiceRoom,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("room_card_${room.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (room.featured) VibeGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                Image(
                    painter = painterResource(id = coverDrawableRes(room.coverKey)),
                    contentDescription = "${room.title} room cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                // Top Row: Category + Lock/Featured Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VibeViolet.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = VibeCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = room.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (room.featured) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = VibeGold.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = "HOT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (room.hasPassword) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Password,
                                    contentDescription = "Password protected",
                                    tint = VibeGold,
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(14.dp)
                                )
                            }
                        } else if (room.locked) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Room locked",
                                    tint = VibePink,
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Overlay: Title
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = room.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (room.announcement.isNotBlank()) {
                        Text(
                            text = room.announcement,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    VibeAvatar(
                        avatarUrl = room.hostAvatarUrl,
                        username = room.hostName,
                        size = 28.dp
                    )
                    Text(
                        text = room.hostName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = "Listeners",
                            tint = VibeCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${room.listenerCount + room.activeSpeakersCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GiftBurstOverlay(
    event: GiftBurstEvent?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(event?.id) {
        if (event != null) {
            delay(2800L)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn() + slideInVertically { -it / 2 } + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        event?.let { burst ->
            val infinite = rememberInfiniteTransition(label = "gift_float")
            val floatY by infinite.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "gift_float_y"
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(VibeGold, VibePink, VibeCyan)))
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    VibeViolet.copy(alpha = 0.94f),
                                    VibePink.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = burst.giftEmoji,
                        fontSize = 32.sp,
                        modifier = Modifier.offset(y = floatY.dp)
                    )
                    Column {
                        Text(
                            text = "${burst.senderName} sent ${burst.giftName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "To ${burst.recipientName} · ${burst.coinCost} Coins",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VipEntranceOverlay(
    user: UserProfile?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(user?.userId, user?.vipLevel) {
        if (user != null) {
            delay(3000L)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = user != null && user.vipLevel > 0L,
        enter = fadeIn() + slideInVertically { -it } + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier.testTag("vip_entrance_overlay")
    ) {
        user?.let { vip ->
            val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == vip.vipLevel.toInt() }
            val accent = tier?.let { Color(it.badgeColorHex) } ?: VibeGold
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(accent, VibeGold)))
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.88f),
                                    accent.copy(alpha = 0.42f),
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VibeAvatar(
                        avatarUrl = vip.avatarUrl,
                        username = vip.username,
                        size = 34.dp,
                        vipLevel = vip.vipLevel
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VipBadgePill(vip.vipLevel)
                            Text(
                                text = "${vip.username} entered the stage!",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        tier?.let {
                            Text(
                                text = it.entranceEffect,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
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
fun BackendIntegrationGuideSheet(onDismiss: () -> Unit) {
    val livekitUrlConfigured = BuildConfig.LIVEKIT_URL.trim().let {
        it.isNotBlank() && !it.equals("UNCONFIGURED", ignoreCase = true) &&
            (it.startsWith("wss://") || it.startsWith("ws://"))
    }
    val livekitEndpointConfigured = BuildConfig.LIVEKIT_TOKEN_ENDPOINT.trim().let {
        it.isNotBlank() && !it.equals("UNCONFIGURED", ignoreCase = true) &&
            (it.startsWith("https://") || it.startsWith("http://"))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudQueue,
                    contentDescription = null,
                    tint = VibeCyan,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Backend & LiveKit Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            StatusIntegrationRow(
                title = "Firebase Auth (Google Sign-In)",
                active = true,
                detail = "Connected via Jetpack Credential Manager & google-services.json."
            )
            StatusIntegrationRow(
                title = "Cloud Firestore Database",
                active = true,
                detail = "Rooms, 9-seat stages, chat, gifts, inbox & notifications sync live."
            )
            StatusIntegrationRow(
                title = "LiveKit Voice Server (Optional)",
                active = livekitUrlConfigured,
                detail = if (livekitUrlConfigured) "Configured in BuildConfig" else "Voice service not configured yet. Optional during preview; set LIVEKIT_URL in AI Studio Secrets when ready."
            )
            StatusIntegrationRow(
                title = "LiveKit Token Backend (Optional)",
                active = livekitEndpointConfigured,
                detail = if (livekitEndpointConfigured) "Configured in BuildConfig" else "Optional during preview; set LIVEKIT_TOKEN_ENDPOINT in AI Studio Secrets when deploying your token server."
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Optional Firebase Console Services",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Email/Password & Phone OTP Auth: Enable in Firebase Console → Authentication → Sign-in method.\n" +
                            "• Firebase Storage: Enable Storage bucket in Firebase Console to upload avatar/chat photos across devices.\n" +
                            "• Firebase Cloud Messaging (FCM): Register FCM service for background push notifications.\n" +
                            "• Server-Side Wallet Validation: Add Cloud Functions to enforce atomic server coin minting & IAP receipts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Got it")
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun StatusIntegrationRow(title: String, active: Boolean, detail: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (active) VibeEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (active) VibeEmerald else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReportUserOrRoomDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reasons = listOf("Harassment or Abuse", "Spam or Scam", "Inappropriate Audio/Content", "Impersonation")
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select a reason for community moderation review:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reasons.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(400) },
                    label = { Text("Additional context (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
