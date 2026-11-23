package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class Fruit(val color: Color) { CHERRY(Color(0xFFFF397D)), GRAPE(Color(0xFFB34CFF)), LEMON(Color(0xFFFFD83D)), ORANGE(Color(0xFFFF9B28)), MELON(Color(0xFFFF5365)), KIWI(Color(0xFF9AE34B)) }

@Composable
fun FruitTreasureScreen(onBack: () -> Unit) {
    var reels by remember { mutableStateOf(List(9) { Fruit.entries.random() }) }
    var spinning by remember { mutableStateOf(false) }
    var musicOn by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(true) }
    var score by remember { mutableIntStateOf(0) }
    var spins by remember { mutableIntStateOf(0) }
    var dailyBonusClaimed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Match 3 custom fruits in a line to earn points") }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }
    DisposableEffect(Unit) { onDispose { tone.release() } }
    LaunchedEffect(musicOn, spinning) {
        if (musicOn) {
            val notes = intArrayOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_3)
            var i = 0
            while (true) { if (!spinning) tone.startTone(notes[i++ % notes.size], 120); delay(900) }
        }
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF080B46), Color(0xFF17045B), Color(0xFF080B46)))).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("VibePlay", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(onClick = { musicOn = !musicOn }) { Icon(if (musicOn) Icons.Filled.MusicNote else Icons.Filled.MusicOff, "Music toggle", tint = Color(0xFF62E7FF)) }
                IconButton(onClick = { soundOn = !soundOn }) { Icon(if (soundOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, "Sound toggle", tint = Color(0xFF62E7FF)) }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FRUIT", color = Color(0xFFFF54B8), style = MaterialTheme.typography.displaySmall)
            Text("TREASURE", color = Color(0xFFFFC94A), style = MaterialTheme.typography.headlineLarge)
            Text("SCORE  $score", color = Color(0xFFFFD95A), style = MaterialTheme.typography.titleMedium)
        }
        Surface(color = Color(0xFF111B72), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28DFFF))) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TREASURE CHEST", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text("$spins / 10 spins", color = Color(0xFF8DEBFF), style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(progress = { (spins.coerceAtMost(10) / 10f) }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF42E7FF), trackColor = Color(0xFF29317F))
                Text(if (spins >= 10) "Treasure milestone reached!" else "Complete 10 free spins to reach the treasure milestone", color = Color(0xFFB7D9FF), style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("DAILY BONUS", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold); Text(if (dailyBonusClaimed) "Claimed today" else "One free daily score bonus", color = Color(0xFFB7D9FF), style = MaterialTheme.typography.labelSmall) }
            Button(onClick = { if (!dailyBonusClaimed) { score += 25; dailyBonusClaimed = true; message = "Daily bonus claimed: +25 points" } }, enabled = !dailyBonusClaimed, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF236BCE))) { Text(if (dailyBonusClaimed) "CLAIMED" else "+25 POINTS") }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF10094C), RoundedCornerShape(22.dp)).border(3.dp, Color(0xFFB64DFF), RoundedCornerShape(22.dp)).padding(8.dp)) {
            for (row in 0..2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        Box(Modifier.weight(1f).aspectRatio(1f).background(Color(0xFF17105D), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { FruitArtwork(reels[index], Modifier.fillMaxSize().padding(8.dp)) }
                    }
                }
                if (row < 2) Spacer(Modifier.height(4.dp))
            }
        }
        Surface(color = Color(0xFF210B65), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDA4DFF))) { Text(message, Modifier.fillMaxWidth().padding(14.dp), color = Color.White) }
        Button(onClick = { if (!spinning) spinning = true }, enabled = !spinning, modifier = Modifier.fillMaxWidth().height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24D900)), shape = RoundedCornerShape(32.dp)) { Text(if (spinning) "SPINNING..." else "SPIN", style = MaterialTheme.typography.headlineMedium) }
        LaunchedEffect(spinning) {
            if (spinning) {
                message = "Fruit reels are spinning..."
                repeat(12) { reels = List(9) { Fruit.entries.random() }; if (soundOn) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 55); delay(115) }
                spins += 1
                spinning = false
                val lines = listOf(listOf(0,1,2), listOf(3,4,5), listOf(6,7,8), listOf(0,4,8), listOf(2,4,6))
                val wins = lines.count { reels[it[0]] == reels[it[1]] && reels[it[1]] == reels[it[2]] }
                if (wins > 0) { score += wins * 10; message = "Fruit match! +${wins * 10} points"; if (soundOn) tone.startTone(ToneGenerator.TONE_PROP_ACK, 240) } else message = "Try again! Free play — no coins deducted."
            }
        }
        Text("FREE ARCADE MODE • No coins are used", color = Color(0xFFB7D9FF), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FruitArtwork(fruit: Fruit, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val x = size.width / 2f; val y = size.height / 2f; val r = size.minDimension * .29f
        drawCircle(fruit.color.copy(alpha = .22f), r * 1.45f, Offset(x, y))
        when (fruit) {
            Fruit.CHERRY -> { drawCircle(fruit.color, r*.72f, Offset(x-r*.55f,y+r*.25f)); drawCircle(fruit.color, r*.72f, Offset(x+r*.55f,y+r*.25f)); drawLine(Color(0xFF65E65A),Offset(x,y-r*.25f),Offset(x-r*.18f,y-r*1.55f),r*.12f); drawLine(Color(0xFF65E65A),Offset(x,y-r*.25f),Offset(x+r*.65f,y-r*1.4f),r*.12f) }
            Fruit.GRAPE -> { listOf(-.65f to -.3f,0f to -.55f,.65f to -.3f,-.85f to .4f,-.15f to .4f,.55f to .4f,-.5f to 1.05f,.2f to 1.05f).forEach { (a,b) -> drawCircle(fruit.color,r*.45f,Offset(x+r*a,y+r*b)) }; drawOval(Color(0xFF68E95B),androidx.compose.ui.geometry.Rect(x-r*.1f,y-r*1.75f,x+r*.8f,y-r*1.05f)) }
            Fruit.LEMON, Fruit.KIWI -> { drawOval(fruit.color,androidx.compose.ui.geometry.Rect(x-r*1.1f,y-r*.8f,x+r*1.1f,y+r*.8f)); drawOval(Color.White.copy(alpha=.3f),androidx.compose.ui.geometry.Rect(x-r*.62f,y-r*.55f,x-r*.2f,y-r*.08f)); drawOval(Color(0xFF65E65A),androidx.compose.ui.geometry.Rect(x+r*.05f,y-r*1.4f,x+r*.9f,y-r*.75f)) }
            Fruit.ORANGE -> { drawCircle(fruit.color,r,Offset(x,y)); drawCircle(Color.White.copy(alpha=.35f),r*.2f,Offset(x-r*.35f,y-r*.35f)); drawLine(Color(0xFF65E65A),Offset(x,y-r*.9f),Offset(x+r*.08f,y-r*1.35f),r*.12f) }
            Fruit.MELON -> { val p=Path().apply { moveTo(x-r*1.15f,y-r*.4f); quadraticTo(x,y+r*1.5f,x+r*1.15f,y-r*.4f); close() }; drawPath(p,fruit.color); drawPath(p,Color(0xFF54D94B),style=Stroke(width=r*.24f)); for (i in -1..1) drawOval(Color(0xFF4A173C),androidx.compose.ui.geometry.Rect(x+r*i*.42f-r*.05f,y+r*.1f,x+r*i*.42f+r*.05f,y+r*.35f)) }
        }
        drawCircle(Color.White.copy(alpha=.55f),r*.12f,Offset(x-r*.38f,y-r*.4f))
    }
}