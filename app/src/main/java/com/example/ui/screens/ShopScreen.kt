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
import androidx.compose.foundation.layout.size
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
import com.example.ui.components.VibeCoinIcon
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
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
    // Open the Shop directly on Frames so VIP users immediately see their matching Golden Banner.
    var selectedTab by remember { mutableIntStateOf(1) }
    // Normalize legacy VIP values to the same 1–8 range used by Profile.
    val effectiveVipLevel = if (profile.isVipActive) profile.vipLevel.coerceIn(1L, 8L) else 0L
    // Show only the Golden Banner matching this user's active VIP tier.
    // Do not expose other VIP tiers in this user's Shop.
    val visibleFrames = VibeShopCatalog.frames
        .filter { item ->
            when {
                item.id.startsWith("vip_gold_banner_") -> {
                    val requiredVipLevel = item.id.substringAfterLast('_').toLongOrNull()
                    requiredVipLevel != null && profile.isVipActive && effectiveVipLevel == requiredVipLevel
                }
                item.id.startsWith("vip_dragon_") -> false
                else -> true
            }
        }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { VibeCoinIcon(modifier = Modifier.size(20.dp)); Text("${profile.coinBalance} Coins", color = VibeGold, fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("VIP", "Frames").forEachIndexed { index, label ->
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
                else -> "Premium avatar frames · shown around your profile photo"
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
                    val owned = profile.isVipActive && profile.vipLevel >= tier.level
                    ShopCard(
                        title = tier.title,
                        subtitle = tier.perks.joinToString(" · ") + " · Valid for 30 days",
                        price = tier.coinPrice,
                        accent = color,
                        status = if (owned) "Owned" else if (profile.coinBalance >= tier.coinPrice) "Purchase" else "Need ${tier.coinPrice - profile.coinBalance} more",
                        enabled = !owned && profile.coinBalance >= tier.coinPrice,
                        onClick = { onUpgradeVip(tier.level) },
                        icon = "",
                        vipLevel = tier.level
                    )
                }
                1 -> items(visibleFrames.chunked(4)) { frameRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        frameRow.forEach { item ->
                            val owned = profile.isFrameActive(item.id)
                            val equipped = profile.activeFrameId == item.id && owned
                            val requiredVipLevel = if (item.id.startsWith("vip_gold_banner_")) {
                                item.id.substringAfterLast('_').toLongOrNull()
                            } else null
                            val hasRequiredVip = requiredVipLevel == null ||
                                (profile.isVipActive && effectiveVipLevel == requiredVipLevel)
                            FrameGridCard(
                                modifier = Modifier.weight(1f),
                                title = item.title,
                                price = item.coinPrice,
                                accent = Color(item.colorArgb),
                                status = when {
                                    equipped -> "Equipped"
                                    owned -> "Equip"
                                    !hasRequiredVip && requiredVipLevel == effectiveVipLevel -> "VIP expired"
                                    !hasRequiredVip -> "Only VIP $requiredVipLevel"
                                    else -> "Buy"
                                },
                                enabled = !equipped && hasRequiredVip &&
                                    (owned || profile.coinBalance >= item.coinPrice),
                                onClick = { if (owned) onEquip("FRAME", item.id) else onPurchase("FRAME", item.id) },
                                avatarUrl = profile.avatarUrl,
                                frameColorArgb = item.colorArgb
                            )
                        }
                        repeat(4 - frameRow.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            if (selectedTab == 1 && profile.isFrameActive(profile.activeFrameId)) {
                item {
                    Button(onClick = { onEquip("FRAME", null) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VibeViolet)) {
                        Text("Remove equipped frame")
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameGridCard(
    modifier: Modifier = Modifier,
    title: String,
    price: Long,
    accent: Color,
    status: String,
    enabled: Boolean,
    onClick: () -> Unit,
    avatarUrl: String,
    frameColorArgb: Long
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            VibeAvatar(
                avatarUrl = avatarUrl,
                username = "Preview",
                size = 48.dp,
                frameColorArgb = frameColorArgb
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                VibeCoinIcon(modifier = Modifier.size(14.dp))
                Text("$price", style = MaterialTheme.typography.labelSmall, color = VibeGold, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
            ) {
                Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
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
    icon: String,
    vipLevel: Int? = null,
    previewAvatarUrl: String = "",
    previewFrameColorArgb: Long? = null
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
            if (previewFrameColorArgb != null) {
                VibeAvatar(
                    avatarUrl = previewAvatarUrl,
                    username = "Preview",
                    size = 58.dp,
                    frameColorArgb = previewFrameColorArgb
                )
            } else if (vipLevel != null) {
                VipBadgePill(vipLevel = vipLevel.toLong())
            } else {
                Text(icon, style = MaterialTheme.typography.headlineMedium)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { VibeCoinIcon(modifier = Modifier.size(18.dp)); Text("$price Coins", style = MaterialTheme.typography.labelLarge, color = VibeGold, fontWeight = FontWeight.Bold) }
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
