package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import android.view.View
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.decode.GifDecoder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import com.example.BuildConfig
import com.example.R
import com.example.data.model.GiftBurstEvent
import com.example.data.model.GiftCatalogItem
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Shared VibePlay coin artwork. The single and pile variants use the same
 * glossy gold face, raised rim and curved C emblem.
 */
@Composable
fun VibeCoinIcon(modifier: Modifier = Modifier, pile: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val goldFace = Brush.linearGradient(
            listOf(Color(0xFFFFF18A), Color(0xFFFFC52E), Color(0xFFFF9418), Color(0xFFFFD84A)),
            start = Offset(0f, 0f), end = Offset(w, h)
        )
        val rim = Brush.linearGradient(
            listOf(Color(0xFFFFF4A8), Color(0xFFE58A08), Color(0xFFFFD34A), Color(0xFFB95A05)),
            start = Offset(0f, 0f), end = Offset(w, h)
        )
        fun coin(cx: Float, cy: Float, rx: Float, ry: Float) {
            drawOval(rim, Offset(cx-rx, cy-ry), androidx.compose.ui.geometry.Size(rx*2f, ry*2f))
            drawOval(Color(0xFF9E4B05).copy(alpha=0.32f), Offset(cx-rx*0.84f,cy-ry*0.78f), androidx.compose.ui.geometry.Size(rx*1.68f,ry*1.58f))
            drawOval(goldFace, Offset(cx-rx*0.84f,cy-ry*0.88f), androidx.compose.ui.geometry.Size(rx*1.68f,ry*1.68f))
            drawOval(Color.White.copy(alpha=0.48f), Offset(cx-rx*0.62f,cy-ry*0.77f), androidx.compose.ui.geometry.Size(rx*0.78f,ry*0.28f))
            drawArc(
                color = Color(0xFFB96808), startAngle = 48f, sweepAngle = 264f,
                useCenter = false, topLeft = Offset(cx-rx*0.34f,cy-ry*0.40f),
                size = androidx.compose.ui.geometry.Size(rx*0.68f,ry*0.80f),
                style = Stroke(width = minOf(rx,ry)*0.22f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFFFF7BD), startAngle = 48f, sweepAngle = 264f,
                useCenter = false, topLeft = Offset(cx-rx*0.34f,cy-ry*0.40f),
                size = androidx.compose.ui.geometry.Size(rx*0.68f,ry*0.80f),
                style = Stroke(width = minOf(rx,ry)*0.10f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        if (pile) {
            coin(w*0.34f,h*0.39f,w*0.27f,h*0.22f)
            coin(w*0.67f,h*0.39f,w*0.27f,h*0.22f)
            coin(w*0.50f,h*0.66f,w*0.43f,h*0.30f)
        } else {
            coin(w*0.5f,h*0.5f,w*0.46f,h*0.46f)
        }
    }
}

@DrawableRes
fun coverDrawableRes(coverKey: String): Int = when (coverKey) {
    "chill_lounge" -> R.drawable.img_cover_chill_lounge
    "music_arena" -> R.drawable.img_cover_music_arena
    "vip_crown" -> R.drawable.img_cover_vip_crown
    else -> R.drawable.img_hero_party_stage
}

private object VibeAvatarMemoryCache {
    private const val MAX_ENTRIES = 40
    private val entries = object : LinkedHashMap<String, ByteArray>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized
    fun get(key: String): ByteArray? = entries[key]

    @Synchronized
    fun put(key: String, value: ByteArray) {
        entries[key] = value
    }

    private fun diskFile(context: Context, key: String): java.io.File {
        val safeKey = Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return java.io.File(java.io.File(context.filesDir, "avatar_cache"), "$safeKey.img")
    }

    suspend fun getOrReadDisk(context: Context, key: String): ByteArray? {
        get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                diskFile(context, key).takeIf { it.isFile && it.length() > 0L }?.readBytes()
            }.getOrNull()?.also { put(key, it) }
        }
    }

    suspend fun putPersistent(context: Context, key: String, value: ByteArray) {
        put(key, value)
        withContext(Dispatchers.IO) {
            runCatching {
                val file = diskFile(context, key)
                file.parentFile?.mkdirs()
                val temp = java.io.File(file.parentFile, file.name + ".tmp")
                temp.writeBytes(value)
                if (temp.length() == value.size.toLong()) {
                    if (file.exists()) file.delete()
                    temp.renameTo(file)
                } else temp.delete()
            }
        }
    }
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
    val firestoreAvatarUid = if (firestoreAvatar) avatarUrl.removePrefix("firestore-avatar://").substringBefore("?") else ""
    var firestoreAvatarBytes by remember(avatarUrl) { mutableStateOf<ByteArray?>(null) }
    var avatarImageLoaded by remember(avatarUrl) { mutableStateOf(false) }
    var avatarLoadFailed by remember(avatarUrl) { mutableStateOf(false) }
    var avatarRetryCount by remember(avatarUrl) { mutableStateOf(0) }
    val avatarContext = LocalContext.current

    LaunchedEffect(avatarUrl) {
        if (firestoreAvatar && firestoreAvatarUid.isNotBlank()) {
            // Reuse the same decoded photo across profile, room, chat and seat avatars.
            // Reuse a persisted copy before requesting Firestore again.
            val cached = VibeAvatarMemoryCache.getOrReadDisk(avatarContext, avatarUrl)
            if (cached != null) {
                firestoreAvatarBytes = cached
                return@LaunchedEffect
            }
            // Retry briefly on transient network failures; never block the UI thread decoding bytes.
            repeat(3) { attempt ->
                val loaded = runCatching {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users").document(firestoreAvatarUid)
                        .collection("avatar").document("image")
                        .get().await()
                    val encoded = snapshot.getString("data")
                    if (encoded.isNullOrBlank()) null else withContext(Dispatchers.IO) {
                        Base64.decode(encoded, Base64.DEFAULT)
                    }
                }.getOrNull()
                if (loaded != null && loaded.isNotEmpty()) {
                    VibeAvatarMemoryCache.putPersistent(avatarContext, avatarUrl, loaded)
                    firestoreAvatarBytes = loaded
                    return@LaunchedEffect
                }
                if (attempt < 2) delay(500L * (attempt + 1))
            }
        }
    }

    // Remote avatar URLs occasionally fail during a short network interruption.
    // Retry twice, while always showing the user's initial instead of an empty circle.
    LaunchedEffect(avatarUrl, avatarLoadFailed) {
        if (avatarLoadFailed && !firestoreAvatar && avatarRetryCount < 2) {
            delay(700L * (avatarRetryCount + 1))
            avatarRetryCount += 1
            avatarLoadFailed = false
        }
    }
    val displayVipLevel = vipLevel.coerceIn(0L, 8L)
    val isNeonCyanFrame = frameColorArgb == 0xFF22D3EEL
    val isAuroraFrame = frameColorArgb == 0xFF34D399L
    val isRoseGoldFrame = frameColorArgb == 0xFFF472B6L
    val isRoyalGoldFrame = frameColorArgb == 0xFFFBBF24L
    val isGalaxyFrame = frameColorArgb == 0xFFA78BFAL
    val luxuryVipLevel = when (frameColorArgb) {
        0xFFFF5A36L -> 1
        0xFFE83252L -> 2
        0xFF9B5CFFL -> 3
        0xFF398BFFL -> 4
        0xFF20C997L -> 5
        0xFFD18BFFL -> 6
        0xFF42D9E8L -> 7
        0xFFFFC247L -> 8
        else -> null
    }
    val isLuxuryVipFrame = luxuryVipLevel != null
    val bannerVipLevel = when (frameColorArgb) {
        0xFFD4AF37L -> 1
        0xFFC9A227L -> 2
        0xFFE0B84FL -> 3
        0xFFB8860BL -> 4
        0xFFF2C14EL -> 5
        0xFFAA771CL -> 6
        0xFFE6BE65L -> 7
        0xFF967117L -> 8
        else -> null
    }
    val dragonVipLevel = when (frameColorArgb) {
        0xFFB87333L -> 1
        0xFF8C5A2BL -> 2
        0xFF6F42C1L -> 3
        0xFF2563EBL -> 4
        0xFF0F9D78L -> 5
        0xFF9333EAL -> 6
        0xFF0891B2L -> 7
        0xFFCA8A04L -> 8
        else -> null
    }
    val vipColor = remember(displayVipLevel) {
        val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == displayVipLevel.toInt() }
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
        modifier = modifier
            .size(size)
            .drawBehind {
                if (frameColorArgb != null && !isNeonCyanFrame && !isLuxuryVipFrame) {
                    val frameColor = Color(frameColorArgb)
                    val centerX = this.size.width / 2f
                    val centerY = this.size.height / 2f
                    val radius = this.size.minDimension / 2f - 2.dp.toPx()
                    // Premium double halo and four animated jewel glints.
                    drawCircle(
                        color = frameColor.copy(alpha = 0.72f * pulseAlpha),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.62f * pulseAlpha),
                        radius = radius - 3.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                    for (angle in listOf(-135.0, -45.0, 45.0, 135.0)) {
                        val radians = Math.toRadians(angle)
                        val x = centerX + cos(radians).toFloat() * radius
                        val y = centerY + sin(radians).toFloat() * radius
                        drawCircle(
                            color = Color.White.copy(alpha = pulseAlpha),
                            radius = 2.1.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = if (isNeonCyanFrame || isLuxuryVipFrame) 0.dp else if (frameColorArgb != null || speaking || vipLevel > 0) 3.5.dp else 1.5.dp,
                    brush = borderBrush,
                    shape = CircleShape
                )
                .padding(if (isNeonCyanFrame) 0.dp else 2.5.dp)
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
            val avatarImageModel: Any? = if (firestoreAvatar) {
                firestoreAvatarBytes
            } else {
                avatarUrl.takeIf { it.isNotBlank() }?.let { url ->
                    ImageRequest.Builder(avatarContext)
                        .data(url)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
            }
            if (avatarImageModel != null) {
                AsyncImage(
                    model = avatarImageModel,
                    contentDescription = "$username avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onSuccess = { avatarImageLoaded = true; avatarLoadFailed = false },
                    onError = { avatarImageLoaded = false; avatarLoadFailed = true }
                )
            }
            // Keep a reliable visible fallback during loading and after any image error.
            if (!avatarImageLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(VibeViolet, VibeCyan)),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.trim().take(1).uppercase().ifBlank { "V" },
                        style = if (size < 36.dp) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (isNeonCyanFrame) {
            NeonAuroraFrameOverlay(
                modifier = Modifier.align(Alignment.Center).requiredSize(size * 1.55f),
                pulseAlpha = pulseAlpha
            )
        }
        if (isAuroraFrame) {
            AuroraPremiumFrameOverlay(
                modifier = Modifier.align(Alignment.Center).requiredSize(size * 1.55f),
                pulseAlpha = pulseAlpha
            )
        }
        if (isGalaxyFrame) {
            GalaxyFrameOverlay(Modifier.align(Alignment.Center).requiredSize(size * 1.55f), pulseAlpha)
        }
        if (isRoyalGoldFrame) {
            RoyalGoldFrameOverlay(Modifier.align(Alignment.Center).requiredSize(size * 1.55f), pulseAlpha)
        }
        if (isRoseGoldFrame) {
            RoseGoldFrameOverlay(Modifier.align(Alignment.Center).requiredSize(size * 1.55f), pulseAlpha)
        }
        if (dragonVipLevel != null) {
            VipDragonFrameOverlay(
                vipLevel = dragonVipLevel,
                modifier = Modifier.align(Alignment.Center).requiredSize(size * 1.72f),
                pulseAlpha = pulseAlpha
            )
        }
        if (bannerVipLevel != null) {
            LuxuryVipFrameOverlay(
                vipLevel = bannerVipLevel,
                avatarSize = size,
                modifier = Modifier.align(Alignment.Center).requiredSize(size * 1.68f),
                pulseAlpha = pulseAlpha
            )
        }
        if (luxuryVipLevel != null) {
            LuxuryVipFrameOverlay(
                vipLevel = luxuryVipLevel,
                avatarSize = size,
                modifier = Modifier.align(Alignment.Center).requiredSize(size * 1.68f),
                pulseAlpha = pulseAlpha
            )
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
private fun LuxuryVipFrameOverlay(
    vipLevel: Int,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    pulseAlpha: Float
) {
    // A continuous travelling highlight gives the gold artwork visible motion
    // while keeping the original frame illustration and transparent centre intact.
    val motion = rememberInfiniteTransition(label = "luxury_gold_frame_motion_$vipLevel")
    val sweepAngle by motion.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "luxury_gold_frame_sweep"
    )
    val shimmerPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing)),
        label = "luxury_gold_frame_shimmer"
    )

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Image(
            painter = painterResource(R.drawable.vip_luxury_gold_frame),
            contentDescription = "Animated luxury gold VIP $vipLevel frame",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.49f
            val radius = size.minDimension * 0.315f
            val glowAlpha = (0.35f + 0.45f * (0.5f + 0.5f * kotlin.math.sin(shimmerPhase))) * pulseAlpha

            // A travelling gold-white arc moves around the frame perimeter.
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFFFFB52E).copy(alpha = glowAlpha),
                        Color(0xFFFFF4C2).copy(alpha = 0.95f),
                        Color(0xFFFFB52E).copy(alpha = glowAlpha),
                        Color.Transparent
                    )
                ),
                startAngle = sweepAngle,
                sweepAngle = 64f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = 3.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Four moving sparkle points follow the rotating highlight.
            for (index in 0..3) {
                val angle = Math.toRadians((sweepAngle + index * 90f).toDouble())
                val point = Offset(
                    cx + kotlin.math.cos(angle).toFloat() * radius,
                    cy + kotlin.math.sin(angle).toFloat() * radius
                )
                val sparkle = 0.55f + 0.45f * kotlin.math.sin(shimmerPhase + index).toFloat()
                drawCircle(
                    Color(0xFFFFF4C2).copy(alpha = sparkle.coerceIn(0.15f, 1f)),
                    (1.5f + sparkle * 1.4f).dp.toPx(),
                    point
                )
            }
        }

        // VIP number remains the same; only the frame's light effects move.
        Text(
            text = "VIP $vipLevel",
            color = Color(0xFFFFE6A0),
            fontSize = (avatarSize.value * 0.18f).coerceIn(8f, 17f).sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(avatarSize * 0.31f))
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0x99FFB52E).copy(alpha = pulseAlpha), Color.Transparent)
                        )
                    )
                }
        )
    }
}


@Composable
private fun VipDragonFrameOverlay(
    vipLevel: Int,
    modifier: Modifier = Modifier,
    pulseAlpha: Float
) {
    val palette = listOf(
        Color(0xFFB87333), Color(0xFF8C5A2B), Color(0xFF6F42C1), Color(0xFF2563EB),
        Color(0xFF0F9D78), Color(0xFF9333EA), Color(0xFF0891B2), Color(0xFFCA8A04)
    )
    val accent = palette[(vipLevel - 1).coerceIn(0, 7)]
    val motion = rememberInfiniteTransition(label = "vip_dragon_motion_$vipLevel")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "vip_flame_phase"
    )
    val sweepAngle by motion.animateFloat(
        initialValue = -45f,
        targetValue = 315f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "vip_fire_sweep"
    )
    val gemPulse by motion.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(780, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "vip_crown_gem_pulse"
    )
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.49f
            val r = size.minDimension * 0.285f
            val wave = kotlin.math.sin(phase)
            val glow = accent.copy(alpha = 0.30f + pulseAlpha * 0.48f)
            val hot = Color(0xFFFFF2C4).copy(alpha = 0.45f + gemPulse * 0.5f)

            // Multi-layer transparent ring: the avatar photo remains visible through the centre.
            drawCircle(glow, r + 3.dp.toPx(), Offset(cx, cy), style = Stroke(4.dp.toPx()))
            drawCircle(accent.copy(alpha = 0.9f), r, Offset(cx, cy), style = Stroke(1.8.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.42f * pulseAlpha), r - 4.dp.toPx(), Offset(cx, cy), style = Stroke(0.9.dp.toPx()))

            // Mirrored dragon bodies with animated flame whiskers and breathing glow.
            for (side in listOf(-1f, 1f)) {
                val breathe = wave * r * 0.035f
                val dragon = Path().apply {
                    moveTo(cx + side * r * 0.98f, cy - r * 0.55f)
                    cubicTo(
                        cx + side * r * (1.60f + wave * 0.035f), cy - r * 1.24f - breathe,
                        cx + side * r * (1.82f + wave * 0.025f), cy - r * 0.10f,
                        cx + side * r * 1.12f, cy + r * 0.12f
                    )
                    cubicTo(
                        cx + side * r * (1.58f - wave * 0.035f), cy + r * 0.62f,
                        cx + side * r * 1.08f, cy + r * 1.08f + breathe,
                        cx + side * r * 0.78f, cy + r * 0.72f
                    )
                }
                drawPath(dragon, glow, style = Stroke(7.dp.toPx()))
                drawPath(dragon, accent, style = Stroke(2.4.dp.toPx()))
                drawPath(dragon, hot.copy(alpha = 0.5f), style = Stroke(0.9.dp.toPx()))

                // Three moving flame tongues along each dragon's outer edge.
                for (i in 0..2) {
                    val t = phase + i * 1.7f + if (side < 0f) 0.8f else 0f
                    val baseX = cx + side * r * (1.18f + i * 0.12f)
                    val baseY = cy - r * 0.48f + i * r * 0.48f
                    val flick = kotlin.math.sin(t) * r * 0.12f
                    val flame = Path().apply {
                        moveTo(baseX, baseY + r * 0.16f)
                        cubicTo(
                            baseX + side * (r * 0.22f + flick), baseY - r * 0.02f,
                            baseX + side * (r * 0.10f - flick), baseY - r * 0.28f,
                            baseX + side * flick, baseY - r * (0.48f + 0.05f * kotlin.math.cos(t))
                        )
                    }
                    drawPath(flame, accent.copy(alpha = 0.72f), style = Stroke(2.4.dp.toPx()))
                    drawPath(flame, hot.copy(alpha = 0.7f), style = Stroke(0.8.dp.toPx()))
                }

                // Scales, eye glint, and three lower claws.
                for (i in 0..3) {
                    val y = cy - r * 0.34f + i * r * 0.31f
                    drawLine(accent.copy(alpha = 0.9f), Offset(cx + side * r * 1.04f, y), Offset(cx + side * r * 1.34f, y + r * 0.13f), 2.dp.toPx())
                }
                drawCircle(hot, 2.1.dp.toPx(), Offset(cx + side * r * 1.11f, cy - r * 0.48f))
                for (i in 0..2) {
                    val x = cx + side * (r * (0.82f + i * 0.13f))
                    val y = cy + r * 1.00f
                    val claw = Path().apply {
                        moveTo(x, y)
                        quadraticTo(x + side * r * 0.13f, y + r * 0.04f, x + side * r * (0.15f + i * 0.015f), y + r * (0.22f + wave * 0.025f))
                    }
                    drawPath(claw, accent, style = Stroke(2.2.dp.toPx()))
                }
            }

            // Crown with a pulsing centre jewel and animated glint.
            val crown = Path().apply {
                moveTo(cx - r * 0.55f, cy - r * 0.92f)
                lineTo(cx - r * 0.62f, cy - r * 1.38f)
                lineTo(cx - r * 0.20f, cy - r * 1.12f)
                lineTo(cx, cy - r * 1.55f)
                lineTo(cx + r * 0.20f, cy - r * 1.12f)
                lineTo(cx + r * 0.62f, cy - r * 1.38f)
                lineTo(cx + r * 0.55f, cy - r * 0.92f)
                close()
            }
            drawPath(crown, accent.copy(alpha = 0.96f), style = Stroke(3.5.dp.toPx()))
            drawPath(crown, hot, style = Stroke(1.dp.toPx()))
            val gem = Offset(cx, cy - r * 1.13f)
            drawCircle(accent.copy(alpha = 0.25f + gemPulse * 0.35f), r * 0.19f, gem)
            drawCircle(hot, r * 0.095f * gemPulse, gem)
            drawCircle(Color.White.copy(alpha = gemPulse), r * 0.035f, Offset(gem.x - r * 0.035f, gem.y - r * 0.035f))

            // Rotating fire sweep around the avatar ring.
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, accent, hot, accent, Color.Transparent)),
                startAngle = sweepAngle,
                sweepAngle = 86f,
                useCenter = false,
                topLeft = Offset(cx - r - 6.dp.toPx(), cy - r - 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size((r + 6.dp.toPx()) * 2f, (r + 6.dp.toPx()) * 2f),
                style = Stroke(width = 4.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Sparks travel around the frame rather than staying fixed in place.
            for (i in 0..15) {
                val a = Math.toRadians((i * 22.5) + sweepAngle * (if (i % 2 == 0) 1.0 else -0.72))
                val radius = r * (1.40f + (i % 4) * 0.075f)
                val drift = kotlin.math.sin(phase + i) * r * 0.045f
                val point = Offset(
                    cx + kotlin.math.cos(a).toFloat() * (radius + drift),
                    cy + kotlin.math.sin(a).toFloat() * (radius + drift)
                )
                val alpha = (0.28f + 0.72f * (0.5f + 0.5f * kotlin.math.sin(phase + i))).coerceIn(0f, 1f)
                drawCircle(accent.copy(alpha = alpha), (1.2f + (i % 3) * 0.65f) * density, point)
                if (i % 3 == 0) drawCircle(Color.White.copy(alpha = alpha * 0.8f), 0.8.dp.toPx(), point)
            }
        }

    }
}
@Composable
private fun NeonAuroraFrameOverlay(
    modifier: Modifier = Modifier,
    pulseAlpha: Float
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val cyan = Color(0xFF22D3EE).copy(alpha = 0.72f + pulseAlpha * 0.25f)
        val ice = Color(0xFFB9F7FF).copy(alpha = 0.72f * pulseAlpha)
        val blue = Color(0xFF168BFF).copy(alpha = 0.78f * pulseAlpha)
        val ring = h.coerceAtMost(w) * 0.322f

        // Bright circular rim sits just outside the unchanged full-size avatar.
        drawCircle(cyan, ring, androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(3.2.dp.toPx()))
        drawCircle(ice, ring - 5.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(1.2.dp.toPx()))
        drawCircle(blue.copy(alpha = 0.55f), ring + 4.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(1.4.dp.toPx()))

        // Crown above the avatar.
        val crown = Path().apply {
            moveTo(cx - ring * 0.72f, cy - ring * 0.55f)
            lineTo(cx - ring * 0.82f, cy - ring * 1.05f)
            lineTo(cx - ring * 0.35f, cy - ring * 0.78f)
            lineTo(cx, cy - ring * 1.32f)
            lineTo(cx + ring * 0.35f, cy - ring * 0.78f)
            lineTo(cx + ring * 0.82f, cy - ring * 1.05f)
            lineTo(cx + ring * 0.72f, cy - ring * 0.55f)
            close()
        }
        drawPath(crown, cyan, style = Stroke(3.dp.toPx()))
        drawPath(crown, ice.copy(alpha = 0.72f), style = Stroke(1.dp.toPx()))
        drawLine(ice, androidx.compose.ui.geometry.Offset(cx - ring * 0.72f, cy - ring * 0.55f), androidx.compose.ui.geometry.Offset(cx + ring * 0.72f, cy - ring * 0.55f), 3.dp.toPx())

        // Feather-like wings on both sides.
        for (side in listOf(-1f, 1f)) {
            val wing = Path().apply {
                moveTo(cx + side * ring * 0.82f, cy - ring * 0.28f)
                cubicTo(cx + side * ring * 1.35f, cy - ring * 0.65f, cx + side * ring * 1.55f, cy - ring * 0.05f, cx + side * ring * 1.08f, cy + ring * 0.38f)
                cubicTo(cx + side * ring * 1.42f, cy + ring * 0.15f, cx + side * ring * 1.24f, cy + ring * 0.78f, cx + side * ring * 0.68f, cy + ring * 0.74f)
            }
            drawPath(wing, cyan, style = Stroke(3.dp.toPx()))
            drawPath(wing, ice, style = Stroke(1.dp.toPx()))
            for (i in 0..2) {
                val y = cy - ring * 0.12f + i * ring * 0.27f
                drawLine(blue, androidx.compose.ui.geometry.Offset(cx + side * ring * 0.82f, y), androidx.compose.ui.geometry.Offset(cx + side * ring * (1.22f - i * 0.12f), y + ring * 0.18f), 2.dp.toPx())
            }
        }

        // Crystal centerpiece and hanging jewel details.
        fun diamond(x: Float, y: Float, r: Float) {
            val p = Path().apply {
                moveTo(x, y - r)
                lineTo(x + r * 0.62f, y)
                lineTo(x, y + r)
                lineTo(x - r * 0.62f, y)
                close()
            }
            drawPath(p, cyan, style = Stroke(2.dp.toPx()))
            drawPath(p, ice.copy(alpha = pulseAlpha), style = Stroke(0.8.dp.toPx()))
        }
        diamond(cx, cy - ring * 1.27f, ring * 0.13f)
        diamond(cx, cy + ring * 1.13f, ring * 0.18f)
        diamond(cx - ring * 1.02f, cy + ring * 0.78f, ring * 0.09f)
        diamond(cx + ring * 1.02f, cy + ring * 0.78f, ring * 0.09f)

        // Small animated glints around the rim.
        val glints = listOf(
            androidx.compose.ui.geometry.Offset(cx - ring * 1.02f, cy - ring * 0.72f),
            androidx.compose.ui.geometry.Offset(cx + ring * 1.02f, cy - ring * 0.72f),
            androidx.compose.ui.geometry.Offset(cx - ring * 1.08f, cy + ring * 0.18f),
            androidx.compose.ui.geometry.Offset(cx + ring * 1.08f, cy + ring * 0.18f)
        )
        glints.forEach {
            drawCircle(ice, 2.4.dp.toPx() * pulseAlpha, it)
            drawLine(ice, androidx.compose.ui.geometry.Offset(it.x - 5.dp.toPx(), it.y), androidx.compose.ui.geometry.Offset(it.x + 5.dp.toPx(), it.y), 1.dp.toPx())
            drawLine(ice, androidx.compose.ui.geometry.Offset(it.x, it.y - 5.dp.toPx()), androidx.compose.ui.geometry.Offset(it.x, it.y + 5.dp.toPx()), 1.dp.toPx())
        }
    }
}


@Composable
private fun AuroraPremiumFrameOverlay(
    modifier: Modifier = Modifier,
    pulseAlpha: Float
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = h.coerceAtMost(w) * 0.322f
        val gold = Color(0xFFFFD98A).copy(alpha = 0.92f * pulseAlpha)
        val champagne = Color(0xFFFFF0C2).copy(alpha = 0.92f * pulseAlpha)
        val emerald = Color(0xFF35D6A0).copy(alpha = 0.88f * pulseAlpha)
        val amethyst = Color(0xFFB99AFF).copy(alpha = 0.82f * pulseAlpha)
        val center = androidx.compose.ui.geometry.Offset(cx, cy)

        // Luxury Art-Deco double bezel: deliberately unlike Neon Aurora's cyan ring.
        drawCircle(gold, r + 3.dp.toPx(), center, style = Stroke(3.8.dp.toPx()))
        drawCircle(champagne, r - 3.dp.toPx(), center, style = Stroke(1.4.dp.toPx()))
        drawCircle(emerald.copy(alpha = 0.72f * pulseAlpha), r + 8.dp.toPx(), center, style = Stroke(1.1.dp.toPx()))

        // Elegant fan-shaped sunburst crest instead of a crown.
        val crest = Path().apply {
            moveTo(cx - r * 0.62f, cy - r * 0.70f)
            cubicTo(cx - r * 0.52f, cy - r * 1.13f, cx - r * 0.20f, cy - r * 1.24f, cx, cy - r * 1.40f)
            cubicTo(cx + r * 0.20f, cy - r * 1.24f, cx + r * 0.52f, cy - r * 1.13f, cx + r * 0.62f, cy - r * 0.70f)
        }
        drawPath(crest, gold, style = Stroke(3.2.dp.toPx()))
        for (i in -3..3) {
            val x = cx + i * r * 0.145f
            val height = (1.08f - kotlin.math.abs(i) * 0.10f) * r
            drawLine(champagne, androidx.compose.ui.geometry.Offset(x, cy - r * 0.78f), androidx.compose.ui.geometry.Offset(x, cy - height), 1.5.dp.toPx())
        }
        drawCircle(emerald, r * 0.105f, androidx.compose.ui.geometry.Offset(cx, cy - r * 1.28f))
        drawCircle(champagne, r * 0.045f, androidx.compose.ui.geometry.Offset(cx, cy - r * 1.28f))

        // Mirrored laurel branches with gold leaves; no wing silhouettes.
        for (side in listOf(-1f, 1f)) {
            val branch = Path().apply {
                moveTo(cx + side * r * 0.82f, cy + r * 0.72f)
                cubicTo(cx + side * r * 1.38f, cy + r * 0.28f, cx + side * r * 1.36f, cy - r * 0.42f, cx + side * r * 0.78f, cy - r * 0.78f)
            }
            drawPath(branch, gold, style = Stroke(2.6.dp.toPx()))
            for (i in 0..4) {
                val t = i / 4f
                val x = cx + side * r * (0.88f + 0.36f * kotlin.math.sin(t * Math.PI).toFloat())
                val y = cy + r * (0.58f - t * 1.22f)
                val leaf = Path().apply {
                    moveTo(x, y)
                    cubicTo(x + side * r * 0.28f, y - r * 0.02f, x + side * r * 0.30f, y - r * 0.20f, x + side * r * 0.08f, y - r * 0.22f)
                    cubicTo(x - side * r * 0.02f, y - r * 0.12f, x - side * r * 0.02f, y - r * 0.04f, x, y)
                }
                drawPath(leaf, if (i % 2 == 0) emerald else gold, style = Stroke(2.dp.toPx()))
            }
        }

        // Faceted emerald pendant at six o'clock.
        val jewel = Path().apply {
            moveTo(cx, cy + r * 1.22f)
            lineTo(cx + r * 0.20f, cy + r * 1.43f)
            lineTo(cx, cy + r * 1.68f)
            lineTo(cx - r * 0.20f, cy + r * 1.43f)
            close()
        }
        drawPath(jewel, emerald, style = Stroke(2.8.dp.toPx()))
        drawLine(champagne, androidx.compose.ui.geometry.Offset(cx, cy + r * 1.27f), androidx.compose.ui.geometry.Offset(cx, cy + r * 1.62f), 1.2.dp.toPx())
        drawCircle(amethyst, r * 0.065f, androidx.compose.ui.geometry.Offset(cx - r * 1.08f, cy + r * 0.62f))
        drawCircle(amethyst, r * 0.065f, androidx.compose.ui.geometry.Offset(cx + r * 1.08f, cy + r * 0.62f))

        // Slow luminous accents across the gold-and-emerald detailing.
        val glints = listOf(
            androidx.compose.ui.geometry.Offset(cx - r * 1.08f, cy - r * 0.48f),
            androidx.compose.ui.geometry.Offset(cx + r * 1.08f, cy - r * 0.48f),
            androidx.compose.ui.geometry.Offset(cx - r * 0.70f, cy + r * 0.96f),
            androidx.compose.ui.geometry.Offset(cx + r * 0.70f, cy + r * 0.96f)
        )
        glints.forEach { point ->
            drawCircle(champagne, 2.8.dp.toPx() * pulseAlpha, point)
            drawCircle(emerald.copy(alpha = 0.7f * pulseAlpha), 5.dp.toPx() * pulseAlpha, point, style = Stroke(1.dp.toPx()))
        }
    }
}


@Composable
private fun GalaxyFrameOverlay(modifier: Modifier, pulseAlpha: Float) {
    Canvas(modifier) {
        val cx=size.width/2f; val cy=size.height/2f; val r=size.minDimension*0.322f
        val violet=Color(0xFFB69CFF).copy(alpha=pulseAlpha)
        val blue=Color(0xFF68C8FF).copy(alpha=pulseAlpha)
        val white=Color(0xFFF1E9FF).copy(alpha=pulseAlpha)
        drawCircle(violet,r,androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(2.4.dp.toPx(),pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(),4.dp.toPx()))))
        drawCircle(blue,r+7.dp.toPx(),androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(1.2.dp.toPx()))
        // Orbiting elliptical rings and a pair of planetary satellites.
        drawOval(violet,topLeft=androidx.compose.ui.geometry.Offset(cx-r*1.48f,cy-r*.42f),size=androidx.compose.ui.geometry.Size(r*2.96f,r*.84f),style=Stroke(2.dp.toPx()))
        drawOval(blue,topLeft=androidx.compose.ui.geometry.Offset(cx-r*1.36f,cy-r*.30f),size=androidx.compose.ui.geometry.Size(r*2.72f,r*.60f),style=Stroke(1.dp.toPx()))
        drawCircle(white,r*.12f,androidx.compose.ui.geometry.Offset(cx-r*1.36f,cy-r*.28f))
        drawCircle(blue,r*.09f,androidx.compose.ui.geometry.Offset(cx+r*1.36f,cy+r*.28f))
        // Star-shaped constellation accents.
        val stars=listOf(androidx.compose.ui.geometry.Offset(cx,cy-r*1.38f),androidx.compose.ui.geometry.Offset(cx-r*1.10f,cy-r*.92f),androidx.compose.ui.geometry.Offset(cx+r*1.10f,cy-r*.92f),androidx.compose.ui.geometry.Offset(cx-r*1.12f,cy+r*.88f),androidx.compose.ui.geometry.Offset(cx+r*1.12f,cy+r*.88f),androidx.compose.ui.geometry.Offset(cx,cy+r*1.42f))
        stars.forEachIndexed { i,p ->
            val q=Path().apply { moveTo(p.x,p.y-r*.10f); lineTo(p.x+r*.055f,p.y-r*.055f); lineTo(p.x+r*.10f,p.y); lineTo(p.x+r*.055f,p.y+r*.055f); lineTo(p.x,p.y+r*.10f); lineTo(p.x-r*.055f,p.y+r*.055f); lineTo(p.x-r*.10f,p.y); lineTo(p.x-r*.055f,p.y-r*.055f); close() }
            drawPath(q,if(i%2==0) white else blue)
            drawCircle(violet,r*.045f,p)
        }
    }
}

@Composable
private fun RoyalGoldFrameOverlay(modifier: Modifier, pulseAlpha: Float) {
    Canvas(modifier) {
        val cx=size.width/2f; val cy=size.height/2f; val r=size.minDimension*.322f
        val gold=Color(0xFFFFD45C).copy(alpha=pulseAlpha); val light=Color(0xFFFFF0B0).copy(alpha=pulseAlpha)
        val dark=Color(0xFFB87916).copy(alpha=pulseAlpha)
        drawCircle(gold,r,androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(4.dp.toPx()))
        drawCircle(light,r-6.dp.toPx(),androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(1.3.dp.toPx()))
        // Regal shield crest, flanked by ornamental scrollwork.
        val shield=Path().apply { moveTo(cx-r*.55f,cy-r*.62f); lineTo(cx+r*.55f,cy-r*.62f); lineTo(cx+r*.48f,cy-r*1.05f); lineTo(cx,cy-r*1.35f); lineTo(cx-r*.48f,cy-r*1.05f); close() }
        drawPath(shield,dark,style=Stroke(3.dp.toPx())); drawPath(shield,light,style=Stroke(1.2.dp.toPx()))
        for(side in listOf(-1f,1f)) {
            val scroll=Path().apply { moveTo(cx+side*r*.82f,cy-r*.42f); cubicTo(cx+side*r*1.45f,cy-r*.68f,cx+side*r*1.52f,cy+r*.12f,cx+side*r*1.02f,cy+r*.32f); cubicTo(cx+side*r*1.40f,cy+r*.58f,cx+side*r*.92f,cy+r*.92f,cx+side*r*.72f,cy+r*.68f) }
            drawPath(scroll,gold,style=Stroke(3.dp.toPx()))
            for(i in 0..2) drawCircle(light,(3-i*.45f).dp.toPx(),androidx.compose.ui.geometry.Offset(cx+side*r*(1.03f+i*.13f),cy-r*.20f+i*r*.42f))
        }
        // Faceted gold medallion at the base.
        val gem=Path().apply { moveTo(cx,cy+r*1.13f); lineTo(cx+r*.22f,cy+r*1.38f); lineTo(cx,cy+r*1.63f); lineTo(cx-r*.22f,cy+r*1.38f); close() }
        drawPath(gem,gold,style=Stroke(3.dp.toPx())); drawLine(light,androidx.compose.ui.geometry.Offset(cx,cy+r*1.18f),androidx.compose.ui.geometry.Offset(cx,cy+r*1.58f),1.4.dp.toPx())
    }
}

@Composable
private fun RoseGoldFrameOverlay(modifier: Modifier, pulseAlpha: Float) {
    Canvas(modifier) {
        val cx=size.width/2f; val cy=size.height/2f; val r=size.minDimension*.322f
        val rose=Color(0xFFFF9DB7).copy(alpha=pulseAlpha); val blush=Color(0xFFFFE0E8).copy(alpha=pulseAlpha); val copper=Color(0xFFB96878).copy(alpha=pulseAlpha)
        drawCircle(rose,r,androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(3.dp.toPx()))
        drawCircle(blush,r-5.dp.toPx(),androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(1.1.dp.toPx()))
        // Floral garlands: paired petals create a soft feminine filigree silhouette.
        for(side in listOf(-1f,1f)) {
            for(i in 0..3) {
                val x=cx+side*r*(.82f+i*.13f); val y=cy-r*.60f+i*r*.38f
                val petal=Path().apply { moveTo(x,y); cubicTo(x+side*r*.28f,y-r*.20f,x+side*r*.43f,y+r*.08f,x,y+r*.18f); cubicTo(x-side*r*.12f,y+r*.05f,x-side*r*.12f,y-r*.10f,x,y); close() }
                drawPath(petal,if(i%2==0) rose else copper,style=Stroke(2.2.dp.toPx()))
                drawCircle(blush,r*.045f,androidx.compose.ui.geometry.Offset(x,y))
            }
            val vine=Path().apply { moveTo(cx+side*r*.78f,cy+r*.72f); cubicTo(cx+side*r*1.25f,cy+r*.35f,cx+side*r*1.25f,cy-r*.20f,cx+side*r*.78f,cy-r*.65f) }
            drawPath(vine,copper,style=Stroke(1.8.dp.toPx()))
        }
        // Floral rosette at twelve o'clock and dangling teardrop pearl below.
        for(i in 0..7) {
            val angle=i*Math.PI/4.0
            val x=cx+kotlin.math.cos(angle).toFloat()*r*.20f; val y=cy-r*1.28f+kotlin.math.sin(angle).toFloat()*r*.20f
            drawOval(color=rose,topLeft=androidx.compose.ui.geometry.Offset(x-r*.075f,y-r*.13f),size=androidx.compose.ui.geometry.Size(r*.15f,r*.26f),style=Stroke(1.5.dp.toPx()))
        }
        drawCircle(blush,r*.075f,androidx.compose.ui.geometry.Offset(cx,cy-r*1.28f))
        val drop=Path().apply { moveTo(cx,cy+r*1.10f); cubicTo(cx+r*.36f,cy+r*1.40f,cx+r*.12f,cy+r*1.65f,cx,cy+r*1.68f); cubicTo(cx-r*.12f,cy+r*1.65f,cx-r*.36f,cy+r*1.40f,cx,cy+r*1.10f) }
        drawPath(drop,rose,style=Stroke(2.6.dp.toPx())); drawCircle(blush,r*.045f,androidx.compose.ui.geometry.Offset(cx,cy+r*1.40f))
    }
}

@Composable
fun VipShiningNameText(
    text: String,
    vipLevel: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null
) {
    if (vipLevel > 0L) {
        val shimmer = rememberInfiniteTransition(label = "vip_name_amethyst_shimmer")
        val shimmerOffset by shimmer.animateFloat(
            initialValue = -260f,
            targetValue = 260f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vip_name_shimmer_offset"
        )
        val royalAmethystBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF6500A8),
                Color(0xFFD500F9),
                Color(0xFFFFD8FF),
                Color(0xFFFFFFFF),
                Color(0xFFD500F9),
                Color(0xFF6500A8)
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 260f, 90f)
        )
        Text(
            text = text,
            modifier = modifier,
            style = style.copy(brush = royalAmethystBrush),
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    }
}

@Composable
fun VipBadgePill(vipLevel: Long, modifier: Modifier = Modifier, compact: Boolean = false) {
    val displayVipLevel = vipLevel.coerceIn(0L, 8L)
    if (displayVipLevel <= 0L) return
    val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == displayVipLevel.toInt() }
    val accent = tier?.let { Color(it.badgeColorHex) } ?: VibeGold
    val badgeGradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF182B50),
            accent.copy(alpha = 0.72f),
            Color(0xFF26334F)
        )
    )
    val borderGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFE7A0), Color(0xFF438BFF), accent, Color(0xFFFFD66B))
    )

    // Original profile VIP badge: metallic blue gradient, luminous multicolor
    // outline, and a gold star/label. Tier accent differentiates VIP 1-8.
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        color = Color.Transparent,
        border = BorderStroke(if (compact) 1.5.dp else 2.dp, borderGradient)
    ) {
        Row(
            modifier = Modifier
                .background(badgeGradient)
                .padding(
                    horizontal = if (compact) 7.dp else 10.dp,
                    vertical = if (compact) 3.dp else 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFFFD66B),
                modifier = Modifier.size(if (compact) 12.dp else 16.dp)
            )
            Text(
                text = "VIP $displayVipLevel",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFD66B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun VipMiniBadge(vipLevel: Long, modifier: Modifier = Modifier) {
    val level = vipLevel.coerceIn(1L, 8L)
    if (level <= 0L) return
    val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == level.toInt() }
    val accent = tier?.let { Color(it.badgeColorHex) } ?: VibeGold
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(modifier = Modifier.size(width = 25.dp, height = 23.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shield = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.02f)
                    lineTo(size.width * 0.96f, size.height * 0.22f)
                    lineTo(size.width * 0.88f, size.height * 0.68f)
                    lineTo(size.width * 0.5f, size.height * 0.98f)
                    lineTo(size.width * 0.12f, size.height * 0.68f)
                    lineTo(size.width * 0.04f, size.height * 0.22f)
                    close()
                }
                drawPath(shield, Brush.linearGradient(listOf(Color(0xFFBFEAFF), accent, Color(0xFF183D78))))
                drawPath(shield, Color.White.copy(alpha = 0.92f), style = Stroke(width = 1.15.dp.toPx()))
                val inner = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.12f)
                    lineTo(size.width * 0.84f, size.height * 0.27f)
                    lineTo(size.width * 0.78f, size.height * 0.63f)
                    lineTo(size.width * 0.5f, size.height * 0.86f)
                    lineTo(size.width * 0.22f, size.height * 0.63f)
                    lineTo(size.width * 0.16f, size.height * 0.27f)
                    close()
                }
                drawPath(inner, Brush.linearGradient(listOf(Color(0xFF174B91), accent.copy(alpha = 0.9f))))
            }
            Text(
                text = "V$level",
                color = Color.White,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "VIP",
            color = accent,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun LevelBadge(level: Long, modifier: Modifier = Modifier) {
    // ID levels 1–21 each have a distinct badge palette; higher levels cycle safely.
    val palettes = listOf(
        listOf(Color(0xFFB8F4FF), Color(0xFF2878D8), Color(0xFFE9FFFF)),
        listOf(Color(0xFF8DEBD7), Color(0xFF167D78), Color(0xFFE1FFF7)),
        listOf(Color(0xFFC5B5FF), Color(0xFF5A43C8), Color(0xFFF2ECFF)),
        listOf(Color(0xFF9FE7FF), Color(0xFF2679B8), Color(0xFFEAFBFF)),
        listOf(Color(0xFF91B8FF), Color(0xFF3158B7), Color(0xFFEAF0FF)),
        listOf(Color(0xFFD0B5FF), Color(0xFF7545C8), Color(0xFFF7EDFF)),
        listOf(Color(0xFFFFE49A), Color(0xFFB47B1E), Color(0xFFFFF7D8)),
        listOf(Color(0xFFFFC77C), Color(0xFFB85A27), Color(0xFFFFE8C7)),
        listOf(Color(0xFFFFA98A), Color(0xFFB74444), Color(0xFFFFE4D8)),
        listOf(Color(0xFFFFD3A0), Color(0xFF9F673B), Color(0xFFFFF1DE)),
        listOf(Color(0xFFFFB879), Color(0xFF9D4E2D), Color(0xFFFFE2C9)),
        listOf(Color(0xFFFFD96D), Color(0xFF946515), Color(0xFFFFF4C2)),
        listOf(Color(0xFFDAB6FF), Color(0xFF7945B9), Color(0xFFF6E9FF)),
        listOf(Color(0xFFC5A0FF), Color(0xFF633BB1), Color(0xFFEFE4FF)),
        listOf(Color(0xFFE8A8FF), Color(0xFF963FAF), Color(0xFFFFE8FF)),
        listOf(Color(0xFFFFB8EA), Color(0xFF9A4EBB), Color(0xFFFFE8F8)),
        listOf(Color(0xFFBBA8FF), Color(0xFF5942BD), Color(0xFFEFEAFF)),
        listOf(Color(0xFFFFA8C9), Color(0xFFB33F76), Color(0xFFFFE5EF)),
        listOf(Color(0xFFB6F5FF), Color(0xFF3B94B8), Color(0xFFE8FDFF)),
        listOf(Color(0xFFFFE7A3), Color(0xFFA77A28), Color(0xFFFFF7DB)),
        listOf(Color(0xFFFFC6F1), Color(0xFF8549C4), Color(0xFFFFECFA))
    )
    val palette = palettes[(level - 1L).coerceAtLeast(0L).rem(palettes.size.toLong()).toInt()]
    val shape = when ((level - 1L).coerceAtLeast(0L).rem(3L).toInt()) {
        0 -> RoundedCornerShape(7.dp)
        1 -> RoundedCornerShape(50)
        else -> RoundedCornerShape(topStart = 3.dp, topEnd = 10.dp, bottomEnd = 3.dp, bottomStart = 10.dp)
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = palette[1].copy(alpha = 0.20f),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(palette[0], palette[1], palette[2])))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(palette[0].copy(alpha = 0.34f), palette[1].copy(alpha = 0.18f))))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = "Lv.$level",
                style = MaterialTheme.typography.labelSmall,
                color = palette[1],
                fontWeight = FontWeight.ExtraBold
            )
        }
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
fun RoomCoverImage(
    roomId: String,
    coverUrl: String,
    fallbackCoverKey: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val isFirestoreCover = coverUrl.startsWith("firestore-room-cover://")
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, roomId, coverUrl) {
        value = null
        if (isFirestoreCover) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val databaseId = context.getString(R.string.firestore_database_id)
                    val snapshot = FirebaseFirestore.getInstance(databaseId)
                        .collection("rooms").document(roomId)
                        .collection("cover").document("image").get().await()
                    val encoded = snapshot.getString("data") ?: return@runCatching null
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
        }
    }
    when {
        isFirestoreCover && bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Room cover",
            modifier = modifier,
            contentScale = contentScale
        )
        isFirestoreCover -> Image(
            painter = painterResource(id = coverDrawableRes(fallbackCoverKey)),
            contentDescription = "Room cover",
            modifier = modifier,
            contentScale = contentScale
        )
        coverUrl.isNotBlank() -> AsyncImage(
            model = coverUrl,
            contentDescription = "Room cover",
            modifier = modifier,
            contentScale = contentScale
        )
        else -> Image(
            painter = painterResource(id = coverDrawableRes(fallbackCoverKey)),
            contentDescription = "Room cover",
            modifier = modifier,
            contentScale = contentScale
        )
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
                RoomCoverImage(
                    roomId = room.id,
                    coverUrl = room.coverUrl,
                    fallbackCoverKey = room.coverKey,
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

private fun giftVideoSourceUrl(giftId: String): String? = when (giftId) {
    "gift_sunset_love" -> "https://raw.githubusercontent.com/sumanrajj213321/VibePlay-Anshu/main/gift-assets/sunset_love_gift.mp4"
    "gift_royal_vows" -> "https://raw.githubusercontent.com/sumanrajj213321/VibePlay-Anshu/main/gift-assets/From%20Klickpin.com-%20Discover%20Simple%20family%20dinner%20ideas%20that%20are%20trending%20right%20now%20across%20Pinterest%20boards%20for%20ideas%20worth%20saving%20right%20now-pin-id%20%28online-video-cutter.com%29.mp4"
    "gift_enchanted_heart" -> "https://raw.githubusercontent.com/sumanrajj213321/VibePlay-Anshu/main/gift-assets/enchanted_heart_gift.mp4"
    "gift_sunlit_dream" -> "https://raw.githubusercontent.com/sumanrajj213321/VibePlay-Anshu/main/From%20Klickpin.com-%20Save%20Modern%20weekend%20getaway%20inspiration%20that%20make%20your%20next%20project%20look%20polished%20and%20expensive%20for%20your%20next%20inspiration%20board%20%28online-video-cutter.com%29.mp4"
    "gift_prism_muse" -> "https://raw.githubusercontent.com/sumanrajj213321/VibePlay-Anshu/main/From%20Klickpin.com-%20Try%20this%20cozy%20easy%20makeup%20roundup%20that%20make%20everything%20look%20instantly%20polished%20with%20smart%20steps%20cute%20details%20and%20cozy%20vibes%20and.mp4"
    else -> null
}

@Composable
fun PreloadGiftVideo(gift: GiftCatalogItem?) {
    val context = LocalContext.current
    LaunchedEffect(gift?.id) {
        val activeGift = gift ?: return@LaunchedEffect
        val sourceUrl = giftVideoSourceUrl(activeGift.id) ?: return@LaunchedEffect
        // Begin downloading the selected gift while the user is reviewing it,
        // so its MP4 (including original audio) is ready before Send is tapped.
        cacheGiftVideo(context, activeGift.id, sourceUrl)
    }
}

private suspend fun cacheGiftVideo(context: Context, giftId: String, sourceUrl: String): java.io.File? =
    withContext(Dispatchers.IO) {
        val cacheFile = java.io.File(context.cacheDir, "gift-video-$giftId.mp4")
        if (cacheFile.exists() && cacheFile.length() > 0L) return@withContext cacheFile
        runCatching {
            val tempFile = java.io.File(context.cacheDir, "gift-video-$giftId.tmp")
            val connection = java.net.URL(sourceUrl).openConnection().apply {
                connectTimeout = 12_000
                readTimeout = 30_000
                useCaches = true
            }
            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (tempFile.length() > 0L) {
                if (cacheFile.exists()) cacheFile.delete()
                if (tempFile.renameTo(cacheFile)) cacheFile else null
            } else {
                tempFile.delete()
                null
            }
        }.getOrNull()
    }

@Composable
fun GiftCatalogArtwork(gift: GiftCatalogItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val videoGiftThumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, gift.id, context) {
        val sourceUrl = giftVideoSourceUrl(gift.id)
        value = if (sourceUrl == null) null else {
            val cachedFile = cacheGiftVideo(context, gift.id, sourceUrl)
            if (gift.id in setOf("gift_sunset_love", "gift_royal_vows", "gift_enchanted_heart", "gift_sunlit_dream", "gift_prism_muse") && cachedFile != null) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        MediaMetadataRetriever().let { retriever ->
                            try {
                                retriever.setDataSource(cachedFile.absolutePath)
                                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            } finally {
                                retriever.release()
                            }
                        }
                    }.getOrNull()
                }
            } else null
        }
    }
    if (gift.id in setOf("gift_sunset_love", "gift_royal_vows", "gift_enchanted_heart", "gift_sunlit_dream", "gift_prism_muse") && videoGiftThumbnail != null) {
        Image(
            bitmap = videoGiftThumbnail!!.asImageBitmap(),
            contentDescription = gift.name,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        return
    }
    if (gift.id == "gift_sunset_love") {
        Image(painter = painterResource(R.drawable.couple_kiss_thumbnail), contentDescription = gift.name,
            modifier = modifier.clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        return
    }
    val colors = when (gift.id) {
        "gift_royal_vows" -> listOf(Color(0xFFFFD76A), Color(0xFF8B1E3F))
        "gift_royal_ride" -> listOf(Color(0xFFFFD76A), Color(0xFF173B45))
        "gift_couple_kiss", "gift_love_promise", "gift_moonlight_dance" -> listOf(Color(0xFFFF8DBB), Color(0xFF713A91))
        "gift_royal_phoenix", "gift_galaxy_crown" -> listOf(Color(0xFFFFC857), Color(0xFF8138A8))
        "gift_crystal_wave", "gift_diamond_rain", "gift_ocean_palace" -> listOf(Color(0xFF37D9E8), Color(0xFF2450A4))
        "gift_dragon_flight", "gift_vibe_universe" -> listOf(Color(0xFF9B70F5), Color(0xFF22215D))
        else -> listOf(Color(0xFF42D5C7), Color(0xFF3153A5))
    }
    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(colors))) {
        val c = Offset(size.width * 0.5f, size.height * 0.48f)
        val u = size.minDimension * 0.22f
        val white = Color.White
        val gold = Color(0xFFFFE6A3)
        fun diamond(x: Float, y: Float, r: Float, color: Color) {
            val p = Path().apply { moveTo(x,y-r); lineTo(x+r*0.72f,y); lineTo(x,y+r); lineTo(x-r*0.72f,y); close() }
            drawPath(p,color); drawLine(white,Offset(x,y-r),Offset(x,y+r),1.2.dp.toPx())
        }
        when (gift.id) {
            "gift_aurora_bloom" -> {
                for(i in 0..7){val a=i*Math.PI.toFloat()/4f;drawCircle(Color(0xFFFFD5F0),u*0.5f,Offset(c.x+kotlin.math.cos(a)*u*0.72f,c.y+kotlin.math.sin(a)*u*0.72f))}
                drawCircle(gold,u*0.4f,c);drawLine(white,Offset(c.x,c.y+u*0.4f),Offset(c.x,c.y+u*1.6f),3.dp.toPx())
            }
            "gift_starfall" -> { diamond(c.x,c.y,u*1.3f,gold);drawLine(white,Offset(c.x-u*1.5f,c.y+u*1.4f),Offset(c.x+u*1.4f,c.y-u*1.4f),3.dp.toPx()) }
            "gift_crystal_wave" -> { val p=Path().apply{moveTo(0f,size.height*0.7f);cubicTo(size.width*0.25f,size.height*0.05f,size.width*0.65f,size.height*0.98f,size.width,size.height*0.25f);lineTo(size.width,size.height);lineTo(0f,size.height);close()};drawPath(p,Color(0xFF8CFBFF));drawLine(white,Offset(0f,size.height*0.7f),Offset(size.width,size.height*0.25f),2.dp.toPx()) }
            "gift_love_promise" -> { val p=Path().apply{moveTo(c.x,c.y+u*1.2f);cubicTo(c.x-u*2f,c.y,c.x-u*1.2f,c.y-u*1.5f,c.x,c.y-u*0.45f);cubicTo(c.x+u*1.2f,c.y-u*1.5f,c.x+u*2f,c.y,c.x,c.y+u*1.2f)};drawPath(p,Color(0xFFFFD0E4));drawPath(p,gold,style=Stroke(2.dp.toPx())) }
            "gift_couple_kiss" -> { drawCircle(Color(0xFFFFD8C8),u*0.65f,Offset(c.x-u*0.72f,c.y-u*0.45f));drawCircle(Color(0xFFFFD8C8),u*0.65f,Offset(c.x+u*0.72f,c.y-u*0.45f));drawOval(Color(0xFF512B59),Offset(c.x-u*1.35f,c.y-u*1.15f),androidx.compose.ui.geometry.Size(u*1.25f,u*0.7f));drawOval(Color(0xFF29365C),Offset(c.x+u*0.1f,c.y-u*1.15f),androidx.compose.ui.geometry.Size(u*1.25f,u*0.7f));drawRoundRect(Color(0xFFFF8DBB),Offset(c.x-u*1.35f,c.y+u*0.2f),androidx.compose.ui.geometry.Size(u*2.7f,u*1.35f),androidx.compose.ui.geometry.CornerRadius(u*0.5f)) }
            "gift_moonlight_dance" -> { drawCircle(gold,u*1.05f,Offset(c.x+u*0.55f,c.y-u*0.45f));drawCircle(colors.first(),u*0.92f,Offset(c.x+u*0.95f,c.y-u*0.75f));drawLine(white,Offset(c.x-u*0.7f,c.y-u*0.2f),Offset(c.x+u*0.7f,c.y+u*0.7f),4.dp.toPx());drawLine(gold,Offset(c.x-u*0.7f,c.y-u*0.2f),Offset(c.x-u*1.1f,c.y+u*1.25f),3.dp.toPx()) }
            "gift_royal_phoenix" -> { val p=Path().apply{moveTo(c.x,c.y+u*1.4f);cubicTo(c.x-u*2f,c.y+u*0.2f,c.x-u*1.8f,c.y-u*1.5f,c.x,c.y-u*0.4f);cubicTo(c.x+u*1.8f,c.y-u*1.5f,c.x+u*2f,c.y+u*0.2f,c.x,c.y+u*1.4f)};drawPath(p,gold);drawCircle(Color(0xFFFF8A36),u*0.42f,c) }
            "gift_galaxy_crown" -> { val p=Path().apply{moveTo(c.x-u*1.3f,c.y-u*0.7f);lineTo(c.x-u,c.y+u*0.65f);lineTo(c.x+u,c.y+u*0.65f);lineTo(c.x+u*1.3f,c.y-u*0.7f);lineTo(c.x+u*0.35f,c.y-u*0.1f);lineTo(c.x,c.y-u*1.1f);lineTo(c.x-u*0.35f,c.y-u*0.1f);close()};drawPath(p,gold) }
            "gift_diamond_rain" -> { diamond(c.x,c.y,u,white);diamond(c.x-u*1.1f,c.y-u*0.8f,u*0.42f,gold);diamond(c.x+u*1.15f,c.y+u*0.75f,u*0.48f,white) }
            "gift_dragon_flight" -> { val p=Path().apply{moveTo(c.x-u*1.5f,c.y+u*0.7f);cubicTo(c.x-u*0.3f,c.y-u*1.8f,c.x+u*0.3f,c.y+u*1.8f,c.x+u*1.5f,c.y-u*0.7f);lineTo(c.x+u*0.65f,c.y-u*0.35f);lineTo(c.x+u*0.95f,c.y-u*1.35f);lineTo(c.x-u*0.15f,c.y-u*0.6f);close()};drawPath(p,Color(0xFFD6B9FF));drawPath(p,white,style=Stroke(2.dp.toPx())) }
            "gift_ocean_palace" -> { drawRoundRect(Color(0xFFB6F8FF),Offset(c.x-u*1.35f,c.y-u*0.2f),androidx.compose.ui.geometry.Size(u*2.7f,u*1.35f),androidx.compose.ui.geometry.CornerRadius(u*0.12f));for(i in -1..1){val x=c.x+i*u*0.9f;val p=Path().apply{moveTo(x-u*0.42f,c.y-u*0.2f);lineTo(x,c.y-u*1.15f);lineTo(x+u*0.42f,c.y-u*0.2f);close()};drawPath(p,gold)} }
            "gift_vibe_universe" -> { drawCircle(white,u*0.48f,c);drawCircle(gold,u,c,style=Stroke(3.dp.toPx()));drawOval(white,Offset(c.x-u*1.5f,c.y-u*0.48f),androidx.compose.ui.geometry.Size(u*3f,u*0.95f),style=Stroke(3.dp.toPx())) }
            "gift_royal_ride" -> {
                // Static preview mirrors the Royal Ride overlay: twilight forest,
                // white horse, blue-and-crimson royal outfit, gold crown and sparks.
                val forest = Color(0xFF071C22)
                for (i in 0..4) {
                    val tx = c.x - u * 1.9f + i * u * 0.95f
                    val top = c.y + u * 0.05f - (i % 2) * u * 0.35f
                    drawPath(Path().apply {
                        moveTo(tx, c.y + u * 1.55f)
                        lineTo(tx - u * 0.42f, top)
                        lineTo(tx + u * 0.42f, top)
                        close()
                    }, forest.copy(alpha = 0.92f))
                }
                val horseColor = Color(0xFFF4F0E7)
                val horseY = c.y + u * 0.55f
                // Horse body, neck, head, mane and four legs follow the side profile
                // used by the full-screen Royal Ride animation.
                drawOval(Color.Black.copy(alpha = 0.22f), Offset(c.x-u*1.25f,horseY+u*0.48f), androidx.compose.ui.geometry.Size(u*2.45f,u*0.28f))
                drawOval(horseColor, Offset(c.x-u*1.15f,horseY-u*0.32f), androidx.compose.ui.geometry.Size(u*1.75f,u*0.78f))
                drawLine(horseColor,Offset(c.x+u*0.38f,horseY-u*0.02f),Offset(c.x+u*0.72f,horseY-u*0.9f),u*0.27f,cap=androidx.compose.ui.graphics.StrokeCap.Round)
                drawCircle(horseColor,u*0.27f,Offset(c.x+u*0.82f,horseY-u*0.91f))
                drawLine(Color(0xFFB9C6D5),Offset(c.x+u*0.66f,horseY-u*1.04f),Offset(c.x+u*0.4f,horseY-u*0.64f),u*0.1f,cap=androidx.compose.ui.graphics.StrokeCap.Round)
                drawPath(Path().apply { moveTo(c.x-u*1.12f,horseY-u*0.1f); cubicTo(c.x-u*1.55f,horseY-u*0.65f,c.x-u*1.5f,horseY+u*0.35f,c.x-u*1.28f,horseY+u*0.45f) },Color(0xFFDFE8F0),style=Stroke(u*0.12f))
                for (i in 0..3) {
                    val lx = c.x-u*0.78f+i*u*0.47f
                    drawLine(horseColor,Offset(lx,horseY+u*0.2f),Offset(lx+(if(i%2==0)-u*0.08f else u*0.08f),horseY+u*0.92f),u*0.12f,cap=androidx.compose.ui.graphics.StrokeCap.Round)
                }
                // Royal rider in the same navy-blue and crimson colors as the overlay.
                val riderX = c.x-u*0.1f
                drawRoundRect(Color(0xFF263B68),Offset(riderX-u*0.28f,horseY-u*1.0f),androidx.compose.ui.geometry.Size(u*0.56f,u*0.72f),androidx.compose.ui.geometry.CornerRadius(u*0.12f))
                drawPath(Path().apply {
                    moveTo(riderX-u*0.24f,horseY-u*0.38f)
                    lineTo(riderX+u*0.34f,horseY-u*0.38f)
                    lineTo(riderX+u*0.72f,horseY+u*0.42f)
                    lineTo(riderX-u*0.52f,horseY+u*0.42f)
                    close()
                },Color(0xFF7D3152))
                drawCircle(Color(0xFFF0C9A7),u*0.2f,Offset(riderX,horseY-u*1.15f))
                drawArc(gold,205f,130f,false,Offset(riderX-u*0.22f,horseY-u*1.42f),androidx.compose.ui.geometry.Size(u*0.44f,u*0.3f),style=Stroke(u*0.075f))
                drawLine(gold,Offset(riderX+u*0.16f,horseY-u*0.5f),Offset(c.x+u*0.9f,horseY-u*0.82f),u*0.045f,cap=androidx.compose.ui.graphics.StrokeCap.Round)
                for(i in 0..8) {
                    val px=c.x-u*1.45f+(i%5)*u*0.68f
                    val py=c.y-u*1.7f+(i/5)*u*0.35f
                    drawCircle(gold.copy(alpha=0.9f),2.dp.toPx(),Offset(px,py))
                }
            }
            "gift_royal_vows" -> { drawCircle(gold,u*1.05f,Offset(c.x-u*0.48f,c.y)); drawCircle(Color(0xFF8B1E3F),u*0.82f,Offset(c.x+u*0.48f,c.y)); drawCircle(Color(0xFFFFE6A3),u*0.2f,Offset(c.x,c.y-u*0.92f)); diamond(c.x,c.y-u*1.35f,u*0.42f,white) }
            else -> diamond(c.x,c.y,u,white)
        }
        for(i in 0..3)drawCircle(white.copy(alpha=0.75f),1.8.dp.toPx(),Offset(size.width*(0.16f+i*0.22f),size.height*(0.16f+(i%2)*0.68f)))
    }
}

@Composable
fun GiftBurstOverlay(event: GiftBurstEvent?, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val couple = event?.giftId == "gift_couple_kiss"
    val royalRide = event?.giftId == "gift_royal_ride"
    // Video gifts play their exact uploaded source videos.
    val videoGift = event?.giftId in setOf("gift_sunset_love", "gift_royal_vows", "gift_enchanted_heart", "gift_sunlit_dream", "gift_prism_muse")
    var videoUrl by remember(event?.id) { mutableStateOf<String?>(null) }
    var videoReady by remember(event?.id) { mutableStateOf(false) }
    var videoLoadFailed by remember(event?.id) { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    val exoPlayer = remember(event?.id) {
        if (videoGift) {
            ExoPlayer.Builder(context).build().apply {
                val giftAudioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build()
                // Do not request audio focus: the room's shared song must continue underneath.
                setAudioAttributes(giftAudioAttributes, false)
                volume = 1f
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                videoReady = true
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                videoLoadFailed = true
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnFinished()
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(event?.id, videoUrl, exoPlayer) {
        if (videoGift && videoUrl != null && exoPlayer != null) {
            videoReady = false
            videoLoadFailed = false
            val activeVideoUrl = videoUrl ?: return@LaunchedEffect
            exoPlayer.setMediaItem(MediaItem.fromUri(activeVideoUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // A failed remote video must not leave the gift overlay stuck on screen.
    LaunchedEffect(event?.id, videoLoadFailed) {
        if (videoGift && event != null && videoLoadFailed) {
            delay(1800L)
            currentOnFinished()
        }
    }

    LaunchedEffect(event?.id) {
        if (event == null) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(if (videoGift) 6900 else if (royalRide) 8500 else 5000, easing = LinearEasing)
        )
    }

    LaunchedEffect(event?.id) {
        val active = event
        if (active != null) {
            val sourceUrl = giftVideoSourceUrl(active.giftId)
            if (sourceUrl != null) {
                // Catalog artwork preloads these clips into app cache. Prefer the local
                // copy so video frames and their original audio start without a network wait.
                val cachedFile = withContext(Dispatchers.IO) {
                    java.io.File(context.cacheDir, "gift-video-${active.giftId}.mp4")
                        .takeIf { it.exists() && it.length() > 0L }
                }
                videoUrl = cachedFile?.toURI()?.toString() ?: sourceUrl
            } else {
                // Gift visuals must never request or emit STREAM_MUSIC audio:
                // keep the shared room song uninterrupted while the animation runs.
                delay(if (active.giftId == "gift_royal_ride") 8500L else 5000L)
                onFinished()
            }
        }
    }

    if (event != null) {
        val accent = if (royalRide) Color(0xFFFFD98A) else if (couple || videoGift) Color(0xFFFF8DBB) else when (event.giftId) {
            "gift_love_promise", "gift_moonlight_dance" -> Color(0xFFFF78B7)
            "gift_royal_phoenix", "gift_galaxy_crown" -> Color(0xFFFFC857)
            "gift_crystal_wave", "gift_ocean_palace", "gift_diamond_rain" -> Color(0xFF55DDF5)
            "gift_dragon_flight", "gift_vibe_universe" -> Color(0xFFB58BFF)
            else -> Color(0xFF8B9CFF)
        }
        Box(
            modifier.fillMaxSize().zIndex(100f)
                .then(if (videoGift && videoUrl != null && !videoLoadFailed) Modifier.background(Color.Black) else Modifier.background(Brush.verticalGradient(if (royalRide) listOf(Color(0xFF061A27), Color(0xFF123B38), Color(0xFF302C3E), Color(0xFF080F1C)) else listOf(Color(0xFF08091E), Color(0xFF171039), Color(0xFF260F36), Color(0xFF08091E)))))
        ) {
            if (videoGift && videoUrl != null && !videoLoadFailed) {
                // Draw an animated fallback immediately underneath the invisible
                // VideoView. This prevents SurfaceView's black buffer from blanking the screen.
                if (!videoReady) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = 0.38f), Color(0xFF090817), Color.Black),
                                Offset(w * 0.5f, h * 0.48f),
                                w * 0.9f
                            )
                        )
                        for (i in 0..85) {
                            val x = ((i * 73) % 997) / 997f * w
                            val y = ((i * 137) % 991) / 991f * h
                            val twinkle = 0.35f + 0.65f * kotlin.math.abs(
                                kotlin.math.sin(progress.value * 6.28f + i * 0.37f)
                            )
                            drawCircle(
                                accent.copy(alpha = twinkle),
                                (1.2f + (i % 3)) * density,
                                Offset(x, y)
                            )
                        }
                        val pulse = 0.82f + 0.18f * kotlin.math.sin(progress.value * 6.28f)
                        drawCircle(
                            accent.copy(alpha = 0.18f),
                            size.minDimension * 0.22f * pulse,
                            Offset(w * 0.5f, h * 0.48f)
                        )
                    }
                }
                if (exoPlayer != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = exoPlayer
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        update = { view ->
                            if (view.player !== exoPlayer) view.player = exoPlayer
                        },
                        onRelease = { view -> view.player = null }
                    )
                }
            } else if (royalRide) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val t = progress.value
                    val gold = Color(0xFFFFD98A)
                    val horse = Color(0xFFF4F0E7)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFBDEFFF).copy(alpha = 0.25f), Color.Transparent), Offset(w * 0.75f, h * 0.2f), w * 0.65f), w * 0.65f, Offset(w * 0.75f, h * 0.2f))
                    for (i in 0..8) {
                        val x = (i * 137 % 997) / 997f * w
                        val top = h * (0.45f + (i % 4) * 0.04f)
                        val p = Path().apply { moveTo(x, h); lineTo(x - w * 0.07f, top); lineTo(x + w * 0.07f, top); close() }
                        drawPath(p, Color(0xFF071C22).copy(alpha = 0.9f))
                    }
                    for (i in 0..5) {
                        val x = w * (0.12f + ((i * 0.17f + t * 0.4f) % 0.78f))
                        val y = h * (0.25f + (i % 3) * 0.14f + 0.025f * kotlin.math.sin((t * 8f + i).toDouble()).toFloat())
                        drawCircle(gold, 3.dp.toPx(), Offset(x, y))
                        drawCircle(Color(0xFFBDEFFF), 2.dp.toPx(), Offset(x + 5.dp.toPx(), y - 3.dp.toPx()))
                    }
                    val travel = ((t - 0.08f) / 0.82f).coerceIn(0f, 1f)
                    val cx = -w * 0.25f + w * 1.3f * travel
                    val cy = h * 0.68f
                    val s = w * 0.2f
                    drawOval(Color.Black.copy(alpha = 0.3f), Offset(cx - s, cy + s * 0.55f), androidx.compose.ui.geometry.Size(s * 2f, s * 0.2f))
                    drawOval(horse, Offset(cx - s * 0.7f, cy - s * 0.2f), androidx.compose.ui.geometry.Size(s * 1.4f, s * 0.62f))
                    drawLine(horse, Offset(cx + s * 0.4f, cy - s * 0.05f), Offset(cx + s * 0.72f, cy - s * 0.78f), s * 0.25f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawCircle(horse, s * 0.25f, Offset(cx + s * 0.78f, cy - s * 0.78f))
                    drawLine(Color(0xFFB9C6D5), Offset(cx + s * 0.55f, cy - s * 0.8f), Offset(cx + s * 0.25f, cy - s * 0.48f), s * 0.09f)
                    val stride = kotlin.math.sin((t * 42f).toDouble()).toFloat() * s * 0.12f
                    for (i in 0..3) {
                        val lx = cx - s * 0.5f + i * s * 0.34f
                        val swing = if (i % 2 == 0) stride else -stride
                        drawLine(horse, Offset(lx, cy + s * 0.2f), Offset(lx + swing, cy + s * 0.75f), s * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                    val rx = cx - s * 0.12f
                    drawRoundRect(Color(0xFF263B68), Offset(rx - s * 0.2f, cy - s * 0.78f), androidx.compose.ui.geometry.Size(s * 0.46f, s * 0.6f), androidx.compose.ui.geometry.CornerRadius(s * 0.12f))
                    drawPath(Path().apply { moveTo(rx - s * 0.18f, cy - s * 0.3f); lineTo(rx + s * 0.35f, cy - s * 0.3f); lineTo(rx + s * 0.6f, cy + s * 0.25f); lineTo(rx - s * 0.4f, cy + s * 0.25f); close() }, Color(0xFF7D3152))
                    drawCircle(Color(0xFFF0C9A7), s * 0.16f, Offset(rx, cy - s * 0.92f))
                    drawArc(gold, 205f, 130f, false, Offset(rx - s * 0.18f, cy - s * 1.13f), androidx.compose.ui.geometry.Size(s * 0.36f, s * 0.25f), style = Stroke(s * 0.045f))
                    for (i in 0..24) {
                        val px = cx - s + ((i * 47) % 113) / 113f * s * 2.3f
                        val py = cy - s * (0.9f + ((i * 31) % 100) / 100f)
                        drawCircle(gold.copy(alpha = 0.55f), (1.2f + (i % 3)) * density, Offset(px, py))
                    }
                }
            } else if (!videoGift || !videoReady || videoLoadFailed) {
                // Keep an animated visual on screen while a remote gift video buffers,
                // and fall back to it if the clip cannot be loaded.
                Canvas(Modifier.fillMaxSize()) {
                    val w=size.width; val h=size.height
                    drawRect(Brush.radialGradient(listOf(accent.copy(alpha=0.34f),Color.Transparent),Offset(w*0.5f,h*0.48f),w*0.78f))
                    for(i in 0..70) {
                        val x=((i*73)%997)/997f*w; val y=((i*137)%991)/991f*h
                        val drift=((progress.value*1.8f+i*0.11f)%1f)
                        drawCircle(if(i%3==0) accent.copy(alpha=0.78f) else Color.White.copy(alpha=0.55f),(2f+(i%4)*1.2f)*density,Offset(x,(y-drift*h*0.18f+h)%h))
                    }
                    for(ring in 1..4) drawCircle(accent.copy(alpha=0.18f-ring*0.025f),w*(0.12f+ring*0.12f+progress.value*0.08f),Offset(w*0.5f,h*0.49f),style=Stroke(width=2.5f.dp.toPx()))
                    if (couple) {
                        val approach=(progress.value*1.45f).coerceIn(0f,1f)
                        val lx=w*(0.25f+0.19f*approach); val rx=w*(0.75f-0.19f*approach)
                        val y=h*0.43f; val r=w*0.075f; val lean=if(progress.value>0.72f) r*0.16f else 0f
                        drawOval(Color(0xFF5A2D58),Offset(lx-r*1.05f,y-r*1.25f),androidx.compose.ui.geometry.Size(r*2.1f,r*2.55f))
                        drawCircle(Color(0xFFFFD8C8),r*0.88f,Offset(lx+lean,y))
                        drawOval(Color(0xFF3A234C),Offset(lx-r,y-r*1.12f),androidx.compose.ui.geometry.Size(r*2f,r*0.82f))
                        drawCircle(Color(0xFF35213D),r*0.075f,Offset(lx-r*0.3f,y)); drawCircle(Color(0xFF35213D),r*0.075f,Offset(lx+r*0.3f,y))
                        drawCircle(Color(0xFFFF91B6),r*0.13f,Offset(lx-r*0.48f,y+r*0.28f)); drawCircle(Color(0xFFFF91B6),r*0.13f,Offset(lx+r*0.48f,y+r*0.28f))
                        drawRoundRect(Color(0xFFFF78B7),Offset(lx-r*1.12f,y+r*0.92f),androidx.compose.ui.geometry.Size(r*2.24f,h*0.28f),androidx.compose.ui.geometry.CornerRadius(r*0.55f))
                        drawCircle(Color(0xFF20243D),r*1.05f,Offset(rx,y-r*0.12f)); drawCircle(Color(0xFFFFDCCB),r*0.86f,Offset(rx-lean,y))
                        drawOval(Color(0xFF20243D),Offset(rx-r,y-r*1.08f),androidx.compose.ui.geometry.Size(r*2f,r*0.72f))
                        drawCircle(Color(0xFF27304A),r*0.075f,Offset(rx-r*0.3f,y)); drawCircle(Color(0xFF27304A),r*0.075f,Offset(rx+r*0.3f,y))
                        drawRoundRect(Color(0xFF5B8DDA),Offset(rx-r*1.12f,y+r*0.92f),androidx.compose.ui.geometry.Size(r*2.24f,h*0.28f),androidx.compose.ui.geometry.CornerRadius(r*0.55f))
                        if(progress.value>0.72f) {
                            val glow=(progress.value-0.72f)/0.28f
                            drawCircle(Color(0xFFFFA7CF).copy(alpha=glow),r*0.25f,Offset(w*0.5f,y+r*0.2f))
                            for(i in 0..8) { val angle=i*(Math.PI.toFloat()*2f/9f); val rad=r*(1.3f+glow*0.8f); drawCircle(Color(0xFFFFC4DF).copy(alpha=glow),r*0.055f,Offset(w*0.5f+kotlin.math.cos(angle)*rad,y+kotlin.math.sin(angle)*rad)) }
                        }
                    } else {
                                            val center=Offset(w*0.5f,h*0.48f); val g=w*(0.13f+0.035f*kotlin.math.sin(progress.value*Math.PI.toFloat()))
                            val shape=Path().apply { moveTo(center.x,center.y-g); lineTo(center.x+g*0.72f,center.y); lineTo(center.x,center.y+g); lineTo(center.x-g*0.72f,center.y); close() }
                            drawPath(shape,accent); drawPath(shape,Color.White.copy(alpha=0.85f),style=Stroke(width=3.dp.toPx()))
                            for(i in 0..11) { val angle=i*(Math.PI.toFloat()*2f/12f); val inner=g*1.35f; val outer=g*(1.8f+progress.value*0.45f); drawLine(accent,Offset(center.x+kotlin.math.cos(angle)*inner,center.y+kotlin.math.sin(angle)*inner),Offset(center.x+kotlin.math.cos(angle)*outer,center.y+kotlin.math.sin(angle)*outer),strokeWidth=3.dp.toPx()) }
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 38.dp),
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.8f))
            ) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GIFT SENT BY", color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text(event.senderName, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(event.giftName, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                if (event.quantity > 1L) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, accent)
                    ) {
                        Text(
                            "×${event.quantity} COMBO",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("For ${event.recipientName}  ·  ${event.coinCost} coins", color = accent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

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
            val displayVipLevel = vip.vipLevel.coerceIn(0L, 8L)
            val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == displayVipLevel.toInt() }
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
                        vipLevel = displayVipLevel
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VipBadgePill(displayVipLevel)
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
