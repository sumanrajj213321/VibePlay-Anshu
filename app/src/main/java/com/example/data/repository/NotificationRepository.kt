package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
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

class NotificationRepository(
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

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit> {
        if (notificationIds.isEmpty()) return Result.success(Unit)
        val path = "notifications"
        return try {
            requireUserId()
            notificationIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.update(db.collection("notifications").document(id), "read", true)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun inviteFriendToRoom(
        sender: UserProfile,
        recipientId: String,
        room: VoiceRoom
    ): Result<Unit> {
        val path = "notifications"
        return try {
            val uid = requireUserId()
            check(uid == sender.userId)
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to recipientId,
                    "senderId" to uid,
                    "senderName" to sender.username,
                    "type" to "ROOM_INVITE",
                    "title" to "Voice Room Invite",
                    "body" to "${sender.username} invited you to join \"${room.title}\".",
                    "targetId" to room.id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        reporter: UserProfile,
        targetType: String,
        targetId: String,
        targetName: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val path = "reports"
        return try {
            val uid = requireUserId()
            check(uid == reporter.userId)
            db.collection("reports").document().set(
                mapOf(
                    "reporterId" to uid,
                    "reporterName" to reporter.username,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "targetName" to targetName.take(100),
                    "reason" to reason.trim().take(100),
                    "details" to details.trim().take(500),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
