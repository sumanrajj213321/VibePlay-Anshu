package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VibeCoinIcon
import com.example.ui.theme.VibeGold
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GiftRankingScreen(
    users: List<UserProfile>,
    onBack: () -> Unit,
    onOpenUserProfile: (UserProfile) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPeriod by remember { mutableIntStateOf(2) }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val weekKey = remember {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
    fun score(user: UserProfile): Long = when (selectedPeriod) {
        0 -> if (user.dailyGiftCoinsDate == todayKey) {
            if (selectedTab == 0) user.dailyGiftCoinsSent else user.dailyGiftCoinsReceived
        } else 0L
        1 -> if (user.weeklyGiftCoinsWeek == weekKey) {
            if (selectedTab == 0) user.weeklyGiftCoinsSent else user.weeklyGiftCoinsReceived
        } else 0L
        else -> if (selectedTab == 0) user.totalGiftCoinsSent else user.totalGiftCoinsReceived
    }
    val rankedUsers = remember(users, selectedTab, selectedPeriod, todayKey, weekKey) {
        users.filter { score(it) > 0L }.sortedByDescending { score(it) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Ranking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        TabRow(selectedTabIndex = selectedPeriod) {
            Tab(selected = selectedPeriod == 0, onClick = { selectedPeriod = 0 }, text = { Text("Daily") })
            Tab(selected = selectedPeriod == 1, onClick = { selectedPeriod = 1 }, text = { Text("Weekly") })
            Tab(selected = selectedPeriod == 2, onClick = { selectedPeriod = 2 }, text = { Text("Total") })
        }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Gifters") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Receivers") })
        }
        if (rankedUsers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No gift ranking data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(rankedUsers, key = { _, user -> user.userId }) { index, user ->
                    val total = score(user)
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenUserProfile(user) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(if (index == 0) VibeGold.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", fontWeight = FontWeight.Bold, color = if (index == 0) VibeGold else MaterialTheme.colorScheme.onSurface)
                            }
                            VibeAvatar(
                                avatarUrl = user.avatarUrl,
                                username = user.username,
                                size = 52.dp,
                                vipLevel = if (user.isVipActive) user.vipLevel else 0L,
                                frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == user.activeFrameId && user.isFrameActive(it.id) }?.colorArgb
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(if (selectedTab == 0) "Top Gifter" else "Top Receiver", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    VibeCoinIcon(modifier = Modifier.size(18.dp))
                                    Text("%,d".format(total), fontWeight = FontWeight.Bold)
                                }
                                Text("Coins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}