package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FollowEdge
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Timestamp
import java.security.MessageDigest
import java.util.Date

private data class RelationshipStyle(
    val title: String,
    val detail: String,
    val accent: Color,
    val symbol: Int,
    val backendType: String
)

private val relationshipStyles = listOf(
    RelationshipStyle("Buddies", "A playful bond built on shared moments.", Color(0xFF35C8F2), 0, "BUDDIES"),
    RelationshipStyle("Bro", "A bold, steadfast bond with a shield crest.", Color(0xFF9B83FF), 1, "BRO"),
    RelationshipStyle("Confidant", "A refined bond represented by a crystal seal.", Color(0xFF38D5AE), 2, "CONFIDANT"),
    RelationshipStyle("Bestie", "A radiant bond with an exclusive star crest.", Color(0xFFFF78B7), 3, "BESTIE")
)

@Composable
fun RelationshipScreen(
    currentUserId: String,
    followingList: List<FollowEdge>,
    onBack: () -> Unit
) {
    var statusMessage by remember { mutableStateOf("") }
    var sendingTo by remember { mutableStateOf<String?>(null) }
    val firestore = remember { FirebaseFirestore.getInstance() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Relationships", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "You can invite only people you already follow. Search by Vibe ID is not available here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Text("People you follow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (followingList.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    "You are not following anyone yet. Follow a user first to send a relationship invitation.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            followingList.forEach { person ->
                RelationshipFollowingCard(
                    person = person,
                    sending = sendingTo == person.peerId,
                    onInvite = { tagType ->
                        if (person.peerId.isBlank() || person.peerId == currentUserId) {
                            statusMessage = "This user cannot receive an invitation."
                        } else {
                            sendingTo = person.peerId
                            statusMessage = ""
                            val pairId = MessageDigest.getInstance("SHA-256")
                                .digest(listOf(currentUserId, person.peerId).sorted().joinToString(":").toByteArray())
                                .joinToString("") { "%02x".format(it) }
                            val inviteRef = firestore.collection("relationship_invites").document(pairId)
                            val notificationRef = firestore.collection("notifications").document()
                            firestore.collection("users").document(currentUserId).get()
                                .addOnSuccessListener { senderSnapshot ->
                                    val senderName = senderSnapshot.getString("username").orEmpty().ifBlank { "VibePlay user" }.take(60)
                                    val senderAvatar = senderSnapshot.getString("avatarUrl").orEmpty()
                                    val invitedName = person.peerName.ifBlank { "VibePlay user" }.take(60)
                                    val invitedAvatar = person.peerAvatarUrl
                                    val expiresAt = Timestamp(Date(System.currentTimeMillis() + 48L * 60L * 60L * 1000L))
                                    val inviteData = mapOf(
                                        "pairId" to pairId,
                                        "inviterId" to currentUserId,
                                        "invitedTo" to person.peerId,
                                        "inviterName" to senderName,
                                        "invitedName" to invitedName,
                                        "inviterAvatarUrl" to senderAvatar,
                                        "invitedAvatarUrl" to invitedAvatar,
                                        "tagType" to tagType,
                                        "status" to "PENDING",
                                        "notificationId" to notificationRef.id,
                                        "createdAt" to FieldValue.serverTimestamp(),
                                        "expiresAt" to expiresAt
                                    )
                                    val notificationData = mapOf(
                                        "recipientId" to person.peerId,
                                        "senderId" to currentUserId,
                                        "senderName" to senderName,
                                        "type" to "RELATIONSHIP_INVITE",
                                        "title" to "Relationship invitation",
                                        "body" to "$senderName invited you to become ${tagType.lowercase()}.",
                                        "targetId" to pairId,
                                        "read" to false,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )
                                    firestore.runBatch { batch ->
                                        batch.set(inviteRef, inviteData)
                                        batch.set(notificationRef, notificationData)
                                    }.addOnSuccessListener {
                                        sendingTo = null
                                        statusMessage = "Invitation sent to ${person.peerName.ifBlank { "this user" }}."
                                    }.addOnFailureListener { error ->
                                        sendingTo = null
                                        statusMessage = error.localizedMessage ?: "Could not send invitation."
                                    }
                                }
                                .addOnFailureListener { error ->
                                    sendingTo = null
                                    statusMessage = error.localizedMessage ?: "Could not load your profile."
                                }
                        }
                    }
                )
            }
        }
        if (statusMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text("Relationship tag styles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        relationshipStyles.forEach { style ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(18.dp),
                color = style.accent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, style.accent.copy(alpha = 0.58f))
            ) {
                Row(
                    Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RelationshipCustomMark(style.symbol, style.accent, Modifier.size(48.dp))
                    Column {
                        Text(style.title, color = style.accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        Text(style.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipFollowingCard(
    person: FollowEdge,
    sending: Boolean,
    onInvite: (String) -> Unit
) {
    var expanded by remember(person.peerId) { mutableStateOf(false) }
    var selectedTag by remember(person.peerId) { mutableStateOf("BUDDIES") }
    val selected = relationshipStyles.first { it.backendType == selectedTag }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, selected.accent.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(person.peerName.ifBlank { "VibePlay user" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Following", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { expanded = true }) {
                        Text(selected.title, color = selected.accent)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        relationshipStyles.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style.title, color = style.accent) },
                                onClick = {
                                    selectedTag = style.backendType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Button(onClick = { onInvite(selectedTag) }, enabled = !sending) {
                    Text(if (sending) "Sending…" else "Invite")
                }
            }
        }
    }
}

@Composable
private fun RelationshipCustomMark(kind: Int, accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "relationship-emblem")
    val rotation by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem-rotation"
    )
    val glow by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem-glow"
    )
    Canvas(modifier.graphicsLayer {
        rotationZ = rotation
        scaleX = glow
        scaleY = glow
    }) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.065f, cap = StrokeCap.Round)
        drawCircle(accent.copy(alpha = 0.14f), radius = w * 0.47f, center = Offset(w / 2f, h / 2f))
        when (kind) {
            0 -> {
                drawCircle(accent, w * 0.22f, Offset(w * 0.38f, h * 0.48f), style = stroke)
                drawCircle(accent, w * 0.22f, Offset(w * 0.62f, h * 0.48f), style = stroke)
                drawLine(accent, Offset(w * 0.43f, h * 0.49f), Offset(w * 0.57f, h * 0.49f), w * 0.07f)
            }
            1 -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.17f)
                    lineTo(w * 0.79f, h * 0.29f)
                    lineTo(w * 0.73f, h * 0.65f)
                    lineTo(w * 0.5f, h * 0.84f)
                    lineTo(w * 0.27f, h * 0.65f)
                    lineTo(w * 0.21f, h * 0.29f)
                    close()
                }
                drawPath(p, accent, style = stroke)
                drawLine(accent, Offset(w * 0.37f, h * 0.49f), Offset(w * 0.63f, h * 0.49f), w * 0.07f)
            }
            2 -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.82f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.86f)
                    lineTo(w * 0.18f, h * 0.5f)
                    close()
                }
                drawPath(p, accent, style = stroke)
                drawLine(accent, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.72f), w * 0.045f)
                drawLine(accent, Offset(w * 0.32f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), w * 0.045f)
            }
            else -> {
                val p = Path()
                for (i in 0 until 10) {
                    val angle = Math.PI * i / 5.0 - Math.PI / 2.0
                    val radius = if (i % 2 == 0) w * 0.32f else w * 0.14f
                    val x = w / 2f + kotlin.math.cos(angle).toFloat() * radius
                    val y = h / 2f + kotlin.math.sin(angle).toFloat() * radius
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, accent, style = stroke)
                drawCircle(accent, w * 0.045f, Offset(w / 2f, h / 2f))
            }
        }
    }
}
