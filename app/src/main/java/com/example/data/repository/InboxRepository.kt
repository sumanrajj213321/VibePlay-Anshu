package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InboxRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun conversationIdFor(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> =
        db.collection("conversations").document(conversationId).collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }

    suspend fun getOrCreateConversation(
        currentUser: UserProfile,
        peer: UserProfile
    ): Result<String> {
        val convId = conversationIdFor(currentUser.userId, peer.userId)
        val path = "conversations/$convId"
        return try {
            val uid = requireUserId()
            check(uid == currentUser.userId && uid != peer.userId)
            val convRef = db.collection("conversations").document(convId)
            val snap = convRef.get().await()
            if (!snap.exists()) {
                convRef.set(
                    mapOf(
                        "conversationType" to "DIRECT",
                        "groupName" to "",
                        "groupAvatarUrl" to "",
                        "ownerId" to "",
                        "adminIds" to emptyList<String>(),
                        "participantIds" to listOf(uid, peer.userId).sorted(),
                        "participantNames" to mapOf(uid to currentUser.username, peer.userId to peer.username),
                        "participantAvatars" to mapOf(uid to currentUser.avatarUrl, peer.userId to peer.avatarUrl),
                        "lastMessage" to "Conversation started",
                        "lastSenderId" to uid,
                        "unreadBy" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupConversation(
        creator: UserProfile,
        groupName: String,
        groupAvatarUrl: String,
        members: List<UserProfile>
    ): Result<String> {
        val convRef = db.collection("conversations").document()
        val path = convRef.path
        return try {
            val uid = requireUserId()
            check(uid == creator.userId)
            val cleanName = groupName.trim().take(60)
            require(cleanName.length >= 2) { "Group name must be at least 2 characters." }
            val allParticipants = (listOf(creator) + members).distinctBy { it.userId }.take(50)
            require(allParticipants.size >= 2) { "Select at least 1 following user to create a group." }

            val participantIds = allParticipants.map { it.userId }
            val participantNames = allParticipants.associate { it.userId to it.username }
            val participantAvatars = allParticipants.associate { it.userId to it.avatarUrl }
            val recipientIds = participantIds.filter { it != uid }
            val createdMsg = "${creator.username} created group \"$cleanName\""

            val batch = db.batch()
            batch.set(
                convRef,
                mapOf(
                    "conversationType" to "GROUP",
                    "groupName" to cleanName,
                    "groupAvatarUrl" to groupAvatarUrl.take(500),
                    "ownerId" to uid,
                    "adminIds" to listOf(uid),
                    "participantIds" to participantIds,
                    "participantNames" to participantNames,
                    "participantAvatars" to participantAvatars,
                    "lastMessage" to createdMsg,
                    "lastSenderId" to uid,
                    "unreadBy" to recipientIds,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                convRef.collection("messages").document(),
                mapOf(
                    "participantIds" to participantIds,
                    "senderId" to uid,
                    "senderName" to creator.username,
                    "senderAvatarUrl" to creator.avatarUrl,
                    "senderGender" to creator.gender,
                    "type" to "SYSTEM",
                    "text" to createdMsg,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            // Commit the group and its welcome message first. Notifications are
            // best-effort so a notification-rule failure cannot cancel group creation.
            batch.commit().await()
            recipientIds.take(45).forEach { targetId ->
                try {
                    db.collection("notifications").document().set(
                        mapOf(
                            "recipientId" to targetId,
                            "senderId" to uid,
                            "senderName" to creator.username.take(60),
                            "type" to "MESSAGE",
                            "title" to cleanName,
                            "body" to "${creator.username.take(60)} added you to group ${cleanName}".take(240),
                            "targetId" to convRef.id,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {
                    // Group membership is already saved; inbox notification can be retried later.
                }
            }
            Result.success(convRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        sender: UserProfile,
        text: String,
        imageUri: String? = null
    ): Result<Unit> {
        val path = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            check(uid == sender.userId)
            val cleanText = text.trim().take(1000)
            val isImage = !imageUri.isNullOrBlank()
            require(cleanText.isNotEmpty() || isImage) { "Enter a message or pick an image." }

            val convRef = db.collection("conversations").document(conversation.id)
            val msgRef = convRef.collection("messages").document()
            val preview = if (isImage && cleanText.isEmpty()) "[Photo]" else cleanText.take(120)
            val recipients = conversation.participantIds.filter { it != uid }.take(49)

            val updatedNames = conversation.participantNames.toMutableMap().apply {
                put(uid, sender.username)
            }
            val updatedAvatars = conversation.participantAvatars.toMutableMap().apply {
                put(uid, sender.avatarUrl)
            }

            val batch = db.batch()
            val msgPayload = mutableMapOf<String, Any>(
                "participantIds" to conversation.participantIds,
                "senderId" to uid,
                "senderName" to sender.username,
                "senderAvatarUrl" to sender.avatarUrl,
                "senderGender" to sender.gender,
                "type" to if (isImage) "IMAGE" else "TEXT",
                "text" to cleanText,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!imageUri.isNullOrBlank()) {
                msgPayload["imageUri"] = imageUri.take(600)
            }
            batch.set(msgRef, msgPayload)
            batch.update(
                convRef,
                mapOf(
                    "participantNames" to updatedNames,
                    "participantAvatars" to updatedAvatars,
                    "lastMessage" to if (conversation.isGroupChat) "${sender.username}: $preview".take(400) else preview,
                    "lastSenderId" to uid,
                    "unreadBy" to recipients,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            recipients.take(25).forEach { targetId ->
                batch.set(
                    db.collection("notifications").document(),
                    mapOf(
                        "recipientId" to targetId,
                        "senderId" to uid,
                        "senderName" to sender.username,
                        "type" to "MESSAGE",
                        "title" to if (conversation.isGroupChat) {
                            "${conversation.groupName.ifBlank { "Group" }} · ${sender.username}".take(100)
                        } else {
                            sender.username.take(100)
                        },
                        "body" to preview.take(240),
                        "targetId" to conversation.id,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addGroupMembers(
        conversation: Conversation,
        actor: UserProfile,
        newMembers: List<UserProfile>
    ): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            check(conversation.isGroupChat)
            check(conversation.ownerId == uid || uid in conversation.adminIds) {
                "Only group admins can add members."
            }
            val fresh = newMembers.filter { it.userId !in conversation.participantIds }
            if (fresh.isEmpty()) return Result.success(Unit)

            val convRef = db.collection("conversations").document(conversation.id)
            val nextIds = (conversation.participantIds + fresh.map { it.userId }).distinct().take(50)
            val nextNames = conversation.participantNames.toMutableMap().apply {
                fresh.forEach { put(it.userId, it.username) }
            }
            val nextAvatars = conversation.participantAvatars.toMutableMap().apply {
                fresh.forEach { put(it.userId, it.avatarUrl) }
            }
            val notice = "${actor.username} added ${fresh.joinToString { it.username }}".take(400)
            val batch = db.batch()
            batch.update(
                convRef,
                mapOf(
                    "participantIds" to nextIds,
                    "participantNames" to nextNames,
                    "participantAvatars" to nextAvatars,
                    "lastMessage" to notice,
                    "lastSenderId" to uid,
                    "unreadBy" to nextIds.filter { it != uid },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                convRef.collection("messages").document(),
                mapOf(
                    "participantIds" to nextIds,
                    "senderId" to uid,
                    "senderName" to actor.username,
                    "senderAvatarUrl" to actor.avatarUrl,
                    "senderGender" to actor.gender,
                    "type" to "SYSTEM",
                    "text" to notice,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeGroupMember(
        conversation: Conversation,
        actor: UserProfile,
        targetUserId: String
    ): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            check(conversation.isGroupChat)
            check(conversation.ownerId == uid || uid in conversation.adminIds) {
                "Only group admins can remove members."
            }
            require(targetUserId != conversation.ownerId && targetUserId != uid) {
                "Use Leave Group to exit yourself, and the group owner cannot be removed."
            }
            val targetName = conversation.participantNames[targetUserId] ?: "Member"
            val nextIds = conversation.participantIds.filter { it != targetUserId }
            val nextAdmins = conversation.adminIds.filter { it != targetUserId }
            val nextNames = conversation.participantNames.toMutableMap().apply { remove(targetUserId) }
            val nextAvatars = conversation.participantAvatars.toMutableMap().apply { remove(targetUserId) }
            val notice = "${actor.username} removed $targetName".take(400)

            val convRef = db.collection("conversations").document(conversation.id)
            val batch = db.batch()
            batch.update(
                convRef,
                mapOf(
                    "participantIds" to nextIds,
                    "participantNames" to nextNames,
                    "participantAvatars" to nextAvatars,
                    "adminIds" to nextAdmins,
                    "lastMessage" to notice,
                    "lastSenderId" to uid,
                    "unreadBy" to nextIds.filter { it != uid },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                convRef.collection("messages").document(),
                mapOf(
                    "participantIds" to nextIds,
                    "senderId" to uid,
                    "senderName" to actor.username,
                    "senderAvatarUrl" to actor.avatarUrl,
                    "senderGender" to actor.gender,
                    "type" to "SYSTEM",
                    "text" to notice,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupDetails(
        conversation: Conversation,
        groupName: String,
        groupAvatarUrl: String
    ): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            check(conversation.isGroupChat)
            check(conversation.ownerId == uid || uid in conversation.adminIds) {
                "Only group admins can edit group details."
            }
            val cleanName = groupName.trim().take(60)
            require(cleanName.length >= 2) { "Group name must be at least 2 characters." }
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "groupName" to cleanName,
                    "groupAvatarUrl" to groupAvatarUrl.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(conversation: Conversation, user: UserProfile): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            check(uid == user.userId && conversation.isGroupChat)
            val convRef = db.collection("conversations").document(conversation.id)
            db.runTransaction { tx ->
                val snap = tx.get(convRef)
                if (!snap.exists()) return@runTransaction
                val latest = snap.toObject(Conversation::class.java) ?: conversation
                val remaining = latest.participantIds.filter { it != uid }
                if (remaining.isEmpty()) {
                    tx.delete(convRef)
                } else {
                    val nextAdmins = latest.adminIds.filter { it != uid }.toMutableList()
                    val nextOwner = if (latest.ownerId == uid) {
                        val promoted = nextAdmins.firstOrNull { it in remaining } ?: remaining.first()
                        if (promoted !in nextAdmins) nextAdmins.add(promoted)
                        promoted
                    } else {
                        latest.ownerId
                    }
                    val nextNames = latest.participantNames.toMutableMap().apply { remove(uid) }
                    val nextAvatars = latest.participantAvatars.toMutableMap().apply { remove(uid) }
                    tx.update(
                        convRef,
                        mapOf(
                            "participantIds" to remaining,
                            "participantNames" to nextNames,
                            "participantAvatars" to nextAvatars,
                            "adminIds" to nextAdmins,
                            "ownerId" to nextOwner,
                            "unreadBy" to latest.unreadBy.filter { it != uid },
                            "lastMessage" to "${user.username} left the group",
                            "lastSenderId" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePinConversation(userId: String, conversationId: String, pin: Boolean): Result<Unit> {
        val path = "users/$userId/private/account"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val accountRef = db.collection("users").document(uid).collection("private").document("account")
            db.runTransaction { tx ->
                val snap = tx.get(accountRef)
                @Suppress("UNCHECKED_CAST")
                val existingPins = (snap.get("pinnedConversationIds") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val existingBlocked = (snap.get("blockedUserIds") as? List<String>) ?: emptyList()
                val existingEmail = snap.getString("email") ?: (auth.currentUser?.email ?: "")
                val nextPins = if (pin) {
                    (listOf(conversationId) + existingPins.filter { it != conversationId }).take(100)
                } else {
                    existingPins.filter { it != conversationId }
                }
                tx.set(
                    accountRef,
                    mapOf(
                        "userId" to uid,
                        "email" to existingEmail,
                        "blockedUserIds" to existingBlocked,
                        "pinnedConversationIds" to nextPins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(conversation: Conversation, userId: String): Result<Unit> {
        if (!conversation.unreadBy.contains(userId)) return Result.success(Unit)
        val path = "conversations/${conversation.id}"
        return try {
            requireUserId()
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "unreadBy" to conversation.unreadBy.filter { it != userId },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
