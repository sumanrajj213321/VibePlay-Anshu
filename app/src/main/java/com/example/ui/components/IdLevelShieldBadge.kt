package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact ID-level emblem styled like the ornate VIP rank badges. */
@Composable
fun IdLevelShieldBadge(level: Long, modifier: Modifier = Modifier) {
    val safeLevel = level.coerceAtLeast(1L)
    val palette = when (((safeLevel - 1L) / 3L).toInt() % 7) {
        0 -> listOf(Color(0xFF8DEBFF), Color(0xFF1766B5), Color(0xFFE8FCFF))
        1 -> listOf(Color(0xFF9CF3D5), Color(0xFF167A68), Color(0xFFE5FFF5))
        2 -> listOf(Color(0xFFC9B2FF), Color(0xFF5C3AB8), Color(0xFFF4ECFF))
        3 -> listOf(Color(0xFFFFE49A), Color(0xFF9B6415), Color(0xFFFFF5D5))
        4 -> listOf(Color(0xFFFFB5A2), Color(0xFF9D3E54), Color(0xFFFFE8E1))
        5 -> listOf(Color(0xFFB8C8FF), Color(0xFF394C9B), Color(0xFFEEF0FF))
        else -> listOf(Color(0xFFFFC6F1), Color(0xFF8549A8), Color(0xFFFFECFA))
    }

    Box(
        modifier = modifier.size(width = 34.dp, height = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Symmetrical swept wings, matching the silhouette of the VIP emblems.
            val leftWing = Path().apply {
                moveTo(w * 0.29f, h * 0.43f)
                lineTo(w * 0.03f, h * 0.20f)
                lineTo(w * 0.10f, h * 0.48f)
                lineTo(w * 0.00f, h * 0.43f)
                lineTo(w * 0.17f, h * 0.72f)
                lineTo(w * 0.32f, h * 0.66f)
                close()
            }
            val rightWing = Path().apply {
                moveTo(w * 0.71f, h * 0.43f)
                lineTo(w * 0.97f, h * 0.20f)
                lineTo(w * 0.90f, h * 0.48f)
                lineTo(w * 1.00f, h * 0.43f)
                lineTo(w * 0.83f, h * 0.72f)
                lineTo(w * 0.68f, h * 0.66f)
                close()
            }
            val wingBrush = Brush.linearGradient(listOf(palette[2], palette[0], palette[1]))
            drawPath(leftWing, wingBrush)
            drawPath(rightWing, wingBrush)
            drawPath(leftWing, palette[2], style = Stroke(width = 0.8.dp.toPx()))
            drawPath(rightWing, palette[2], style = Stroke(width = 0.8.dp.toPx()))

            // Small crown points above the shield.
            val crown = Path().apply {
                moveTo(w * 0.28f, h * 0.28f)
                lineTo(w * 0.23f, h * 0.08f)
                lineTo(w * 0.39f, h * 0.20f)
                lineTo(w * 0.50f, h * 0.02f)
                lineTo(w * 0.61f, h * 0.20f)
                lineTo(w * 0.77f, h * 0.08f)
                lineTo(w * 0.72f, h * 0.28f)
                close()
            }
            drawPath(crown, Brush.verticalGradient(listOf(palette[2], palette[0], palette[1])))
            drawPath(crown, Color.White.copy(alpha = 0.9f), style = Stroke(width = 0.9.dp.toPx()))

            val shield = Path().apply {
                moveTo(w * 0.50f, h * 0.17f)
                lineTo(w * 0.83f, h * 0.31f)
                lineTo(w * 0.78f, h * 0.69f)
                lineTo(w * 0.50f, h * 0.96f)
                lineTo(w * 0.22f, h * 0.69f)
                lineTo(w * 0.17f, h * 0.31f)
                close()
            }
            drawPath(shield, Brush.linearGradient(listOf(palette[2], palette[0], palette[1])))
            drawPath(shield, Color.White.copy(alpha = 0.95f), style = Stroke(width = 1.1.dp.toPx()))

            val inner = Path().apply {
                moveTo(w * 0.50f, h * 0.27f)
                lineTo(w * 0.73f, h * 0.37f)
                lineTo(w * 0.69f, h * 0.66f)
                lineTo(w * 0.50f, h * 0.83f)
                lineTo(w * 0.31f, h * 0.66f)
                lineTo(w * 0.27f, h * 0.37f)
                close()
            }
            drawPath(inner, Brush.verticalGradient(listOf(palette[1], Color.Black.copy(alpha = 0.28f))))
        }

        // Only the level number is shown; the redundant "ID" label is removed.
        Text(
            text = safeLevel.toString(),
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}
