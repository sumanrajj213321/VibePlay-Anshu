package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Original VibePlay luxury Charm crest: animated faceted crystal and halo, no emoji. */
@Composable
fun CharmEmblem(level: Int = 1, modifier: Modifier = Modifier, size: Dp = 24.dp, showLevel: Boolean = false) {
    val tier = when {
        level >= 16 -> listOf(Color(0xFFFFB5F1), Color(0xFF9B62FF), Color(0xFFFFD88A))
        level >= 7 -> listOf(Color(0xFFFFE18A), Color(0xFFFF963F), Color(0xFFFFF0B5))
        level >= 4 -> listOf(Color(0xFF9AF3FF), Color(0xFF397DFF), Color(0xFFE8FDFF))
        else -> listOf(Color(0xFF9BFFF1), Color(0xFF557CFF), Color(0xFFF1FFFF))
    }
    val transition = rememberInfiniteTransition(label = "charm_luxury")
    val pulse by transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "charm_pulse"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.22f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "charm_shimmer"
    )
    val orbit by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "charm_orbit"
    )
    Box(modifier = modifier.size(size * 1.28f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawContext.transform.scale(pulse, pulse, pivot = center)
            val w = this.size.width
            val h = this.size.height
            val c = Offset(w / 2f, h / 2f)
            val radius = w * .45f
            drawCircle(
                brush = Brush.radialGradient(listOf(tier[0].copy(alpha = .34f * shimmer), tier[1].copy(alpha = .13f), Color.Transparent), center = c, radius = radius * 1.8f),
                radius = radius * 1.8f, center = c
            )
            drawCircle(Brush.sweepGradient(listOf(tier[2], tier[0], Color.White, tier[1], tier[2])), radius * .91f, c, style = Stroke(w * .055f))
            drawCircle(Color.White.copy(alpha = .76f), radius * .77f, c, style = Stroke(w * .022f))
            rotate(orbit, pivot = c) {
                drawOval(
                    color = tier[2].copy(alpha = .82f * shimmer),
                    topLeft = Offset(w * .08f, h * .34f),
                    size = androidx.compose.ui.geometry.Size(w * .84f, h * .32f),
                    style = Stroke(w * .025f)
                )
                drawCircle(Color.White.copy(alpha = shimmer), radius = w * .045f, center = Offset(w * .91f, h * .50f))
            }
            val crown = Path().apply {
                moveTo(w * .50f, h * .07f); lineTo(w * .61f, h * .25f); lineTo(w * .83f, h * .20f)
                lineTo(w * .76f, h * .43f); lineTo(w * .91f, h * .57f); lineTo(w * .67f, h * .65f)
                lineTo(w * .50f, h * .93f); lineTo(w * .33f, h * .65f); lineTo(w * .09f, h * .57f)
                lineTo(w * .24f, h * .43f); lineTo(w * .17f, h * .20f); lineTo(w * .39f, h * .25f); close()
            }
            drawPath(crown, Brush.linearGradient(listOf(Color.White.copy(alpha = .92f), tier[0], tier[1], tier[2]), start = Offset(w * .15f, h * .08f), end = Offset(w * .82f, h * .94f)))
            val crystal = Path().apply {
                moveTo(w * .50f, h * .19f); lineTo(w * .73f, h * .37f); lineTo(w * .64f, h * .70f)
                lineTo(w * .50f, h * .83f); lineTo(w * .36f, h * .70f); lineTo(w * .27f, h * .37f); close()
            }
            drawPath(crystal, Brush.linearGradient(tier, start = Offset(w * .18f, h * .08f), end = Offset(w * .82f, h * .92f)))
            val leftFacet = Path().apply {
                moveTo(w * .20f, h * .34f); lineTo(w * .50f, h * .43f); lineTo(w * .50f, h * .91f)
                lineTo(w * .32f, h * .76f); close()
            }
            drawPath(leftFacet, Color.White.copy(alpha = .34f))
            val rightFacet = Path().apply {
                moveTo(w * .50f, h * .43f); lineTo(w * .80f, h * .34f); lineTo(w * .68f, h * .76f)
                lineTo(w * .50f, h * .91f); close()
            }
            drawPath(rightFacet, Color(0xFF17204A).copy(alpha = .18f))
            drawLine(Color.White.copy(alpha = shimmer), Offset(w * .31f, h * .35f), Offset(w * .48f, h * .22f), strokeWidth = w * .045f)
            drawLine(Color.White.copy(alpha = .55f * shimmer), Offset(w * .50f, h * .25f), Offset(w * .50f, h * .78f), strokeWidth = w * .018f)
            drawCircle(Color.White.copy(alpha = shimmer), radius = w * .055f, center = Offset(w * .78f, h * .19f))
            drawCircle(tier[2].copy(alpha = .85f * shimmer), radius = w * .035f, center = Offset(w * .22f, h * .78f))
            drawCircle(tier[2].copy(alpha = shimmer), radius = w * .035f, center = Offset(w * .13f, h * .65f))
            drawLine(tier[2].copy(alpha = shimmer), Offset(w * .87f, h * .56f), Offset(w * .97f, h * .56f), strokeWidth = w * .035f)
            drawLine(tier[2].copy(alpha = shimmer), Offset(w * .92f, h * .51f), Offset(w * .92f, h * .61f), strokeWidth = w * .035f)
        }
        if (showLevel) {
            Box(Modifier.align(Alignment.BottomEnd).size(size * .42f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(size * .42f)) {
                    drawCircle(Brush.linearGradient(listOf(tier[2], tier[1])))
                    drawCircle(Color.White.copy(alpha = .95f), style = Stroke(width = size.toPx() * .025f))
                }
                Text(level.toString(), color = Color(0xFF18213B), fontSize = (size.value * .22f).sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}
