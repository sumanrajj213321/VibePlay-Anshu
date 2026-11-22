package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RelationshipStyle(
    val title: String,
    val detail: String,
    val accent: Color,
    val symbol: Int
)

@Composable
fun RelationshipScreen(onBack: () -> Unit) {
    val styles = listOf(
        RelationshipStyle("Buddies", "A playful bond built on shared moments.", Color(0xFF35C8F2), 0),
        RelationshipStyle("Bro", "A bold, steadfast bond with a shield crest.", Color(0xFF9B83FF), 1),
        RelationshipStyle("Confidant", "A refined bond represented by a crystal seal.", Color(0xFF38D5AE), 2),
        RelationshipStyle("Bestie", "A radiant bond with an exclusive star crest.", Color(0xFFFF78B7), 3)
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Relationships", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Choose a bond to invite another user. A tag becomes active only after they accept.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        styles.forEach { style ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = style.accent.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, style.accent.copy(alpha = 0.65f))
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RelationshipCustomMark(style.symbol, style.accent, Modifier.size(58.dp))
                    Column {
                        Text(style.title, color = style.accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(style.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10 familiarity levels", style = MaterialTheme.typography.labelSmall, color = style.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipCustomMark(kind: Int, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.065f, cap = StrokeCap.Round)
        drawCircle(accent.copy(alpha = 0.14f), radius = w * 0.47f, center = Offset(w / 2f, h / 2f))
        when (kind) {
            0 -> {
                drawCircle(accent, w * 0.22f, Offset(w * 0.38f, h * 0.48f), style = stroke)
                drawCircle(accent, w * 0.22f, Offset(w * 0.62f, h * 0.48f), style = stroke)
                drawLine(accent, Offset(w * 0.43f, h * 0.49f), Offset(w * 0.57f, h * 0.49f), w * 0.07f)
            }
            1 -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.17f)
                    lineTo(w * 0.79f, h * 0.29f)
                    lineTo(w * 0.73f, h * 0.65f)
                    lineTo(w * 0.5f, h * 0.84f)
                    lineTo(w * 0.27f, h * 0.65f)
                    lineTo(w * 0.21f, h * 0.29f)
                    close()
                }
                drawPath(p, accent, style = stroke)
                drawLine(accent, Offset(w * 0.37f, h * 0.49f), Offset(w * 0.63f, h * 0.49f), w * 0.07f)
            }
            2 -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.82f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.86f)
                    lineTo(w * 0.18f, h * 0.5f)
                    close()
                }
                drawPath(p, accent, style = stroke)
                drawLine(accent, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.72f), w * 0.045f)
                drawLine(accent, Offset(w * 0.32f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), w * 0.045f)
            }
            else -> {
                val p = Path()
                for (i in 0 until 10) {
                    val angle = Math.PI * i / 5.0 - Math.PI / 2.0
                    val radius = if (i % 2 == 0) w * 0.32f else w * 0.14f
                    val x = w / 2f + kotlin.math.cos(angle).toFloat() * radius
                    val y = h / 2f + kotlin.math.sin(angle).toFloat() * radius
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, accent, style = stroke)
                drawCircle(accent, w * 0.045f, Offset(w / 2f, h / 2f))
            }
        }
    }
}
