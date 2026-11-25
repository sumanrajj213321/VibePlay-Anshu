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

data class AnimatedRoomBackgroundPreset(val key: String, val label: String, val colors: List<Color>, val style: String = "network")

object AnimatedRoomBackgroundPresets {
    val entries = listOf(
        AnimatedRoomBackgroundPreset("aurora_luxe", "Aurora Luxe", listOf(Color(0xFF061A2D), Color(0xFF142C55), Color(0xFF38265F), Color(0xFF071B2D))),
        AnimatedRoomBackgroundPreset("royal_gold", "Royal Gold", listOf(Color(0xFF100D18), Color(0xFF332414), Color(0xFF80602B), Color(0xFF120E18))),
        AnimatedRoomBackgroundPreset("rose_velvet", "Rose Velvet", listOf(Color(0xFF210C20), Color(0xFF5A183D), Color(0xFF9C3B68), Color(0xFF180D22))),
        AnimatedRoomBackgroundPreset("emerald_palace", "Emerald Palace", listOf(Color(0xFF061B19), Color(0xFF0D4940), Color(0xFF94723A), Color(0xFF071B20))),
        AnimatedRoomBackgroundPreset("ocean_sapphire", "Ocean Sapphire", listOf(Color(0xFF041526), Color(0xFF06466A), Color(0xFF167B91), Color(0xFF06182D))),
        AnimatedRoomBackgroundPreset("cosmic_amethyst", "Cosmic Amethyst", listOf(Color(0xFF150B2B), Color(0xFF3D1B68), Color(0xFF7045A0), Color(0xFF100B2C))),
        AnimatedRoomBackgroundPreset("obsidian_silver", "Obsidian Silver", listOf(Color(0xFF080B12), Color(0xFF252C3B), Color(0xFF596273), Color(0xFF090D16))),
        AnimatedRoomBackgroundPreset("celestial_tides", "Celestial Tides", listOf(Color(0xFF031B35), Color(0xFF075477), Color(0xFF1A9CA3), Color(0xFF041329)), "tides"),
        AnimatedRoomBackgroundPreset("royal_bloom", "Royal Bloom", listOf(Color(0xFF260B27), Color(0xFF70254F), Color(0xFFC16C83), Color(0xFF170D29)), "bloom"),
        AnimatedRoomBackgroundPreset("cosmic_orbit", "Cosmic Orbit", listOf(Color(0xFF090D2B), Color(0xFF252A68), Color(0xFF5A4BA1), Color(0xFF07091B)), "orbit"),
        AnimatedRoomBackgroundPreset("golden_cascade", "Golden Cascade", listOf(Color(0xFF211506), Color(0xFF66451A), Color(0xFFC08A37), Color(0xFF100D12)), "cascade"),
        AnimatedRoomBackgroundPreset("crystal_circuit", "Crystal Circuit", listOf(Color(0xFF061B1B), Color(0xFF123F46), Color(0xFF398B91), Color(0xFF06111D)), "circuit")
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

    Box(modifier = modifier.background(Brush.verticalGradient(preset.colors))) {
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

            when (preset.style) {
                "tides" -> {
                    // Layered ocean ribbons drift horizontally at different speeds.
                    for (band in 0..4) {
                        val y = h * (0.20f + band * 0.135f)
                        val shift = ((drift * (if (band % 2 == 0) 1f else -0.72f)) + 1f) % 1f
                        val path = Path().apply {
                            moveTo(-w * 0.2f, y)
                            cubicTo(w * 0.18f, y - h * 0.10f, w * 0.42f, y + h * 0.12f, w * 0.68f, y - h * 0.025f)
                            cubicTo(w * 0.86f, y - h * 0.10f, w * 1.08f, y + h * 0.08f, w * 1.2f, y)
                        }
                        drawPath(path, Color(0xFF7CE6E2).copy(alpha = 0.10f + pulse * 0.07f), style = Stroke(width = minSide * 0.018f))
                        drawCircle(Color(0xFFB8FFFF).copy(alpha = 0.48f), minSide * 0.003f, Offset(w * shift, y))
                    }
                    for (i in 0..20) {
                        val x = w * ((i * 0.137f + travel * 0.08f) % 1f)
                        val y = h * (0.12f + (i % 8) * 0.085f)
                        drawCircle(Color(0xFF9BFFFF).copy(alpha = 0.22f), minSide * 0.0025f, Offset(x, y))
                    }
                }
                "bloom" -> {
                    // Large translucent floral petals slowly rotate around two quiet focal points.
                    val centers = listOf(Offset(w * 0.18f, h * 0.30f), Offset(w * 0.82f, h * 0.42f))
                    centers.forEachIndexed { ci, center ->
                        for (petal in 0..5) {
                            val angle = (petal * 60f + (if (ci == 0) drift else counterDrift) * 24f) * (Math.PI / 180.0)
                            val dx = kotlin.math.cos(angle).toFloat()
                            val dy = kotlin.math.sin(angle).toFloat()
                            val end = Offset(center.x + dx * minSide * 0.105f, center.y + dy * minSide * 0.105f)
                            drawLine(Color(0xFFFFB5D2).copy(alpha = 0.16f), center, end, minSide * 0.026f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            drawCircle(Color(0xFFFFD5E2).copy(alpha = 0.18f + pulse * 0.08f), minSide * 0.012f, end)
                        }
                        drawCircle(Color(0xFFFFD6A0).copy(alpha = 0.16f), minSide * 0.018f, center)
                    }
                    for (i in 0..11) {
                        val x = w * ((i * 0.19f + travel * 0.12f) % 1f)
                        val y = h * (0.10f + (i % 6) * 0.12f)
                        drawCircle(Color(0xFFFFC6D9).copy(alpha = 0.42f), minSide * 0.0028f, Offset(x, y))
                    }
                }
                "orbit" -> {
                    // Elliptical orbital paths and slow-moving luminous bodies.
                    val center = Offset(w * 0.5f, h * 0.36f)
                    for (ring in 0..3) {
                        val rw = w * (0.20f + ring * 0.095f)
                        val rh = h * (0.055f + ring * 0.027f)
                        drawOval(Color(0xFFB7A5FF).copy(alpha = 0.13f), Offset(center.x - rw, center.y - rh), androidx.compose.ui.geometry.Size(rw * 2f, rh * 2f), style = Stroke(minSide * 0.0018f))
                        val angle = (travel * 360f + ring * 83f) * (Math.PI / 180.0)
                        val point = Offset(center.x + kotlin.math.cos(angle).toFloat() * rw, center.y + kotlin.math.sin(angle).toFloat() * rh)
                        drawCircle(Color(0xFFFFD98A).copy(alpha = 0.20f), minSide * 0.019f, point)
                        drawCircle(Color(0xFFFFEBC0), minSide * 0.0035f, point)
                    }
                    drawCircle(Color(0xFFB99AFF).copy(alpha = 0.13f + pulse * 0.08f), minSide * 0.085f, center)
                }
                "cascade" -> {
                    // Soft vertical golden light curtains, flowing down at staggered rates.
                    for (i in 0..10) {
                        val x = w * (i / 10f)
                        val phase = (travel + i * 0.083f) % 1f
                        val top = h * (phase * 0.72f - 0.18f)
                        drawLine(Color(0xFFFFD88A).copy(alpha = 0.10f), Offset(x, 0f), Offset(x + w * 0.035f, h * 0.82f), minSide * 0.003f)
                        drawCircle(Color(0xFFFFE8B3).copy(alpha = 0.38f), minSide * 0.004f, Offset(x + w * 0.035f, top + h * 0.16f))
                        drawCircle(Color(0xFFFFC86B).copy(alpha = 0.10f), minSide * 0.025f, Offset(x + w * 0.035f, top + h * 0.16f))
                    }
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFFD98A).copy(alpha = 0.07f), Color.Transparent)))
                }
                "circuit" -> {
                    // Angular circuit traces with travelling pulses and crystalline junctions.
                    for (i in 0..6) {
                        val y = h * (0.13f + i * 0.105f)
                        val x0 = w * (0.08f + (i % 2) * 0.12f)
                        val x1 = w * (0.90f - (i % 2) * 0.12f)
                        val mid = (x0 + x1) * 0.5f
                        val path = Path().apply {
                            moveTo(x0, y)
                            lineTo(mid - w * 0.045f, y)
                            lineTo(mid, y - h * 0.07f)
                            lineTo(mid + w * 0.045f, y)
                            lineTo(x1, y)
                        }
                        drawPath(path, Color(0xFF8CE8DF).copy(alpha = 0.15f), style = Stroke(minSide * 0.0018f))
                        val px = x0 + (x1 - x0) * ((travel + i * 0.14f) % 1f)
                        drawCircle(Color(0xFFB8FFF1).copy(alpha = 0.55f), minSide * 0.0035f, Offset(px, y))
                        drawCircle(Color(0xFF73D8D0).copy(alpha = 0.12f), minSide * 0.018f, Offset(mid, y - h * 0.07f))
                    }
                }
                else -> Unit
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
