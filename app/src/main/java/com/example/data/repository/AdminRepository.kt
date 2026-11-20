package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.GiftCatalogItem
import com.example.data.model.ModerationReport
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

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminUserId(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing admin tools.")
        check(uid == OFFICIAL_ADMIN_UID) { "This account is not authorized to use the VibePlay admin panel." }
        return uid
    }

    fun isCurrentUserAdmin(profile: UserProfile?): Boolean =
        auth.currentUser?.uid == OFFICIAL_ADMIN_UID && profile?.userId == OFFICIAL_ADMIN_UID

    private companion object {
        const val OFFICIAL_ADMIN_UID = "1fLIHgOyAJV6NbHBXNeVg9JR6v1"
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeAllRoomsForAdmin(): Flow<List<VoiceRoom>> =
        db.collection("rooms")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VoiceRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeReportsForAdmin(): Flow<List<ModerationReport>> =
        db.collection("reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllGiftsForAdmin(): Flow<List<GiftCatalogItem>> =
        db.collection("gifts_catalog")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GiftCatalogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.coinCost }
            }

    suspend fun updateUserAccountStatus(userId: String, status: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireAdminUserId()
            db.collection("users").document(userId).update(
                mapOf(
                    "accountStatus" to status,
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

    suspend fun grantUserVipAndCoins(userId: String, vipLevel: Long, coinBalance: Long): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireAdminUserId()
            db.collection("users").document(userId).update(
                mapOf(
                    "vipLevel" to vipLevel.coerceIn(0L, 5L),
                    "coinBalance" to coinBalance.coerceAtLeast(0L),
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

    suspend fun toggleRoomFeatured(roomId: String, featured: Boolean): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            requireAdminUserId()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "featured" to featured,
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

    suspend fun forceCloseRoom(roomId: String): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            requireAdminUserId()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "status" to "ENDED",
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

    suspend fun resolveReport(reportId: String, status: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireAdminUserId()
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status,
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

    suspend fun upsertGiftCatalogItem(
        giftId: String?,
        name: String,
        category: String,
        coinCost: Long,
        emoji: String,
        active: Boolean
    ): Result<Unit> {
        val docRef = if (giftId.isNullOrBlank()) {
            db.collection("gifts_catalog").document()
        } else {
            db.collection("gifts_catalog").document(giftId)
        }
        return try {
            requireAdminUserId()
            docRef.set(
                mapOf(
                    "name" to name.trim().take(40),
                    "category" to category,
                    "coinCost" to coinCost.coerceIn(1L, 1000000L),
                    "emoji" to emoji.trim().ifBlank { "\uD83C\uDF81" }.take(16),
                    "accentColorHex" to "#EC4899",
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultGifts(): Result<Int> {
        val path = "gifts_catalog"
        return try {
            requireAdminUserId()
            val batch = db.batch()
            val items = com.example.data.model.VibeCatalogDefaults.defaultGifts
            items.forEach { gift ->
                val ref = db.collection("gifts_catalog").document(gift.id)
                batch.set(
                    ref,
                    mapOf(
                        "name" to gift.name,
                        "category" to gift.category,
                        "coinCost" to gift.coinCost,
                        "emoji" to gift.emoji,
                        "accentColorHex" to gift.accentColorHex,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(items.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
