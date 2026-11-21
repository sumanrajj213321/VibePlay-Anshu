package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VibeShopCatalog
import com.example.data.model.ShopCosmeticItem
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibeViolet

@Composable
fun ShopScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onUpgradeVip: (Int) -> Unit,
    onPurchase: (String, String) -> Unit,
    onEquip: (String, String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("VibePlay Shop", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text("${profile.coinBalance} Coins", color = VibeGold, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("VIP", "Frames", "Titles").forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(label, fontWeight = FontWeight.Bold) }
                )
            }
        }
        Text(
            when (selectedTab) {
                0 -> "Upgrade your VIP level with coins"
                1 -> "Premium avatar frames · shown around your profile photo"
                else -> "Bold highlighted titles · shown only on your profile"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> items(VibeCatalogDefaults.vipTiers, key = { "vip_${it.level}" }) { tier ->
                    val color = Color(tier.badgeColorHex)
                    val owned = profile.vipLevel >= tier.level
                    ShopCard(
                        title = tier.title,
                        subtitle = tier.perks.joinToString(" · "),
                        price = tier.coinPrice,
                        accent = color,
                        status = if (owned) "Owned" else if (profile.coinBalance >= tier.coinPrice) "Purchase" else "Need ${tier.coinPrice - profile.coinBalance} more",
                        enabled = !owned && profile.coinBalance >= tier.coinPrice,
                        onClick = { onUpgradeVip(tier.level) },
                        icon = "👑"
                    )
                }
                1 -> items(VibeShopCatalog.frames, key = { it.id }) { item ->
                    val owned = item.id in profile.ownedFrameIds
                    val equipped = profile.activeFrameId == item.id
                    ShopCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        price = item.coinPrice,
                        accent = Color(item.colorArgb),
                        status = when { equipped -> "Equipped"; owned -> "Equip"; else -> "Purchase" },
                        enabled = !equipped && (owned || profile.coinBalance >= item.coinPrice),
                        onClick = { if (owned) onEquip("FRAME", item.id) else onPurchase("FRAME", item.id) },
                        icon = "✨"
                    )
                }
                else -> items(VibeShopCatalog.titles, key = { it.id }) { item ->
                    val owned = item.id in profile.ownedTitleIds
                    val equipped = profile.activeTitleId == item.id
                    ShopCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        price = item.coinPrice,
                        accent = Color(item.colorArgb),
                        status = when { equipped -> "Equipped"; owned -> "Equip"; else -> "Purchase" },
                        enabled = !equipped && (owned || profile.coinBalance >= item.coinPrice),
                        onClick = { if (owned) onEquip("TITLE", item.id) else onPurchase("TITLE", item.id) },
                        icon = "🏷️"
                    )
                }
            }
            if (selectedTab == 1 && profile.activeFrameId.isNotBlank()) {
                item {
                    Button(onClick = { onEquip("FRAME", null) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VibeViolet)) {
                        Text("Remove equipped frame")
                    }
                }
            }
            if (selectedTab == 2 && profile.activeTitleId.isNotBlank()) {
                item {
                    Button(onClick = { onEquip("TITLE", null) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VibeViolet)) {
                        Text("Remove profile title")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCard(
    title: String,
    subtitle: String,
    price: Long,
    accent: Color,
    status: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$price Coins", style = MaterialTheme.typography.labelLarge, color = VibeGold, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
            ) {
                Text(status, fontWeight = FontWeight.Bold)
            }
        }
    }
}
