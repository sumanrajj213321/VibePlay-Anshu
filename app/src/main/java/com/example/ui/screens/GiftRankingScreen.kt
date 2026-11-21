package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CardGiftcard
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
import com.example.ui.components.VibeAvatar
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink

@Composable
fun GiftRankingScreen(
    users: List<UserProfile>,
    onBack: () -> Unit,
    onOpenUserProfile: (UserProfile) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val rankedUsers = remember(users, selectedTab) {
        users.filter {
            if (selectedTab == 0) it.totalGiftCoinsSent > 0L else it.totalGiftCoinsReceived > 0L
        }.sortedByDescending {
            if (selectedTab == 0) it.totalGiftCoinsSent else it.totalGiftCoinsReceived
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Total Ranking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Top Gifters") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Top Receivers") })
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
                    val total = if (selectedTab == 0) user.totalGiftCoinsSent else user.totalGiftCoinsReceived
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
                                vipLevel = user.vipLevel
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(if (selectedTab == 0) "Top Gifter" else "Top Receiver", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = if (selectedTab == 0) VibePink else VibeGold, modifier = Modifier.size(16.dp))
                                    Text("%,d".format(total), fontWeight = FontWeight.Bold)
                                }
                                Text("Gift coins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
