package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import com.example.ui.components.CharmEmblem
import com.example.ui.components.VibeCoinIcon
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GiftCatalogItem
import com.example.data.model.RoomCategory
import com.example.data.model.RoomCoverPreset
import com.example.data.model.RoomListener
import com.example.data.model.RoomOnlineMember
import com.example.data.model.SpeakerRequest
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.example.ui.components.GiftCatalogArtwork
import com.example.ui.components.PreloadGiftVideo
import com.example.ui.components.VibeAvatar
import com.example.ui.components.coverDrawableRes
import com.example.ui.components.VipBadgePill
import com.example.ui.theme.VibeCoral
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomGiftStoreSheet(
    currentUser: UserProfile,
    stage: VoiceStage,
    gifts: List<GiftCatalogItem>,
    initialRecipientId: String? = null,
    onDismiss: () -> Unit,
    onSendGift: (recipientId: String, recipientName: String, gift: GiftCatalogItem, quantity: Long) -> Unit
) {
    val stageRecipients = remember(stage) {
        (listOf(stage.hostSeat) + stage.speakerSeats).filter { it.isOccupied }
    }
    var selectedRecipient by remember(stageRecipients, initialRecipientId) {
        mutableStateOf(
            stageRecipients.firstOrNull { it.occupantId == initialRecipientId }
                ?: stageRecipients.firstOrNull()
        )
    }
    val giftOptions = remember(gifts) { gifts.distinctBy { it.id } }
    val giftCategories = remember { listOf("All", "Daily", "Love", "Premium") }
    var selectedCategory by remember { mutableStateOf("All") }
    val visibleGifts = remember(giftOptions, selectedCategory) {
        if (selectedCategory == "All") giftOptions
        else giftOptions.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
    var selectedGift by remember(gifts) { mutableStateOf(gifts.firstOrNull()) }
    LaunchedEffect(visibleGifts) {
        if (selectedGift !in visibleGifts) selectedGift = visibleGifts.firstOrNull()
    }
    var selectedQuantity by remember { mutableStateOf(1L) }
    PreloadGiftVideo(selectedGift)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Send Virtual Gift", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = VibeGold.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        VibeCoinIcon(modifier = Modifier.size(22.dp))
                        Column {
                            Text("${currentUser.coinBalance}", style = MaterialTheme.typography.titleMedium, color = VibeGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text("Recipient on Stage", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stageRecipients, key = { it.seatIndex }) { seat ->
                    val selected = selectedRecipient?.occupantId == seat.occupantId
                    FilterChip(
                        selected = selected,
                        onClick = { selectedRecipient = seat },
                        label = {
                            Text(if (seat.isHostSeat) "Host · ${seat.occupantName}" else seat.occupantName)
                        }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(giftCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("gift_cat_${cat.lowercase()}")
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleGifts, key = { it.id }) { gift ->
                    val isSelected = selectedGift?.id == gift.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) VibeViolet.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) VibePink else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clickable { selectedGift = gift }
                            .testTag("gift_item_${gift.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GiftCatalogArtwork(gift = gift, modifier = Modifier.size(48.dp))
                            Text(
                                text = gift.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                VibeCoinIcon(modifier = Modifier.size(12.dp))
                                Text(
                                    text = "${gift.coinCost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibeGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Combo Quantity", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(1L, 5L, 10L, 50L, 99L)) { qty ->
                        FilterChip(
                            selected = selectedQuantity == qty,
                            onClick = { selectedQuantity = qty },
                            label = { Text("${qty}x") },
                            modifier = Modifier.testTag("gift_qty_$qty")
                        )
                    }
                }
            }

            val giftToSend = selectedGift
            val recipientToSend = selectedRecipient
            val totalCost = (giftToSend?.coinCost ?: 0L) * selectedQuantity
            val canAfford = giftToSend != null && currentUser.coinBalance >= totalCost

            if (giftToSend != null) {
                val previewCharms = totalCost / com.example.data.model.CharmProgression.COINS_PER_CHARM
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF102B3A),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF35F4E5), Color(0xFF8B72FF))))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CharmEmblem(level = 1, size = 22.dp)
                        Text("Get +$previewCharms ${if (previewCharms == 1L) "Charm" else "Charms"}", color = Color(0xFF65FFF0), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Text("10 Gold Coins = 1 Charm", color = Color(0xFFB7D7DF), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Button(
                onClick = {
                    if (giftToSend != null && recipientToSend != null) {
                        onSendGift(recipientToSend.occupantId, recipientToSend.occupantName, giftToSend, selectedQuantity)
                        onDismiss()
                    }
                },
                enabled = canAfford && recipientToSend != null,
                colors = ButtonDefaults.buttonColors(containerColor = VibePink),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_send_gift_button")
            ) {
                val qtyPrefix = if (selectedQuantity > 1L) "${selectedQuantity}x " else ""
                Text(
                    text = when {
                        giftToSend == null || recipientToSend == null -> "Select Gift & Recipient"
                        !canAfford -> "Insufficient Coins ($totalCost needed)"
                        else -> "Send $qtyPrefix${giftToSend.name} ($totalCost Coins)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostSettingsSheet(
    room: VoiceRoom,
    stage: VoiceStage,
    isOwner: Boolean = true,
    onDismiss: () -> Unit,
    onSaveSettings: (title: String, announcement: String, category: String, coverKey: String, keepCustomCover: Boolean, locked: Boolean, newPassword: String?) -> Unit,
    onUploadCover: (Uri) -> Unit = {},
    onMuteAllSpeakers: () -> Unit = {},
    onTransferOwnership: (VoiceSeat) -> Unit,
    onEndRoom: () -> Unit,
    onUnblockRoomMember: (String) -> Unit = {}
) {
    var title by remember(room.title) { mutableStateOf(room.title) }
    var announcement by remember(room.announcement) { mutableStateOf(room.announcement) }
    var selectedCategory by remember(room.category) { mutableStateOf(RoomCategory.fromString(room.category)) }
    var selectedCoverKey by remember(room.coverKey) { mutableStateOf(room.coverKey) }
    var keepCustomCover by remember(room.coverUrl) { mutableStateOf(room.coverUrl.isNotBlank()) }
    var locked by remember(room.locked) { mutableStateOf(room.locked) }
    var changePassword by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var transferTarget by remember { mutableStateOf<VoiceSeat?>(null) }
    var confirmEndRoom by remember { mutableStateOf(false) }
    var pendingRoomCoverUri by remember { mutableStateOf<Uri?>(null) }
    val coverPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pendingRoomCoverUri = uri
    }

    pendingRoomCoverUri?.let { selectedUri ->
        ImageCropDialog(
            imageUri = selectedUri,
            title = "Adjust Room Cover",
            aspectRatio = 16f / 9f,
            onCancel = { pendingRoomCoverUri = null },
            onApply = { croppedUri ->
                pendingRoomCoverUri = null
                onUploadCover(croppedUri)
            }
        )
    }

    val eligibleSpeakers = remember(stage.speakerSeats) {
        stage.speakerSeats.filter { it.isOccupied && !it.isHostSeat }
    }
    val canEndRoom = isOwner || !room.effectiveOwnerOnline

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isOwner) "Host Stage Controls" else "Room Admin Stage Controls",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isOwner && room.adminIds.isNotEmpty()) {
                        Text(
                            text = "Managing stage while room owner ${room.effectiveOwnerName} is ${if (room.effectiveOwnerOnline) "online" else "offline"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeCyan
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Room Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it.take(200) },
                    label = { Text("Room Announcement") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Room Category", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RoomCategory.creatableCategories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Room Cover Gallery", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { keepCustomCover = true; coverPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().testTag("room_cover_upload_button")
                    ) {
                        Text("Upload from Gallery")
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(RoomCoverPreset.entries) { preset ->
                            val selected = !keepCustomCover && selectedCoverKey == preset.key
                            Column(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { selectedCoverKey = preset.key; keepCustomCover = false }
                                    .testTag("room_cover_${preset.key}"),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = coverDrawableRes(preset.key)),
                                    contentDescription = preset.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(82.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) VibeCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = VibePink)
                        Column {
                            Text("Lock Room Stage", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Passcode required to enter this room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Update / Clear Room Passcode", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = changePassword, onCheckedChange = { changePassword = it })
                }
                if (changePassword) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it.take(16) },
                        label = { Text("New Passcode (blank to remove)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        onSaveSettings(
                            title,
                            announcement,
                            selectedCategory.label,
                            selectedCoverKey,
                            keepCustomCover,
                            locked,
                            if (changePassword) newPassword else null
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Stage Settings", fontWeight = FontWeight.Bold)
                }
            }
            if (isOwner && room.bannedUserIds.isNotEmpty()) {
                item {
                    Text(
                        text = "Banned from this room (${room.bannedUserIds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VibeCoral
                    )
                }
                items(room.bannedUserIds, key = { "banned_room_user_$it" }) { bannedId ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bannedId,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(
                            onClick = { onUnblockRoomMember(bannedId) },
                            modifier = Modifier.testTag("unblock_room_member_$bannedId")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unblock")
                        }
                    }
                }
            }

            if (eligibleSpeakers.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = {
                            onMuteAllSpeakers()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("host_mute_all_button")
                    ) {
                        Icon(Icons.Filled.MicOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mute All Stage Speakers", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (isOwner && eligibleSpeakers.isNotEmpty()) {
                item {
                    Text(
                        text = "Transfer Ownership Safely",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VibeCyan
                    )
                }
                items(eligibleSpeakers, key = { "xfer_${it.seatIndex}" }) { seat ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VibeAvatar(seat.occupantAvatarUrl, seat.occupantName, size = 36.dp, vipLevel = seat.occupantVipLevel, frameColorArgb = seat.occupantFrameColorArgb)
                                Text("${seat.occupantName} (Seat ${seat.seatIndex})", style = MaterialTheme.typography.bodyMedium)
                            }
                            OutlinedButton(onClick = { transferTarget = seat }) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Make Host")
                            }
                        }
                    }
                }
            }

            if (canEndRoom) {
                item {
                    OutlinedButton(
                        onClick = { confirmEndRoom = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeCoral),
                        border = BorderStroke(1.dp, VibeCoral.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("host_end_room_button")
                    ) {
                        Text("End Voice Room for Everyone", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    transferTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { transferTarget = null },
            title = { Text("Transfer Room Ownership?") },
            text = {
                Text("${target.occupantName} will become the dedicated Host in Seat 0 and receive full host controls.")
            },
            confirmButton = {
                Button(onClick = {
                    onTransferOwnership(target)
                    transferTarget = null
                    onDismiss()
                }) {
                    Text("Confirm Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = { transferTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmEndRoom) {
        AlertDialog(
            onDismissRequest = { confirmEndRoom = false },
            title = { Text("End Voice Room?") },
            text = { Text("All speakers and listeners will be disconnected from \"${room.title}\".") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmEndRoom = false
                        onDismiss()
                        onEndRoom()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeCoral)
                ) {
                    Text("End Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEndRoom = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomOnlineMembersSheet(
    isHost: Boolean,
    isOwner: Boolean = isHost,
    currentUserId: String = "",
    onlineUsers: List<RoomOnlineMember> = emptyList(),
    listeners: List<RoomListener>,
    hasOpenSpeakerSeat: Boolean = false,
    onAssignListenerToOpenSeat: ((RoomListener) -> Unit)? = null,
    onOpenUserProfile: ((String) -> Unit)? = null,
    onToggleRoomAdmin: ((userId: String, username: String, promote: Boolean) -> Unit)? = null,
    onReportListener: (RoomListener) -> Unit,
    blockedUserIds: List<String> = emptyList(),
    onBlockListener: (RoomListener) -> Unit,
    onUnblockListener: (RoomListener) -> Unit = {},
    onKickMember: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    // Only show users verified by the live room session + online presence stream.
    // Never repopulate the list from stale listener documents.
    val displayMembers = onlineUsers

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("room_online_members_sheet"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Online in Room (${displayMembers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("online_members_sheet_title")
                )
            }
            if (displayMembers.isEmpty()) {
                item {
                    Text(
                        text = "No online members right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(displayMembers, key = { "online_${it.userId}" }) { member ->
                    val roleLabel = when (member.stageRole) {
                        "HOST" -> "Host · Seat 0"
                        "SPEAKER" -> "Speaker · Seat ${member.seatIndex ?: "-"}"
                        else -> "Listener (Audience)"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (onOpenUserProfile != null) {
                                        Modifier.clickable {
                                            onDismiss()
                                            onOpenUserProfile(member.userId)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("online_member_row_${member.userId}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VibeAvatar(member.avatarUrl, member.username, size = 38.dp, vipLevel = member.vipLevel, frameColorArgb = member.frameColorArgb)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = member.username,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (member.isRoomAdmin && member.stageRole != "HOST") {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = VibeGold.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Admin",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VibeGold,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = roleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (member.stageRole == "HOST") VibeGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (member.userId != currentUserId) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOwner && member.stageRole != "HOST" && onToggleRoomAdmin != null) {
                                    IconButton(
                                        onClick = {
                                            onToggleRoomAdmin(member.userId, member.username, !member.isRoomAdmin)
                                        },
                                        modifier = Modifier.testTag("member_admin_toggle_${member.userId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AdminPanelSettings,
                                            contentDescription = if (member.isRoomAdmin) "Remove Room Admin" else "Promote to Room Admin",
                                            tint = if (member.isRoomAdmin) VibeGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                if (isHost && member.stageRole == "LISTENER" && hasOpenSpeakerSeat && onAssignListenerToOpenSeat != null) {
                                    val listenerModel = RoomListener(
                                        userId = member.userId,
                                        username = member.username,
                                        avatarUrl = member.avatarUrl,
                                        vipLevel = member.vipLevel,
                                        gender = member.gender
                                    )
                                    IconButton(
                                        onClick = { onAssignListenerToOpenSeat(listenerModel) },
                                        modifier = Modifier.testTag("listeners_sheet_assign_${member.userId}")
                                    ) {
                                        Icon(Icons.Filled.PersonAdd, contentDescription = "Seat ${member.username}", tint = VibeCyan, modifier = Modifier.size(19.dp))
                                    }
                                }
                                val listenerModel = RoomListener(
                                    userId = member.userId,
                                    username = member.username,
                                    avatarUrl = member.avatarUrl,
                                    vipLevel = member.vipLevel,
                                    gender = member.gender
                                )
                                IconButton(onClick = { onReportListener(listenerModel) }) {
                                    Icon(Icons.Filled.Flag, contentDescription = "Report user", modifier = Modifier.size(18.dp))
                                }
                                if (member.userId in blockedUserIds) {
                                    IconButton(onClick = { onUnblockListener(listenerModel) }) {
                                        Icon(Icons.Filled.Check, contentDescription = "Unblock user", tint = VibeCyan, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = { onBlockListener(listenerModel) }) {
                                        Icon(Icons.Filled.Block, contentDescription = "Block user", tint = VibeCoral, modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (isOwner && member.stageRole != "HOST") {
                                    IconButton(
                                        onClick = { onKickMember(member.userId) },
                                        modifier = Modifier.testTag("kick_room_member_${member.userId}")
                                    ) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = "Kick and ban from room", tint = VibeCoral, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeakerSeatActionDialog(
    seat: VoiceSeat,
    isCurrentUserHost: Boolean,
    isCurrentUserOwner: Boolean = isCurrentUserHost,
    isOccupantRoomAdmin: Boolean = false,
    ownerOnline: Boolean = true,
    isMySeat: Boolean,
    onDismiss: () -> Unit,
    onToggleMute: () -> Unit,
    onRemoveFromSeat: () -> Unit,
    onToggleRoomAdmin: ((Boolean) -> Unit)? = null,
    onQuickGift: (() -> Unit)? = null,
    onViewProfile: (() -> Unit)? = null,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit
) {
    val hostOfflineSeat = seat.isHostSeat && !ownerOnline
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = if (onViewProfile != null && seat.occupantId.isNotBlank()) {
                    Modifier.clickable { onDismiss(); onViewProfile() }
                } else {
                    Modifier
                }
            ) {
                if (hostOfflineSeat) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PersonOff,
                                contentDescription = "Host offline",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    VibeAvatar(seat.occupantAvatarUrl, seat.occupantName, size = 42.dp, vipLevel = seat.occupantVipLevel)
                }
                Column {
                    Text(seat.occupantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            hostOfflineSeat -> "Dedicated Host Seat (0) · OFFLINE"
                            seat.isHostSeat -> "Dedicated Host Seat (0)"
                            isOccupantRoomAdmin -> "Speaker Seat ${seat.seatIndex} · Room Admin"
                            else -> "Speaker Seat ${seat.seatIndex}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hostOfflineSeat) VibeCoral else VibeCyan
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onViewProfile != null && seat.occupantId.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onDismiss(); onViewProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seat_dialog_view_profile")
                    ) {
                        Text("View Profile")
                    }
                }
                if (!hostOfflineSeat && (isMySeat || isCurrentUserHost)) {
                    Button(
                        onClick = { onToggleMute(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (seat.muted) Icons.Filled.Mic else Icons.Filled.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (seat.muted) "Unmute Microphone" else "Mute Microphone")
                    }
                }
                if (!seat.isHostSeat && (isMySeat || isCurrentUserHost)) {
                    OutlinedButton(
                        onClick = { onRemoveFromSeat(); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeCoral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isMySeat) "Step Down to Listener" else "Remove Speaker from Seat")
                    }
                }
                if (isCurrentUserOwner && !seat.isHostSeat && !isMySeat && onToggleRoomAdmin != null) {
                    OutlinedButton(
                        onClick = {
                            onToggleRoomAdmin(!isOccupantRoomAdmin)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isOccupantRoomAdmin) VibeCoral else VibeGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seat_dialog_toggle_admin")
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isOccupantRoomAdmin) "Remove Room Admin" else "Make Room Admin")
                    }
                }
                if (!isMySeat) {
                    if (onQuickGift != null && !hostOfflineSeat) {
                        Button(
                            onClick = { onDismiss(); onQuickGift() },
                            colors = ButtonDefaults.buttonColors(containerColor = VibePink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seat_dialog_send_gift")
                        ) {
                            Text("Send Gift to ${seat.occupantName}", fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onReportUser(); onDismiss() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Report")
                        }
                        OutlinedButton(
                            onClick = { onBlockUser(); onDismiss() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeCoral),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Block")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteSpeakerSheet(
    targetSeatIndex: Int,
    room: VoiceRoom,
    stage: VoiceStage,
    listeners: List<RoomListener>,
    followingProfiles: List<UserProfile>,
    followingIds: Set<String> = emptySet(),
    onToggleSeatLock: ((Boolean) -> Unit)? = null,
    onOpenUserProfile: ((String) -> Unit)? = null,
    onAssignToSeat: (userId: String, username: String, avatarUrl: String, vipLevel: Long, gender: String) -> Unit,
    onInviteUserToRoom: (userId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val targetSeat = stage.speakerSeats.firstOrNull { it.seatIndex.toInt() == targetSeatIndex }
    val seatedUserIds = remember(stage) {
        (listOf(stage.hostSeat) + stage.speakerSeats)
            .map { it.occupantId }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val unseatedListeners = remember(listeners, seatedUserIds) {
        listeners.filter { it.userId !in seatedUserIds }
    }
    val listenerIds = remember(unseatedListeners) {
        unseatedListeners.map { it.userId }.toSet()
    }
    val ownerId = room.effectiveOwnerId
    val availableFollowing = remember(followingProfiles, seatedUserIds, ownerId, followingIds) {
        followingProfiles
            .filter { it.userId != ownerId && it.userId != room.hostId && it.userId !in seatedUserIds }
            .sortedByDescending { it.userId in followingIds }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("invite_speaker_sheet"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Invite Speaker · Seat $targetSeatIndex",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Invite a room listener or someone you follow to this seat. They must accept the seat invitation before being seated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (targetSeat != null && onToggleSeatLock != null) {
                        val locked = targetSeat.seatLocked
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = if (locked) VibePink else VibeCyan, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (locked) "Seat $targetSeatIndex Locked" else "Lock Seat $targetSeatIndex",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Switch(
                                    checked = locked,
                                    onCheckedChange = { nextLocked ->
                                        onToggleSeatLock(nextLocked)
                                    },
                                    modifier = Modifier.testTag("invite_sheet_lock_seat_toggle")
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Room Listeners
            item {
                Text(
                    text = "Room Listeners (${unseatedListeners.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VibeCyan
                )
            }
            if (unseatedListeners.isEmpty()) {
                item {
                    Text(
                        text = "No listeners currently waiting in the room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(unseatedListeners, key = { "listener_${it.userId}" }) { listener ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (onOpenUserProfile != null) {
                                            Modifier.clickable {
                                                onDismiss()
                                                onOpenUserProfile(listener.userId)
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VibeAvatar(listener.avatarUrl, listener.username, size = 38.dp, vipLevel = listener.vipLevel)
                                Column {
                                    Text(listener.username, fontWeight = FontWeight.Medium)
                                    Text("Listening in room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick = {
                                    onAssignToSeat(listener.userId, listener.username, listener.avatarUrl, listener.vipLevel, listener.gender)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                                modifier = Modifier.testTag("invite_sheet_assign_listener_${listener.userId}")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite")
                            }
                        }
                    }
                }
            }

            // Section 3: Following Contacts
            item {
                Text(
                    text = "Following Contacts (${availableFollowing.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VibePink
                )
            }
            if (availableFollowing.isEmpty()) {
                item {
                    Text(
                        text = "Follow users from Explore to invite them straight to your stage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(availableFollowing, key = { "following_${it.userId}" }) { contact ->
                    val isInRoom = contact.userId in listenerIds
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (onOpenUserProfile != null) {
                                            Modifier.clickable {
                                                onDismiss()
                                                onOpenUserProfile(contact.userId)
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VibeAvatar(
                                    avatarUrl = contact.avatarUrl,
                                    username = contact.username,
                                    size = 38.dp,
                                    vipLevel = contact.vipLevel,
                                    online = contact.online && contact.showOnlineStatus
                                )
                                Column {
                                    Text(contact.username, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = when {
                                            isInRoom -> "In room · Ready for Seat $targetSeatIndex"
                                            contact.online && contact.showOnlineStatus -> "Following · Online"
                                            else -> "Following · Send room invite"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isInRoom) {
                                Button(
                                    onClick = {
                                        onAssignToSeat(contact.userId, contact.username, contact.avatarUrl, contact.vipLevel, contact.gender)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                                    modifier = Modifier.testTag("invite_sheet_assign_following_${contact.userId}")
                                ) {
                                    Text("Invite")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onInviteUserToRoom(contact.userId)
                                    },
                                    modifier = Modifier.testTag("invite_sheet_invite_following_${contact.userId}")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Invite")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeatInvitationDialog(
    request: SpeakerRequest,
    onAcceptInvitation: () -> Unit = {},
    onRejectInvitation: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val seatLabel = if (request.preferredSeatIndex in 1L..8L) "Seat ${request.preferredSeatIndex}" else "an open speaker seat"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = VibeGold)
                Text("Seat Invitation", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "The Host invited you to join $seatLabel. Your seat will be assigned only after you accept."
            )
        },
        confirmButton = {
            Button(
                    onClick = { onAcceptInvitation(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeEmerald),
                    modifier = Modifier.testTag("accept_seat_invitation_button")
                ) { Text("Accept") }
        },
        dismissButton = {
            TextButton(
                    onClick = { onRejectInvitation(); onDismiss() },
                    modifier = Modifier.testTag("reject_seat_invitation_button")
                ) { Text("Reject") }
        }
    )
}
