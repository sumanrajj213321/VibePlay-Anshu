package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GiftCatalogItem
import com.example.data.model.ModerationReport
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VipBadgePill
import com.example.ui.state.VibePlayViewModel
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet

@Composable
fun AdminPanelScreen(
    viewModel: VibePlayViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val users by viewModel.observeAdminUsers().collectAsStateWithLifecycle(initialValue = emptyList())
    val rooms by viewModel.observeAdminRooms().collectAsStateWithLifecycle(initialValue = emptyList())
    val reports by viewModel.observeAdminReports().collectAsStateWithLifecycle(initialValue = emptyList())
    val gifts by viewModel.observeAdminGifts().collectAsStateWithLifecycle(initialValue = emptyList())

    AdminPanelContent(
        users = users,
        rooms = rooms,
        reports = reports,
        gifts = gifts,
        onBack = onBack,
        onUpdateStatus = { uid, status -> viewModel.adminUpdateAccountStatus(uid, status) },
        onGrantVipCoins = { uid, vip, coins -> viewModel.adminGrantVipCoins(uid, vip, coins) },
        onToggleFeatured = { id, feat -> viewModel.adminToggleFeaturedRoom(id, feat) },
        onCloseRoom = { viewModel.adminCloseRoom(it) },
        onResolveReport = { id, status -> viewModel.adminResolveReport(id, status) },
        onSaveGift = { id, name, cat, cost, emoji, active ->
            viewModel.adminSaveGift(id, name, cat, cost, emoji, active)
        },
        onSeedDefaultGifts = { viewModel.adminSeedDefaultGifts() }
    )
}

@Composable
internal fun AdminPanelContent(
    users: List<UserProfile>,
    rooms: List<VoiceRoom>,
    reports: List<ModerationReport>,
    gifts: List<GiftCatalogItem>,
    onBack: () -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onGrantVipCoins: (String, Long, Long) -> Unit,
    onToggleFeatured: (String, Boolean) -> Unit,
    onCloseRoom: (String) -> Unit,
    onResolveReport: (String, String) -> Unit,
    onSaveGift: (giftId: String?, name: String, category: String, coinCost: Long, emoji: String, active: Boolean) -> Unit,
    onSeedDefaultGifts: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Rooms", "Reports", "Gifts")

    var grantTargetUser by remember { mutableStateOf<UserProfile?>(null) }
    var editingGift by remember { mutableStateOf<GiftCatalogItem?>(null) }
    var showGiftDialog by remember { mutableStateOf(false) }

    val liveRoomsCount = remember(rooms) { rooms.count { it.status == "LIVE" } }
    val openReportsCount = remember(reports) { reports.count { it.status == "OPEN" } }
    val activeGiftsCount = remember(gifts) { gifts.count { it.active } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Admin Moderation Console", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Users, Rooms, Reports & Economy", style = MaterialTheme.typography.labelSmall, color = VibeGold)
            }
        }

        // KPI Overview Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminKpiChip(label = "Users", value = "${users.size}", tint = VibeCyan, modifier = Modifier.weight(1f))
            AdminKpiChip(label = "Live Rooms", value = "$liveRoomsCount", tint = VibeEmerald, modifier = Modifier.weight(1f))
            AdminKpiChip(label = "Open Reports", value = "$openReportsCount", tint = VibeCoral, modifier = Modifier.weight(1f))
            AdminKpiChip(label = "Gifts", value = "$activeGiftsCount", tint = VibeGold, modifier = Modifier.weight(1f))
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    modifier = Modifier.testTag("admin_tab_${title.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            0 -> AdminUsersTab(
                users = users,
                onStatusChange = onUpdateStatus,
                onGrantVipCoins = { grantTargetUser = it }
            )
            1 -> AdminRoomsTab(
                rooms = rooms,
                onToggleFeatured = onToggleFeatured,
                onCloseRoom = onCloseRoom
            )
            2 -> AdminReportsTab(
                reports = reports,
                onResolve = onResolveReport,
                onSuspendUser = { uid -> onUpdateStatus(uid, "SUSPENDED") },
                onCloseRoom = onCloseRoom
            )
            else -> AdminGiftsTab(
                gifts = gifts,
                onToggleActive = { gift ->
                    onSaveGift(gift.id, gift.name, gift.category, gift.coinCost, gift.emoji, !gift.active)
                },
                onEditGift = { gift ->
                    editingGift = gift
                    showGiftDialog = true
                },
                onAddGift = {
                    editingGift = null
                    showGiftDialog = true
                },
                onSeedDefaults = onSeedDefaultGifts
            )
        }
    }

    grantTargetUser?.let { target ->
        var vipText by remember(target.userId) { mutableStateOf(target.vipLevel.toString()) }
        var coinsText by remember(target.userId) { mutableStateOf(target.coinBalance.toString()) }
        AlertDialog(
            onDismissRequest = { grantTargetUser = null },
            title = { Text("Manage ${target.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vipText,
                        onValueChange = { vipText = it.filter(Char::isDigit).take(1) },
                        label = { Text("VIP Level (0 - 5)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = coinsText,
                        onValueChange = { coinsText = it.filter(Char::isDigit).take(12) },
                        label = { Text("Coin Balance") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val vip = vipText.toLongOrNull()?.coerceIn(0L, 5L) ?: target.vipLevel
                    val coins = coinsText.toLongOrNull()?.coerceAtLeast(0L) ?: target.coinBalance
                    onGrantVipCoins(target.userId, vip, coins)
                    grantTargetUser = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { grantTargetUser = null }) { Text("Cancel") } }
        )
    }

    if (showGiftDialog) {
        val currentEditing = editingGift
        var name by remember(currentEditing?.id) { mutableStateOf(currentEditing?.name ?: "") }
        var emoji by remember(currentEditing?.id) { mutableStateOf(currentEditing?.emoji ?: "\uD83C\uDF81") }
        var costText by remember(currentEditing?.id) { mutableStateOf((currentEditing?.coinCost ?: 100L).toString()) }
        var category by remember(currentEditing?.id) { mutableStateOf(currentEditing?.category ?: "Popular") }
        AlertDialog(
            onDismissRequest = { showGiftDialog = false },
            title = { Text(if (currentEditing == null) "Add Gift to Catalog" else "Edit ${currentEditing.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Gift Name") }, singleLine = true)
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it.take(8) }, label = { Text("Emoji") }, singleLine = true)
                    OutlinedTextField(value = costText, onValueChange = { costText = it.filter(Char::isDigit).take(6) }, label = { Text("Coin Cost") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Popular/Luxury/Romantic/Party)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val safeCat = if (category in listOf("Popular", "Luxury", "Romantic", "Party")) category else "Popular"
                    onSaveGift(
                        currentEditing?.id,
                        name.ifBlank { "Special Gift" },
                        safeCat,
                        costText.toLongOrNull() ?: 100L,
                        emoji.ifBlank { "\uD83C\uDF81" },
                        currentEditing?.active ?: true
                    )
                    showGiftDialog = false
                }) { Text("Save Gift") }
            },
            dismissButton = { TextButton(onClick = { showGiftDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminKpiChip(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tint)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<UserProfile>,
    onStatusChange: (String, String) -> Unit,
    onGrantVipCoins: (UserProfile) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredUsers = remember(users, query) {
        if (query.isBlank()) users
        else users.filter {
            it.username.contains(query, ignoreCase = true) ||
                it.vibeId.contains(query, ignoreCase = true) ||
                it.accountStatus.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search users by name, Vibe ID, or status...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_users_search")
            )
        }
        items(filteredUsers, key = { it.userId }) { user ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VibeAvatar(user.avatarUrl, user.username, size = 42.dp, vipLevel = user.vipLevel)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(user.username, fontWeight = FontWeight.Bold)
                                VipBadgePill(user.vipLevel)
                            }
                            Text("${user.vibeId} · ${user.accountStatus} · ${user.coinBalance} coins", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onGrantVipCoins(user) }, modifier = Modifier.weight(1f)) {
                            Text("VIP / Coins", style = MaterialTheme.typography.labelSmall)
                        }
                        if (user.accountStatus != "SUSPENDED") {
                            OutlinedButton(onClick = { onStatusChange(user.userId, "SUSPENDED") }, modifier = Modifier.weight(1f)) {
                                Text("Suspend", style = MaterialTheme.typography.labelSmall, color = VibeGold)
                            }
                        }
                        if (user.accountStatus != "BANNED") {
                            OutlinedButton(onClick = { onStatusChange(user.userId, "BANNED") }, modifier = Modifier.weight(1f)) {
                                Text("Ban", style = MaterialTheme.typography.labelSmall, color = VibeCoral)
                            }
                        } else {
                            OutlinedButton(onClick = { onStatusChange(user.userId, "ACTIVE") }, modifier = Modifier.weight(1f)) {
                                Text("Restore", style = MaterialTheme.typography.labelSmall, color = VibeEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRoomsTab(
    rooms: List<VoiceRoom>,
    onToggleFeatured: (String, Boolean) -> Unit,
    onCloseRoom: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredRooms = remember(rooms, query) {
        if (query.isBlank()) rooms
        else rooms.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.hostName.contains(query, ignoreCase = true) ||
                it.status.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search rooms by title, host, or status...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_rooms_search")
            )
        }
        items(filteredRooms, key = { it.id }) { room ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${room.title} (${room.status})", fontWeight = FontWeight.Bold)
                    Text("Host: ${room.hostName} · Category: ${room.category} · Listeners: ${room.listenerCount}", style = MaterialTheme.typography.labelSmall)
                    if (room.status == "LIVE") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onToggleFeatured(room.id, !room.featured) }, modifier = Modifier.weight(1f)) {
                                Text(if (room.featured) "Unfeature" else "Feature Room")
                            }
                            Button(
                                onClick = { onCloseRoom(room.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = VibeCoral),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close Room")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReportsTab(
    reports: List<ModerationReport>,
    onResolve: (String, String) -> Unit,
    onSuspendUser: (String) -> Unit,
    onCloseRoom: (String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    val filteredReports = remember(reports, statusFilter) {
        if (statusFilter == "ALL") reports
        else reports.filter { it.status == statusFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ALL", "OPEN", "ACTION_TAKEN", "DISMISSED")) { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text(filter.replace("_", " ")) }
                    )
                }
            }
        }
        if (filteredReports.isEmpty()) {
            item { Text("No matching moderation reports.", style = MaterialTheme.typography.bodySmall) }
        }
        items(filteredReports, key = { it.id }) { rep ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, if (rep.status == "OPEN") VibeCoral.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("[${rep.targetType}] ${rep.targetName} · ${rep.status}", fontWeight = FontWeight.Bold)
                    Text("Reason: ${rep.reason}", style = MaterialTheme.typography.bodySmall, color = VibeCyan)
                    if (rep.details.isNotBlank()) {
                        Text(rep.details, style = MaterialTheme.typography.bodySmall)
                    }
                    if (rep.status == "OPEN") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onResolve(rep.id, "DISMISSED") }, modifier = Modifier.weight(1f)) {
                                Text("Dismiss")
                            }
                            Button(onClick = { onResolve(rep.id, "ACTION_TAKEN") }, modifier = Modifier.weight(1f)) {
                                Text("Mark Resolved")
                            }
                        }
                        if (rep.targetId.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    if (rep.targetType == "ROOM") {
                                        onCloseRoom(rep.targetId)
                                    } else {
                                        onSuspendUser(rep.targetId)
                                    }
                                    onResolve(rep.id, "ACTION_TAKEN")
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeCoral),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (rep.targetType == "ROOM") "Close Reported Room" else "Suspend Reported User")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminGiftsTab(
    gifts: List<GiftCatalogItem>,
    onToggleActive: (GiftCatalogItem) -> Unit,
    onEditGift: (GiftCatalogItem) -> Unit,
    onAddGift: () -> Unit,
    onSeedDefaults: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAddGift,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Gift")
                }
                OutlinedButton(
                    onClick = onSeedDefaults,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_seed_gifts_button")
                ) {
                    Text("Seed Defaults")
                }
            }
        }
        items(gifts, key = { it.id }) { gift ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${gift.emoji} ${gift.name} · ${gift.coinCost} Coins", fontWeight = FontWeight.Medium)
                        Text(gift.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onEditGift(gift) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit gift", modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(onClick = { onToggleActive(gift) }) {
                            Text(if (gift.active) "Active" else "Hidden")
                        }
                    }
                }
            }
        }
    }
}
