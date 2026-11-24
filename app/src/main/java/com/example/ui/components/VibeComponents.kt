package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
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
import coil.decode.GifDecoder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
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
    val firestoreAvatarUid = if (firestoreAvatar) avatarUrl.removePrefix("firestore-avatar://").substringBefore("?") else ""
    var firestoreAvatarBytes by remember(avatarUrl) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarUrl) {
        if (firestoreAvatar && firestoreAvatarUid.isNotBlank()) {
            // Avatar reads can fail briefly during reconnects. Retry instead of
            // permanently replacing a real photo with the initials fallback.
            repeat(3) { attempt ->
                val loaded = runCatching {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users").document(firestoreAvatarUid)
                        .collection("avatar").document("image")
                        .get().await()
                    val encoded = snapshot.getString("data")
                    if (encoded.isNullOrBlank()) null else Base64.decode(encoded, Base64.DEFAULT)
                }.getOrNull()
                if (loaded != null && loaded.isNotEmpty()) {
                    firestoreAvatarBytes = loaded
                    return@LaunchedEffect
                }
                if (attempt < 2) delay(500L * (attempt + 1))
            }
        }
    }
    val displayVipLevel = vipLevel.coerceIn(0L, 8L)
    val isNeonCyanFrame = frameColorArgb == 0xFF22D3EEL
    val isAuroraFrame = frameColorArgb == 0xFF34D399L
    val isRoseGoldFrame = frameColorArgb == 0xFFF472B6L
    val isRoyalGoldFrame = frameColorArgb == 0xFFFBBF24L
    val isGalaxyFrame = frameColorArgb == 0xFFA78BFAL
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
                if (frameColorArgb != null && !isNeonCyanFrame) {
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
                    width = if (isNeonCyanFrame) 0.dp else if (frameColorArgb != null || speaking || vipLevel > 0) 3.5.dp else 1.5.dp,
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
                if (room.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = room.coverUrl,
                        contentDescription = "${room.title} room cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = coverDrawableRes(room.coverKey)),
                        contentDescription = "${room.title} room cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
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
fun GiftCatalogArtwork(gift: GiftCatalogItem, modifier: Modifier = Modifier) {
    if (gift.id == "gift_sunset_love") {
        Image(painter = painterResource(R.drawable.couple_kiss_thumbnail), contentDescription = gift.name,
            modifier = modifier.clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        return
    }
    val colors = when (gift.id) {
        "gift_royal_vows" -> listOf(Color(0xFFFFD76A), Color(0xFF8B1E3F))
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
            "gift_royal_vows" -> { drawCircle(gold,u*1.05f,Offset(c.x-u*0.48f,c.y)); drawCircle(Color(0xFF8B1E3F),u*0.82f,Offset(c.x+u*0.48f,c.y)); drawCircle(Color(0xFFFFE6A3),u*0.2f,Offset(c.x,c.y-u*0.92f)); diamond(c.x,c.y-u*1.35f,u*0.42f,white) }
            else -> diamond(c.x,c.y,u,white)
        }
        for(i in 0..3)drawCircle(white.copy(alpha=0.75f),1.8.dp.toPx(),Offset(size.width*(0.16f+i*0.22f),size.height*(0.16f+(i%2)*0.68f)))
    }
}

@Composable
fun GiftBurstOverlay(event: GiftBurstEvent?, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val couple = event?.giftId == "gift_couple_kiss" || event?.giftId == "gift_royal_vows"
    val videoGift = event?.giftId == "gift_sunset_love"
    var videoUrl by remember(event?.id) { mutableStateOf<String?>(null) }
    var videoReady by remember(event?.id) { mutableStateOf(false) }
    var videoLoadFailed by remember(event?.id) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (event == null) 0f else 1f,
        animationSpec = tween(if (videoGift) 5200 else 5000, easing = LinearEasing),
        label = "full_screen_gift_progress"
    )

    LaunchedEffect(event?.id) {
        val active = event
        if (active != null) {
            if (active.giftId == "gift_sunset_love") {
                videoUrl = "https://raw.githubusercontent.com/sumanrajj213321/VibePlay-Anshu/main/gift-assets/sunset_love_gift.mp4"
                if (videoUrl == null) videoLoadFailed = true
            } else {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                try {
                    tone.startTone(ToneGenerator.TONE_DTMF_0 + Math.floorMod(active.giftId.hashCode(), 16), 420)
                    delay(5000L)
                } finally { tone.release() }
                onFinished()
            }
        }
    }

    LaunchedEffect(event?.id, videoReady, videoLoadFailed) {
        if (videoGift && event != null) {
            when {
                videoReady -> { delay(5200L); onFinished() }
                videoLoadFailed -> { delay(5000L); onFinished() }
            }
        }
    }

    if (event != null) {
        val accent = if (couple || videoGift) Color(0xFFFF8DBB) else when (event.giftId) {
            "gift_love_promise", "gift_moonlight_dance" -> Color(0xFFFF78B7)
            "gift_royal_phoenix", "gift_galaxy_crown" -> Color(0xFFFFC857)
            "gift_crystal_wave", "gift_ocean_palace", "gift_diamond_rain" -> Color(0xFF55DDF5)
            "gift_dragon_flight", "gift_vibe_universe" -> Color(0xFFB58BFF)
            else -> Color(0xFF8B9CFF)
        }
        Box(
            modifier.fillMaxSize().zIndex(100f)
                .then(if (videoGift && videoUrl != null && !videoLoadFailed) Modifier.background(Color.Black) else Modifier.background(Brush.verticalGradient(listOf(Color(0xFF08091E), Color(0xFF171039), Color(0xFF260F36), Color(0xFF08091E)))))
        ) {
            if (videoGift && videoUrl != null && !videoLoadFailed) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            tag = videoUrl
                            setVideoURI(Uri.parse(videoUrl))
                            setOnPreparedListener { player ->
                                player.setVolume(1f, 1f)
                                videoReady = true
                                start()
                            }
                            setOnErrorListener { _, _, _ ->
                                videoLoadFailed = true
                                true
                            }
                        }
                    },
                    update = { view ->
                        if (view.tag != videoUrl) {
                            view.tag = videoUrl
                            view.setVideoURI(Uri.parse(videoUrl))
                        }
                    },
                    onRelease = { it.stopPlayback() }
                )
            } else if (!videoGift || videoLoadFailed) {
                Canvas(Modifier.fillMaxSize()) {
                    val w=size.width; val h=size.height
                    drawRect(Brush.radialGradient(listOf(accent.copy(alpha=0.34f),Color.Transparent),Offset(w*0.5f,h*0.48f),w*0.78f))
                    for(i in 0..70) {
                        val x=((i*73)%997)/997f*w; val y=((i*137)%991)/991f*h
                        val drift=((progress*1.8f+i*0.11f)%1f)
                        drawCircle(if(i%3==0) accent.copy(alpha=0.78f) else Color.White.copy(alpha=0.55f),(2f+(i%4)*1.2f)*density,Offset(x,(y-drift*h*0.18f+h)%h))
                    }
                    for(ring in 1..4) drawCircle(accent.copy(alpha=0.18f-ring*0.025f),w*(0.12f+ring*0.12f+progress*0.08f),Offset(w*0.5f,h*0.49f),style=Stroke(width=2.5f.dp.toPx()))
                    if (couple) {
                        val approach=(progress*1.45f).coerceIn(0f,1f)
                        val lx=w*(0.25f+0.19f*approach); val rx=w*(0.75f-0.19f*approach)
                        val y=h*0.43f; val r=w*0.075f; val lean=if(progress>0.72f) r*0.16f else 0f
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
                        if(progress>0.72f) {
                            val glow=(progress-0.72f)/0.28f
                            drawCircle(Color(0xFFFFA7CF).copy(alpha=glow),r*0.25f,Offset(w*0.5f,y+r*0.2f))
                            for(i in 0..8) { val angle=i*(Math.PI.toFloat()*2f/9f); val rad=r*(1.3f+glow*0.8f); drawCircle(Color(0xFFFFC4DF).copy(alpha=glow),r*0.055f,Offset(w*0.5f+kotlin.math.cos(angle)*rad,y+kotlin.math.sin(angle)*rad)) }
                        }
                    } else {
                                            val center=Offset(w*0.5f,h*0.48f); val g=w*(0.13f+0.035f*kotlin.math.sin(progress*Math.PI.toFloat()))
                            val shape=Path().apply { moveTo(center.x,center.y-g); lineTo(center.x+g*0.72f,center.y); lineTo(center.x,center.y+g); lineTo(center.x-g*0.72f,center.y); close() }
                            drawPath(shape,accent); drawPath(shape,Color.White.copy(alpha=0.85f),style=Stroke(width=3.dp.toPx()))
                            for(i in 0..11) { val angle=i*(Math.PI.toFloat()*2f/12f); val inner=g*1.35f; val outer=g*(1.8f+progress*0.45f); drawLine(accent,Offset(center.x+kotlin.math.cos(angle)*inner,center.y+kotlin.math.sin(angle)*inner),Offset(center.x+kotlin.math.cos(angle)*outer,center.y+kotlin.math.sin(angle)*outer),strokeWidth=3.dp.toPx()) }
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
