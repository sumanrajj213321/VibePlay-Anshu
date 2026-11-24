package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Original VibePlay Charm emblem: a luminous faceted crystal inside a double halo.
 * Drawn natively with Compose Canvas; no emoji or external image asset.
 */
@Composable
fun CharmEmblem(level: Int = 1, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val tier = when {
        level >= 16 -> listOf(Color(0xFFFF8CE8), Color(0xFF8B5CFF), Color(0xFFFFD36E))
        level >= 7 -> listOf(Color(0xFFFFD36E), Color(0xFFFF8C42), Color(0xFFFFE7A0))
        level >= 4 -> listOf(Color(0xFF8CEBFF), Color(0xFF428BFF), Color(0xFFE4FCFF))
        else -> listOf(Color(0xFF7CFFF0), Color(0xFF4A8EFF), Color(0xFFE7FFFF))
    }
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Offset(w / 2f, h / 2f)
        val radius = w * .46f
        drawCircle(
            brush = Brush.radialGradient(listOf(tier[0].copy(alpha = .30f), tier[1].copy(alpha = .08f), Color.Transparent), center = c, radius = radius * 1.6f),
            radius = radius * 1.6f,
            center = c
        )
        drawCircle(color = tier[0].copy(alpha = .88f), radius = radius * .91f, center = c, style = Stroke(width = w * .045f))
        drawCircle(color = Color.White.copy(alpha = .65f), radius = radius * .78f, center = c, style = Stroke(width = w * .025f))
        val crystal = Path().apply {
            moveTo(w * .50f, h * .12f)
            lineTo(w * .79f, h * .36f)
            lineTo(w * .66f, h * .78f)
            lineTo(w * .50f, h * .91f)
            lineTo(w * .34f, h * .78f)
            lineTo(w * .21f, h * .36f)
            close()
        }
        drawPath(crystal, Brush.linearGradient(tier, start = Offset(w * .2f, h * .1f), end = Offset(w * .8f, h * .9f)))
        val leftFacet = Path().apply {
            moveTo(w * .21f, h * .36f)
            lineTo(w * .50f, h * .43f)
            lineTo(w * .50f, h * .91f)
            lineTo(w * .34f, h * .78f)
            close()
        }
        drawPath(leftFacet, Color.White.copy(alpha = .25f))
        val rightFacet = Path().apply {
            moveTo(w * .50f, h * .43f)
            lineTo(w * .79f, h * .36f)
            lineTo(w * .66f, h * .78f)
            lineTo(w * .50f, h * .91f)
            close()
        }
        drawPath(rightFacet, Color(0xFF182A64).copy(alpha = .16f))
        drawLine(Color.White.copy(alpha = .9f), Offset(w * .32f, h * .35f), Offset(w * .50f, h * .18f), strokeWidth = w * .045f)
        drawCircle(Color.White, radius = w * .035f, center = Offset(w * .76f, h * .20f))
        drawCircle(tier[0], radius = w * .045f, center = Offset(w * .16f, h * .66f))
    }
}
