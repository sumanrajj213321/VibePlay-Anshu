package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max

data class AnimatedRoomBackgroundPreset(val key: String, val label: String, val colors: List<Color>)

object AnimatedRoomBackgroundPresets {
    val entries = listOf(
        AnimatedRoomBackgroundPreset("aurora_luxe", "Aurora Luxe", listOf(Color(0xFF061A2D), Color(0xFF142C55), Color(0xFF38265F), Color(0xFF071B2D))),
        AnimatedRoomBackgroundPreset("royal_gold", "Royal Gold", listOf(Color(0xFF100D18), Color(0xFF332414), Color(0xFF80602B), Color(0xFF120E18))),
        AnimatedRoomBackgroundPreset("rose_velvet", "Rose Velvet", listOf(Color(0xFF210C20), Color(0xFF5A183D), Color(0xFF9C3B68), Color(0xFF180D22))),
        AnimatedRoomBackgroundPreset("emerald_palace", "Emerald Palace", listOf(Color(0xFF061B19), Color(0xFF0D4940), Color(0xFF94723A), Color(0xFF071B20))),
        AnimatedRoomBackgroundPreset("ocean_sapphire", "Ocean Sapphire", listOf(Color(0xFF041526), Color(0xFF06466A), Color(0xFF167B91), Color(0xFF06182D))),
        AnimatedRoomBackgroundPreset("cosmic_amethyst", "Cosmic Amethyst", listOf(Color(0xFF150B2B), Color(0xFF3D1B68), Color(0xFF7045A0), Color(0xFF100B2C))),
        AnimatedRoomBackgroundPreset("obsidian_silver", "Obsidian Silver", listOf(Color(0xFF080B12), Color(0xFF252C3B), Color(0xFF596273), Color(0xFF090D16)))
    )

    fun find(key: String): AnimatedRoomBackgroundPreset =
        entries.firstOrNull { it.key == key } ?: entries.first()
}

/**
 * Lightweight ambient room layer. It is intentionally non-interactive and must
 * remain the first child in the room content Box, underneath all room controls.
 */
@Composable
fun AnimatedRoomBackground(backgroundKey: String, modifier: Modifier = Modifier) {
    val preset = AnimatedRoomBackgroundPresets.find(backgroundKey)
    val transition = rememberInfiniteTransition(label = "luxury_room_background")

    val drift by transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "room_glow_drift"
    )
    val counterDrift by transition.animateFloat(
        initialValue = 1.10f,
        targetValue = -0.10f,
        animationSpec = infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Reverse),
        label = "room_counter_glow"
    )
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "room_particle_travel"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(5200), RepeatMode.Reverse),
        label = "room_soft_pulse"
    )

    val indigo = Color(0xFF11102F)
    val violet = Color(0xFF29204F)
    val amethyst = Color(0xFF9B78E8)
    val gold = Color(0xFFE7C77A)

    Box(modifier = modifier.background(Brush.verticalGradient(listOf(
        Color(0xFF090B20), indigo, violet, Color(0xFF15112F), Color(0xFF090A1B)
    )))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minSide = size.minDimension
            val glowRadius = max(w, h) * 0.62f

            // Slow, broad aurora glows; no image assets or per-frame allocations.
            drawRect(Brush.radialGradient(
                listOf(amethyst.copy(alpha = 0.20f + pulse * 0.10f), Color.Transparent),
                center = Offset(w * drift, h * 0.28f),
                radius = glowRadius
            ))
            drawRect(Brush.radialGradient(
                listOf(gold.copy(alpha = 0.10f + pulse * 0.035f), Color.Transparent),
                center = Offset(w * counterDrift, h * 0.43f),
                radius = glowRadius * 0.72f
            ))

            // Architectural perspective grid kept deliberately faint around seats.
            val gridColor = Color(0xFFBBA8F2).copy(alpha = 0.075f)
            for (i in 0..8) {
                val x = w * (i / 8f)
                drawLine(gridColor, Offset(x, h * 0.08f), Offset(w * (0.5f + (i / 8f - 0.5f) * 1.18f), h * 0.86f), 0.7f)
            }
            for (i in 0..6) {
                val y = h * (0.12f + i * 0.105f)
                drawLine(gridColor.copy(alpha = 0.8f), Offset(w * 0.04f, y), Offset(w * 0.96f, y), 0.7f)
            }

            // Delicate connector network and a travelling light pulse on each segment.
            val nodes = listOf(
                Offset(w * 0.10f, h * 0.18f), Offset(w * 0.34f, h * 0.12f),
                Offset(w * 0.66f, h * 0.12f), Offset(w * 0.90f, h * 0.20f),
                Offset(w * 0.16f, h * 0.56f), Offset(w * 0.84f, h * 0.56f),
                Offset(w * 0.28f, h * 0.76f), Offset(w * 0.72f, h * 0.76f)
            )
            val links = listOf(0 to 1, 1 to 2, 2 to 3, 0 to 4, 3 to 5, 4 to 6, 5 to 7, 6 to 7, 1 to 4, 2 to 5)
            links.forEachIndexed { index, pair ->
                val start = nodes[pair.first]
                val end = nodes[pair.second]
                drawLine(Color(0xFFBFA7F5).copy(alpha = 0.13f), start, end, 1f)
                val phase = (travel + index * 0.113f) % 1f
                val point = Offset(
                    start.x + (end.x - start.x) * phase,
                    start.y + (end.y - start.y) * phase
                )
                drawCircle(gold.copy(alpha = 0.20f), radius = minSide * 0.014f, center = point)
                drawCircle(Color(0xFFFFE9B2).copy(alpha = 0.70f), radius = minSide * 0.0032f, center = point)
            }

            // Small diamond nodes; subtle enough not to compete with avatars.
            nodes.forEachIndexed { index, point ->
                val nodePulse = if (index % 2 == 0) pulse else 0.82f - pulse * 0.35f
                drawCircle(amethyst.copy(alpha = 0.12f + nodePulse * 0.10f), minSide * 0.018f, point)
                val diamond = Path().apply {
                    moveTo(point.x, point.y - minSide * 0.006f)
                    lineTo(point.x + minSide * 0.006f, point.y)
                    lineTo(point.x, point.y + minSide * 0.006f)
                    lineTo(point.x - minSide * 0.006f, point.y)
                    close()
                }
                drawPath(diamond, gold.copy(alpha = 0.50f + nodePulse * 0.25f))
            }

            // Sparse slow-moving particles, with a restrained glow.
            for (i in 0 until 14) {
                val phase = (travel + i * 0.071f) % 1f
                val x = w * ((i * 0.173f + phase * (if (i % 2 == 0) 0.28f else -0.24f) + 1f) % 1f)
                val y = h * ((i * 0.137f + phase * 0.44f) % 0.78f)
                val radius = max(1.4f, minSide * (0.0017f + (i % 3) * 0.00045f))
                drawCircle(Color(0xFFFFE7B0).copy(alpha = 0.28f + pulse * 0.24f), radius, Offset(x, y))
                if (i % 3 == 0) {
                    drawCircle(amethyst.copy(alpha = 0.10f), radius * 4f, Offset(x, y))
                }
            }

            // Lower chat zone: intentionally darker and visually quiet.
            drawRect(Brush.verticalGradient(
                colorStops = arrayOf(
                    0.56f to Color.Transparent,
                    0.76f to Color(0xFF08091A).copy(alpha = 0.40f),
                    1f to Color(0xFF050713).copy(alpha = 0.84f)
                ),
                startY = h * 0.56f,
                endY = h
            ))
        }
    }
}
