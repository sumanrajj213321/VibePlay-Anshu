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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
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

@Composable
fun AnimatedRoomBackground(backgroundKey: String, modifier: Modifier = Modifier) {
    val preset = AnimatedRoomBackgroundPresets.find(backgroundKey)
    val transition = rememberInfiniteTransition(label = "luxury_room_background")
    val drift by transition.animateFloat(
        initialValue = -0.16f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "room_background_drift"
    )
    val counterDrift by transition.animateFloat(
        initialValue = 1.12f,
        targetValue = -0.12f,
        animationSpec = infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse),
        label = "room_background_counter_drift"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Reverse),
        label = "room_background_shimmer"
    )

    Box(modifier = modifier.background(Brush.verticalGradient(preset.colors))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = max(w, h) * 0.66f
            val glow1 = Brush.radialGradient(
                colors = listOf(preset.colors[2].copy(alpha = 0.42f * shimmer), Color.Transparent),
                center = Offset(w * drift, h * 0.28f),
                radius = radius
            )
            val glow2 = Brush.radialGradient(
                colors = listOf(preset.colors[1].copy(alpha = 0.48f), Color.Transparent),
                center = Offset(w * counterDrift, h * 0.78f),
                radius = radius * 0.9f
            )
            drawRect(glow1)
            drawRect(glow2)
            // Soft floating light ribbons create a slow, premium ambient motion.
            val ribbonColor = preset.colors[2].copy(alpha = 0.11f * shimmer)
            for (i in 0..3) {
                val y = h * (0.18f + i * 0.22f)
                drawLine(
                    color = ribbonColor,
                    start = Offset(w * (drift * 0.22f), y),
                    end = Offset(w * (0.7f + counterDrift * 0.25f), y + h * 0.12f),
                    strokeWidth = max(1.2f, w * 0.0035f)
                )
            }
        }
    }
}
