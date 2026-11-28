package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RoomCategory
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.data.model.VoiceRoom
import com.example.ui.components.VibeCoinIcon
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VoiceRoomCard
import com.example.ui.state.UiState
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.ui.theme.VibeGold
import com.example.ui.theme.VibePink
import com.example.ui.theme.VibeViolet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    currentUser: UserProfile,
    roomsState: UiState<List<VoiceRoom>>,
    activeUsers: List<UserProfile>,
    recentRoomIds: List<String>,
    unreadNotificationCount: Int,
    blockedUserIds: List<String>,
    followingIds: Set<String> = emptySet(),
    onOpenRoom: (VoiceRoom) -> Unit,
    onCreateRoomClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenGiftRanking: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onOpenUserProfile: (UserProfile) -> Unit,
    onVerifyPasscode: (VoiceRoom, String) -> Boolean,
    onOpenGame: () -> Unit,
    onOpenShop: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RoomCategory.ALL) }
    var passwordPromptRoom by remember { mutableStateOf<VoiceRoom?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val istTimeZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    var todayStr by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istTimeZone }.format(Date())
        )
    }
    LaunchedEffect(istTimeZone) {
        while (true) {
            val nextMidnight = Calendar.getInstance(istTimeZone).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
            todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = istTimeZone }
                .format(Date())
        }
    }
    val hasClaimedDaily = currentUser.lastDailyClaimDay == todayStr
    val canClaimDaily = !hasClaimedDaily

    val onlineUsers = remember(activeUsers, currentUser.userId, blockedUserIds, followingIds) {
        activeUsers.filter {
            it.userId != currentUser.userId &&
                it.userId !in blockedUserIds &&
                it.online &&
                it.showOnlineStatus
        }.sortedByDescending { it.userId in followingIds }.take(15)
    }

    // The same home search supports room IDs and public Vibe IDs (VP-xxxxxx),
    // including users who are not currently online.
    val matchingUsers = remember(activeUsers, searchQuery, currentUser.userId, blockedUserIds) {
        val query = searchQuery.trim()
        if (query.isBlank()) emptyList() else {
            val normalizedId = query.removePrefix("ID-").removePrefix("id-")
            activeUsers.filter { user ->
                user.userId != currentUser.userId &&
                    user.userId !in blockedUserIds &&
                    (user.userId.equals(query, ignoreCase = true) ||
                     user.vibeId.equals(normalizedId, ignoreCase = true) ||
                     user.vibeId.substringAfterLast("-").equals(normalizedId, ignoreCase = true))
            }.take(8)
        }
    }

    val handleRoomTap: (VoiceRoom) -> Unit = { room ->
        if (room.hasPassword && !room.isOwner(currentUser.userId)) {
            enteredPin = ""
            pinError = null
            passwordPromptRoom = room
        } else {
            onOpenRoom(room)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VibePlay",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hey ${currentUser.username}, jump into a live stage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VibeGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, VibeGold.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            VibeCoinIcon(modifier = Modifier.size(24.dp))
                            Text(
                                text = "${currentUser.coinBalance}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = VibeGold
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text("$unreadNotificationCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                }
            }
        }

        // Equal-size Home shortcuts; Profile Shop remains unchanged.
        item {
            AnimatedVisibility(visible = true, enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeShortcutCard("VibePlay Shop", "Discover items", VibePink, "SHOP", Modifier.weight(1f).height(112.dp), isGame = false, onClick = onOpenShop)
                    HomeShortcutCard("Game Zone", "Fruit Treasure", VibeCyan, "PLAY", Modifier.weight(1f).height(112.dp), isGame = true, onClick = onOpenGame)
                }
            }
        }

        item(key = "home_public_moments_feed") {
            HomeMomentsFeed(currentUser.userId)
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onOpenGiftRanking)
                    .testTag("home_total_ranking"),
                shape = RoundedCornerShape(20.dp),
                color = VibeViolet.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, VibeViolet.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VibeCoinIcon(modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ranking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Top Gifters · Top Receivers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("View", color = VibeCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Daily Login Reward Card
        if (canClaimDaily) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF3A3026),
                    border = BorderStroke(1.dp, Color(0xFFFFC66D).copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = VibeGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Daily Coins & XP Reward!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Claim +100,000 Coins & +100 XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onClaimDailyReward,
                            colors = ButtonDefaults.buttonColors(containerColor = VibeGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("claim_daily_reward_button")
                        ) {
                            Text("Claim", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF3A3026),
                    border = BorderStroke(1.dp, Color(0xFFFFC66D).copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = VibeGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Daily Coins Claimed!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your next reward unlocks at 12:00 AM IST.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // User ID search stays on Home; room search lives only in Rooms.
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search user ID...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("home_clear_search_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("home_search_input")
            )
        }

        // Exact user-ID results from the shared search box.
        if (matchingUsers.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Matching Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(matchingUsers, key = { "search_user_${it.userId}" }) { user ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenUserProfile(user) }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VibeAvatar(
                                    avatarUrl = user.avatarUrl,
                                    username = user.username,
                                    size = 42.dp,
                                    vipLevel = if (user.isVipActive) user.vipLevel else 0L
                                )
                                Column {
                                    Text(user.username, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Text("ID-${user.vibeId}", style = MaterialTheme.typography.labelSmall, color = VibeCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Online Following & Active Creators Strip
        if (onlineUsers.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Following & Online",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${onlineUsers.size} active",
                            style = MaterialTheme.typography.labelMedium,
                            color = VibeEmerald
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(onlineUsers, key = { it.userId }) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable { onOpenUserProfile(user) }
                                    .testTag("online_user_${user.userId}")
                            ) {
                                VibeAvatar(
                                    avatarUrl = user.avatarUrl,
                                    username = user.username,
                                    size = 54.dp,
                                    vipLevel = if (user.isVipActive) user.vipLevel else 0L,
                                    online = user.online && user.showOnlineStatus,
                                    frameColorArgb = VibeShopCatalog.frames.firstOrNull { it.id == user.activeFrameId && user.isFrameActive(it.id) }?.colorArgb
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

    }

}



private data class HomeMomentPost(
    val id: String,
    val ownerId: String,
    val photoBase64: String,
    val quote: String,
    val createdAtMillis: Long
)

@Composable
private fun HomeMomentsFeed(currentUserId: String) {
    var posts by remember { mutableStateOf<List<HomeMomentPost>>(emptyList()) }
    var selectedFeed by remember { mutableStateOf("Recent") }
    var loading by remember { mutableStateOf(true) }
    val db = remember { FirebaseFirestore.getInstance() }

    DisposableEffect(currentUserId) {
        val registration = db.collectionGroup("moments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    posts = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        val ownerId = doc.getString("ownerId").orEmpty()
                        val photo = doc.getString("photoBase64").orEmpty()
                        if (doc.getString("mediaType") != "image" ||
                            doc.getString("contentType") != "image/jpeg" ||
                            ownerId.isBlank() || photo.isBlank()) null
                        else HomeMomentPost(
                            id = doc.id,
                            ownerId = ownerId,
                            photoBase64 = photo,
                            quote = doc.getString("quote").orEmpty(),
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        )
                    }
                }
                loading = false
            }
        onDispose { registration.remove() }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Community Photos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("See what everyone is sharing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = VibeCyan)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Most Liked", "Recent").forEach { tab ->
                FilterChip(
                    selected = selectedFeed == tab,
                    onClick = { selectedFeed = tab },
                    label = { Text(tab) },
                    modifier = Modifier.testTag(if (tab == "Most Liked") "home_most_liked_tab" else "home_recent_tab")
                )
            }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), color = VibeCyan)
            }
        } else if (posts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = VibeCyan, modifier = Modifier.size(30.dp))
                    Text("No photos shared yet", fontWeight = FontWeight.Bold)
                    Text("Photos shared in user profiles will appear here for everyone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val orderedPosts = if (selectedFeed == "Recent") posts.sortedByDescending { it.createdAtMillis }
                else posts
            orderedPosts.forEach { post ->
                HomeMomentCard(post = post, currentUserId = currentUserId, mostLiked = selectedFeed == "Most Liked")
            }
        }
    }
}

@Composable
private fun HomeMomentCard(post: HomeMomentPost, currentUserId: String, mostLiked: Boolean) {
    val db = remember { FirebaseFirestore.getInstance() }
    var username by remember(post.ownerId) { mutableStateOf("VibePlay User") }
    var avatarUrl by remember(post.ownerId) { mutableStateOf("") }
    var likeCount by remember(post.id) { mutableStateOf(0) }
    var liked by remember(post.id, currentUserId) { mutableStateOf(false) }
    var likeBusy by remember(post.id) { mutableStateOf(false) }
    val momentRef = remember(post.ownerId, post.id) {
        db.collection("users").document(post.ownerId).collection("moments").document(post.id)
    }

    DisposableEffect(post.ownerId) {
        val profileListener = db.collection("users").document(post.ownerId)
            .addSnapshotListener { snapshot, _ ->
                username = snapshot?.getString("username")?.takeIf { it.isNotBlank() } ?: "VibePlay User"
                avatarUrl = snapshot?.getString("avatarUrl").orEmpty()
            }
        onDispose { profileListener.remove() }
    }
    DisposableEffect(post.id, currentUserId) {
        val likesListener = momentRef.collection("likes").addSnapshotListener { snapshot, _ ->
            likeCount = snapshot?.size() ?: 0
            liked = snapshot?.documents?.any { it.id == currentUserId } == true
        }
        onDispose { likesListener.remove() }
    }

    val photoBitmap = remember(post.photoBase64) {
        runCatching {
            val bytes = Base64.decode(post.photoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("home_moment_${post.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile photo of $username",
                        modifier = Modifier.size(42.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(VibeViolet.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = VibeCyan)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(username, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("VibePlay Moment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (mostLiked) Text("♥ $likeCount", color = VibePink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = "Photo shared by $username",
                    modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("Photo unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (post.quote.isNotBlank()) Text(post.quote, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!likeBusy && currentUserId.isNotBlank()) {
                            likeBusy = true
                            val likeRef = momentRef.collection("likes").document(currentUserId)
                            val task = if (liked) likeRef.delete()
                                else likeRef.set(mapOf("userId" to currentUserId, "createdAt" to FieldValue.serverTimestamp()))
                            task.addOnCompleteListener { likeBusy = false }
                        }
                    },
                    enabled = !likeBusy && currentUserId.isNotBlank(),
                    modifier = Modifier.testTag("home_moment_like_${post.id}")
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (liked) "Unlike photo" else "Like photo",
                        tint = if (liked) VibePink else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("$likeCount likes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun SectionTitle(title: String, badge: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = badge,
            style = MaterialTheme.typography.labelMedium,
            color = VibeCyan
        )
    }
}

@Composable
private fun EmptyRoomsPrompt(onCreateRoomClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VibeViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = VibeCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "No Live Voice Rooms Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Be the first to launch a 9-seat voice party stage and invite friends!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCreateRoomClick,
                colors = ButtonDefaults.buttonColors(containerColor = VibeViolet),
                modifier = Modifier.testTag("empty_create_room_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create 9-Seat Room")
            }
        }
    }
}


@Composable
private fun HomeShortcutCard(
    title: String,
    subtitle: String,
    accent: Color,
    symbol: String,
    modifier: Modifier = Modifier,
    isGame: Boolean,
    onClick: () -> Unit
) {
    val motion = rememberInfiniteTransition(label = "home_shortcut_motion")
    val drift by motion.animateFloat(-7f, 7f, infiniteRepeatable(tween(if (isGame) 1250 else 1850), RepeatMode.Reverse), label = "shortcut_drift")
    val glow by motion.animateFloat(0.12f, 0.34f, infiniteRepeatable(tween(1650), RepeatMode.Reverse), label = "shortcut_glow")
    val spin by motion.animateFloat(-12f, 12f, infiniteRepeatable(tween(2800), RepeatMode.Reverse), label = "shortcut_spin")

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.4.dp, accent.copy(alpha = 0.9f))
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha = glow + 0.12f), Color.Transparent, accent.copy(alpha = 0.08f))))) {
            Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.24f)) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(31.dp).graphicsLayer { translationY = drift * 0.32f; rotationZ = if (isGame) spin * 0.25f else spin * 0.12f }) {
                            val w = size.width; val h = size.height
                            if (isGame) {
                                val body = Path().apply {
                                    moveTo(w*.18f,h*.38f); quadraticTo(w*.20f,h*.22f,w*.36f,h*.25f)
                                    lineTo(w*.64f,h*.25f); quadraticTo(w*.80f,h*.22f,w*.82f,h*.38f)
                                    lineTo(w*.94f,h*.68f); quadraticTo(w*.98f,h*.86f,w*.80f,h*.84f)
                                    lineTo(w*.66f,h*.68f); lineTo(w*.34f,h*.68f); lineTo(w*.20f,h*.84f)
                                    quadraticTo(w*.02f,h*.86f,w*.06f,h*.68f); close()
                                }
                                drawPath(body, accent, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
                                drawLine(accent, Offset(w*.24f,h*.43f), Offset(w*.24f,h*.59f), 2.4.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(accent, Offset(w*.16f,h*.51f), Offset(w*.32f,h*.51f), 2.4.dp.toPx(), cap = StrokeCap.Round)
                                drawCircle(accent, 2.2.dp.toPx(), Offset(w*.70f,h*.46f))
                                drawCircle(accent, 2.2.dp.toPx(), Offset(w*.80f,h*.58f))
                            } else {
                                drawRoundRect(accent, Offset(w*.18f,h*.34f), Size(w*.64f,h*.56f), CornerRadius(4.dp.toPx()), style = Stroke(2.6.dp.toPx()))
                                drawArc(accent, 180f, 180f, false, Offset(w*.34f,h*.08f), Size(w*.32f,h*.48f), style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
                                drawLine(accent, Offset(w*.5f,h*.48f), Offset(w*.5f,h*.70f), 2.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Filled.AutoAwesome, null, tint = accent.copy(alpha = 0.62f + glow * 0.8f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 7.dp).size(17.dp).graphicsLayer {
                    translationY = drift; rotationZ = spin; scaleX = 0.88f + glow; scaleY = 0.88f + glow
                })
            Icon(Icons.Filled.AutoAwesome, null, tint = accent.copy(alpha = 0.38f + glow),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 34.dp, bottom = 11.dp).size(10.dp).graphicsLayer {
                    translationY = -drift; rotationZ = -spin * 1.4f
                })
            Text(symbol, modifier = Modifier.align(Alignment.TopStart).padding(start = 13.dp, top = 7.dp).graphicsLayer {
                translationY = -drift * 0.45f; alpha = 0.48f + glow
            }, color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
        }
    }
}
