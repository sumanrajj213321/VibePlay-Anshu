package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CharmProgression
import com.example.data.model.UserProfile

private val CharmPurple = Color(0xFF8955F6)
private val CharmInk = Color(0xFF25243A)
private val CharmMuted = Color(0xFF9699B3)
private val CharmGold = Color(0xFFFFD84D)

@Composable
fun CharmLevelsScreen(
    profile: UserProfile,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val charms = CharmProgression.fromReceivedGiftCoins(profile.totalGiftCoinsReceived)
    val level = CharmProgression.levelFor(charms)
    val nextGoal = CharmProgression.nextMilestone(charms)
    val previousGoal = CharmProgression.previousMilestone(charms)
    val progress = if (nextGoal == null) 1f else
        ((charms - previousGoal).toFloat() / (nextGoal - previousGoal).toFloat()).coerceIn(0f, 1f)
    val icons = listOf("⭐", "💎", "👑", "👑", "♛", "🌟", "🌟", "👑", "👑", "👑", "👑", "👑", "♛", "♛", "♛", "🌟", "🌟", "🌟", "🌟", "🌟", "🌟")

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF7545F6), Color(0xFFB48AFF), Color.White)))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Charm Levels", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.width(48.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { selectedTab = 0 }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Charm Levels", color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = .65f), fontWeight = FontWeight.Bold)
                            if (selectedTab == 0) Box(Modifier.padding(top = 3.dp).size(width = 30.dp, height = 3.dp).clip(CircleShape).background(Color(0xFF25DDF5)))
                        }
                    }
                    TextButton(onClick = { selectedTab = 1 }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active Levels", color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = .65f), fontWeight = FontWeight.Bold)
                            if (selectedTab == 1) Box(Modifier.padding(top = 3.dp).size(width = 30.dp, height = 3.dp).clip(CircleShape).background(Color(0xFF25DDF5)))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 18.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.size(76.dp).clip(CircleShape).background(Color(0xFFEDEAF8))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(profile.username, color = CharmInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(5.dp))
                            Text("Charm: $charms", color = CharmMuted, fontSize = 17.sp)
                        }
                    }
                }
                if (nextGoal != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp)
                            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                            .background(Color(0xFFDCE3FF).copy(alpha = .92f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Need ${(nextGoal - charms).coerceAtLeast(0)} to Upgrade Charm to ${icons.getOrElse(level.toInt()) { "✨" }}", color = Color(0xFF6255B5), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(4.dp).clip(CircleShape), color = CharmPurple, trackColor = Color.White)
                    }
                }
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Charm Levels", fontSize = 21.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Charm represents a player's popularity. Players performing well in rooms receive gifts that increase their Charm.", color = CharmMuted, fontSize = 15.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Reaching Charm goals unlocks special icons. Your Charm icon highlights your popularity.", color = CharmMuted, fontSize = 15.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFFF5F3F8)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icons.take(7).forEach { Text(it, fontSize = 27.sp) }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("How to Increase Charm", fontSize = 21.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF1E9FF)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = CharmPurple, modifier = Modifier.size(31.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Receive Gifts", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("10 Gold Coins worth of gifts = 1 Charm", color = CharmMuted, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Charm Icons", fontSize = 21.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF5F3F8), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("Charm", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Text("Icon", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
                itemsIndexed(CharmProgression.milestones) { index, milestone ->
                    Row(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF0EEF4), RoundedCornerShape(12.dp)).padding(horizontal = 22.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatCharmGoal(milestone), color = CharmMuted, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icons.getOrElse(index) { "✨" }, fontSize = 25.sp)
                            Text("${index + 1}", color = Color(0xFFE49A38), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Active Levels", fontSize = 21.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    Text("Charm icons you have unlocked", color = CharmMuted, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                }
                itemsIndexed(CharmProgression.milestones.filter { charms >= it }) { index, milestone ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF6F4FA), RoundedCornerShape(14.dp)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icons.getOrElse(CharmProgression.milestones.indexOf(milestone)) { "✨" }, fontSize = 28.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Charm Level ${CharmProgression.milestones.indexOf(milestone) + 1}", fontWeight = FontWeight.Bold, color = CharmInk)
                            Text("${formatCharmGoal(milestone)} Charms", color = CharmMuted)
                        }
                        Text("ACTIVE", color = Color(0xFF2AA876), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (charms < (CharmProgression.milestones.firstOrNull() ?: 1L)) {
                    item { Text("No Charm Level unlocked yet. Receive room gifts to unlock your first icon.", color = CharmMuted) }
                }
            }
        }
    }
}

private fun formatCharmGoal(value: Long): String = when {
    value >= 1_000_000L -> "${value / 1_000_000L}m"
    value >= 1_000L -> "${value / 1_000L}k"
    else -> value.toString()
}
