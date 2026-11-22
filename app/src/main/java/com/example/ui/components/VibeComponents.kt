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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
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
fun VipBadgePill(vipLevel: Long, modifier: Modifier = Modifier, compact: Boolean = false) {
    val displayVipLevel = vipLevel.coerceIn(0L, 8L)
    if (displayVipLevel <= 0L) return
    val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == displayVipLevel.toInt() }
    val accent = tier?.let { Color(it.badgeColorHex) } ?: VibeGold
    // Each VIP tier uses its own strong, clearly visible badge background.
    val luxuryBrush = Brush.linearGradient(
        listOf(
            accent,
            accent.copy(alpha = 0.82f)
        )
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accent,
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.88f), accent, Color.White.copy(alpha = 0.72f)))
        )
    ) {
        Row(
            modifier = Modifier
                .background(luxuryBrush)
                .padding(horizontal = if (compact) 6.dp else 11.dp, vertical = if (compact) 2.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (compact) 10.dp else 14.dp)
            )
            Text(
                text = "VIP",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Text(
                text = displayVipLevel.toString(),
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
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
