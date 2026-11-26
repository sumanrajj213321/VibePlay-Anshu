package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

/** Dedicated shield emblem for the user's ID progression level. */
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
    Box(modifier = modifier.size(width = 43.dp, height = 48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val shield = Path().apply {
                moveTo(size.width * 0.5f, size.height * 0.04f)
                lineTo(size.width * 0.94f, size.height * 0.20f)
                lineTo(size.width * 0.88f, size.height * 0.66f)
                lineTo(size.width * 0.5f, size.height * 0.97f)
                lineTo(size.width * 0.12f, size.height * 0.66f)
                lineTo(size.width * 0.06f, size.height * 0.20f)
                close()
            }
            drawPath(shield, Brush.linearGradient(listOf(palette[0], palette[1], palette[2])))
            drawPath(shield, Color.White.copy(alpha = 0.9f), style = Stroke(width = 1.6.dp.toPx()))
            val inner = Path().apply {
                moveTo(size.width * 0.5f, size.height * 0.15f)
                lineTo(size.width * 0.82f, size.height * 0.27f)
                lineTo(size.width * 0.77f, size.height * 0.62f)
                lineTo(size.width * 0.5f, size.height * 0.84f)
                lineTo(size.width * 0.23f, size.height * 0.62f)
                lineTo(size.width * 0.18f, size.height * 0.27f)
                close()
            }
            drawPath(inner, Brush.verticalGradient(listOf(palette[1].copy(alpha = 0.92f), Color.Black.copy(alpha = 0.24f))))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ID", color = Color.White.copy(alpha = 0.88f), fontSize = 7.sp, lineHeight = 8.sp, fontWeight = FontWeight.Black)
            Text(safeLevel.toString(), color = Color.White, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}
