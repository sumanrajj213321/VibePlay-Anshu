package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FollowEdge
import com.example.data.model.PrivateAccount
import com.example.data.model.RoomCreationLock
import com.example.data.model.UserProfile
import com.example.data.model.VibeCatalogDefaults
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class UserRepository(
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


    private suspend fun ensureWelcomeInbox(uid: String, username: String, avatarUrl: String) {
        val botUid = "VIBEPLAY_OFFICIAL_BOT"
        val botName = "VibePlay Official"
        val conversationId = "bot_${uid}"
        val conversationRef = db.collection("conversations").document(conversationId)
        val messageRef = conversationRef.collection("messages").document("welcome_10_lakh_bonus")
        if (!conversationRef.get().await().exists()) {
            conversationRef.set(mapOf(
                "conversationType" to "DIRECT", "groupName" to "", "groupAvatarUrl" to "",
                "ownerId" to "", "adminIds" to emptyList<String>(),
                "participantIds" to listOf(uid, botUid),
                "participantNames" to mapOf(uid to username, botUid to botName),
                "participantAvatars" to mapOf(uid to avatarUrl, botUid to ""),
                "lastMessage" to "🎉 10 lakh coins welcome bonus credited!",
                "lastSenderId" to botUid, "unreadBy" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )).await()
        }
        if (!messageRef.get().await().exists()) {
            messageRef.set(mapOf(
                "participantIds" to listOf(uid, botUid), "senderId" to botUid,
                "recipientId" to uid,
                "senderName" to botName, "senderAvatarUrl" to "", "type" to "SYSTEM",
                "text" to "🎉 10 LAKH COINS WELCOME BONUS!\n\nCongratulations! 🥳 VibePlay ki taraf se aapke account mein 10,00,000 bonus coins add kiye gaye hain.\n\n❤️ Enjoy VibePlay, send gifts and have fun!\n\n— Team VibePlay",
                "read" to false, "createdAt" to FieldValue.serverTimestamp()
            )).await()
        }
    }

    private suspend fun grantWelcomeBonusToExistingUsers() {
        var cursor: String? = null
        do {
            var query = db.collection("users").orderBy(FieldPath.documentId()).limit(200)
            if (!cursor.isNullOrBlank()) query = query.startAfter(cursor)
            val page = query.get().await()
            for (item in page.documents) {
                val uid = item.id
                val ref = db.collection("users").document(uid)
                val shouldCreateInbox = db.runTransaction { tx ->
                    val snap = tx.get(ref)
                    if (!snap.exists()) false
                    else if (snap.getBoolean("welcomeBonusGranted") == true) true
                    else {
                        val balance = snap.getLong("coinBalance") ?: 0L
                        require(balance >= 0L && balance <= Long.MAX_VALUE - 1_000_000L)
                        tx.update(ref, mapOf(
                            "coinBalance" to balance + 1_000_000L,
                            "welcomeBonusGranted" to true,
                            "welcomeBonusGrantedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                        true
                    }
                }.await()
                if (shouldCreateInbox) ensureWelcomeInbox(uid, item.getString("username") ?: "VibeStar", item.getString("avatarUrl") ?: "")
            }
            cursor = page.documents.lastOrNull()?.id
        } while (page.size() == 200)
    }

    suspend fun ensureCurrentUserProfile(user: FirebaseUser): Result<UserProfile> {
        val docPath = "users/${user.uid}"
        return try {
            val userRef = db.collection("users").document(user.uid)
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "VibeStar")
                    .take(30)
                    .let { if (it.length < 2) "VibeStar" else it }
                val numericCode = (abs(user.uid.hashCode()) % 900000) + 100000
                val vibeId = "VP-$numericCode"
                val payload = mapOf(
                    "userId" to user.uid,
                    "vibeId" to vibeId,
                    "username" to safeName,
                    "bio" to "Ready to vibe in voice rooms!",
                    "avatarUrl" to (user.photoUrl?.toString() ?: ""),
                    "coverUrl" to "",
                    "gender" to "UNSPECIFIED",
                    "genderChangeCount" to 0L,
                    "age" to 0L,
                    "showAge" to false,
                    "showGender" to true,
                    "showOnlineStatus" to true,
                    "online" to true,
                    "level" to 1L,
                    "xp" to 0L,
                    "vipLevel" to 0L,
                    "ownedTitleIds" to emptyList<String>(),
                    "activeTitleId" to "",
                    "ownedFrameIds" to emptyList<String>(),
                    "activeFrameId" to "",
                    "coinBalance" to 1_000_500L,
                    "welcomeBonusGranted" to true,
                    "welcomeBonusGrantedAt" to FieldValue.serverTimestamp(),
                    "totalGiftsReceived" to 0L,
                    "totalGiftsSent" to 0L,
                    "totalGiftCoinsReceived" to 0L,
                    "totalGiftCoinsSent" to 0L,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "accountStatus" to "ACTIVE",
                    "role" to "USER",
                    "lastDailyClaimDay" to "",
                    "hasCreatedRoom" to false,
                    "createdRoomId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()

                val privatePayload = mapOf(
                    "userId" to user.uid,
                    "email" to (user.email ?: ""),
                    "blockedUserIds" to emptyList<String>(),
                    "pinnedConversationIds" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.collection("private").document("account").set(privatePayload).await()
            } else {
                // Do not mutate an existing profile during sign-in.
            }

            // Ensure the deterministic official welcome conversation/message exists.
            // Safe to retry after logout, reinstall, or temporary network failure.
            val latestProfile = userRef.get().await()
            if (latestProfile.getBoolean("welcomeBonusGranted") == true) {
                ensureWelcomeInbox(
                    user.uid,
                    user.displayName?.takeIf { it.isNotBlank() } ?: latestProfile.getString("username") ?: "VibeStar",
                    user.photoUrl?.toString() ?: latestProfile.getString("avatarUrl") ?: ""
                )
            }

            // One-time automatic coin grant for the official account.
            // Firestore rules restrict this privileged update to the admin UID.
            if (user.uid == "1fLIHgOyAJV6LNbHBXNeVg9JR6v1") {
                db.runTransaction { transaction ->
                    val officialSnapshot = transaction.get(userRef)
                    if (officialSnapshot.exists() &&
                        officialSnapshot.getBoolean("officialCoinsGranted") != true
                    ) {
                        transaction.update(
                            userRef,
                            mapOf(
                                "coinBalance" to 1_000_000_000L,
                                "officialCoinsGranted" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
            }

            // Spark-plan migration: the official account grants each existing user once.
            if (user.uid == "1fLIHgOyAJV6LNbHBXNeVg9JR6v1") {
                try { grantWelcomeBonusToExistingUsers() } catch (_: Exception) {
                    // Retry safely on the next official-account sign-in.
                }
            }

            val refreshed = userRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(userId = user.uid, username = user.displayName ?: "VibeStar")
            Result.success(refreshed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun getUserProfileOnce(userId: String): Result<UserProfile?> {
        val docPath = "users/$userId"
        return try {
            requireUserId()
            val snap = db.collection("users").document(userId).get().await()
            val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    fun observeGiftRankings(): Flow<List<UserProfile>> =
        db.collection("users")
            .whereEqualTo("accountStatus", "ACTIVE")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeActiveUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .whereEqualTo("accountStatus", "ACTIVE")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.level * 1000L + it.totalGiftsReceived }
            }

    fun observePrivateAccount(userId: String): Flow<PrivateAccount?> =
        db.collection("users").document(userId).collection("private").document("account")
            .snapshots()
            .map { snap ->
                snap.toObject(PrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeRoomCreationLock(userId: String): Flow<RoomCreationLock?> =
        db.collection("room_creation_locks").document(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(RoomCreationLock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeFollowing(userId: String): Flow<List<FollowEdge>> =
        db.collection("users").document(userId).collection("following")
            .snapshots()
            .map { snap ->
                snap.toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeFollowers(userId: String): Flow<List<FollowEdge>> =
        db.collection("users").document(userId).collection("followers")
            .snapshots()
            .map { snap ->
                snap.toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    private suspend fun saveAvatarToFirestore(uid: String, uri: Uri) {
        val resolver = FirebaseApp.getInstance().applicationContext.contentResolver
        val input = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Could not read the selected photo.")
        val original = input.use { BitmapFactory.decodeStream(it) } ?: throw IllegalArgumentException("Selected photo is not a supported image.")
        val maxSide = 512
        val scale = minOf(1f, maxSide.toFloat() / maxOf(original.width, original.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(original, (original.width * scale).toInt().coerceAtLeast(1), (original.height * scale).toInt().coerceAtLeast(1), true) else original
        var quality = 82
        var bytes: ByteArray
        do {
            val output = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            bytes = output.toByteArray()
            quality -= 8
        } while (bytes.size > 700 * 1024 && quality >= 42)
        if (bytes.size > 700 * 1024) throw IllegalArgumentException("Please choose a smaller photo. Profile photos must compress below 700 KB.")
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        db.collection("users").document(uid).collection("avatar").document("image").set(
            mapOf("data" to encoded, "contentType" to "image/jpeg", "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        if (resized !== original) resized.recycle()
        original.recycle()
    }
    suspend fun updateProfile(
        username: String,
        bio: String,
        avatarUrl: String,
        coverUrl: String,
        gender: String,
        age: Long,
        showAge: Boolean,
        showGender: Boolean,
        showOnlineStatus: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            val cleanName = username.trim().take(30)
            require(cleanName.length >= 2) { "Username must be at least 2 characters." }
            require(age == 0L || age in 13L..120L) { "Please enter an exact age between 13 and 120 (or leave blank)." }
            val cleanBio = bio.trim().take(200)
            val savedAvatarUrl = if (avatarUrl.startsWith("content://")) {
                saveAvatarToFirestore(currentUid, Uri.parse(avatarUrl))
                "firestore-avatar://$currentUid"
            } else {
                avatarUrl
            }
            val profileRef = db.collection("users").document(currentUid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(profileRef)
                require(snapshot.exists()) { "Profile not found." }
                val oldGender = snapshot.getString("gender") ?: "UNSPECIFIED"
                val genderChanges = snapshot.getLong("genderChangeCount") ?: 0L
                require(oldGender == gender || genderChanges < 1L) {
                    "Gender can only be changed once in a lifetime. This change has already been used."
                }
                val updates = mutableMapOf<String, Any>(
                    "username" to cleanName,
                    "bio" to cleanBio,
                    "avatarUrl" to savedAvatarUrl.take(500),
                    "coverUrl" to coverUrl.take(500),
                    "age" to age,
                    "showAge" to showAge,
                    "showGender" to showGender,
                    "showOnlineStatus" to showOnlineStatus,
                    "genderChangeCount" to if (oldGender == gender) genderChanges else genderChanges + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (oldGender != gender) updates["gender"] = gender
                transaction.update(profileRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(online: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val docPath = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "online" to online,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimDailyLoginReward(profile: UserProfile): Result<Long> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()
            val userRef = db.collection("users").document(uid)

            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val lastClaim = snap.getString("lastDailyClaimDay") ?: ""
                if (lastClaim == today) {
                    throw IllegalStateException("Today's daily reward has already been claimed.")
                }
                val currentCoins = snap.getLong("coinBalance") ?: profile.coinBalance
                val currentXp = snap.getLong("xp") ?: profile.xp
                val newXp = currentXp + 50L
                val newLevel = com.example.data.model.LevelProgression.levelForXp(newXp)
                tx.update(
                    userRef,
                    mapOf(
                        "coinBalance" to currentCoins + 150L,
                        "lastDailyClaimDay" to today,
                        "xp" to newXp,
                        "level" to newLevel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(150L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }


    suspend fun purchaseShopCosmetic(category: String, itemId: String): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            val item = when (category) {
                "TITLE" -> com.example.data.model.VibeShopCatalog.titles.firstOrNull { it.id == itemId }
                "FRAME" -> com.example.data.model.VibeShopCatalog.frames.firstOrNull { it.id == itemId }
                else -> null
            } ?: throw IllegalArgumentException("Invalid shop item.")
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                require(snap.exists()) { "Profile not found." }
                val ownedField = if (category == "TITLE") "ownedTitleIds" else "ownedFrameIds"
                val expiryField = if (category == "TITLE") "titleExpiresAt" else "frameExpiresAt"
                val activeField = if (category == "TITLE") "activeTitleId" else "activeFrameId"
                val owned = (snap.get(ownedField) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val expiries = (snap.get(expiryField) as? Map<*, *>)?.entries
                    ?.associate { it.key.toString() to (it.value as? Timestamp) } ?: emptyMap()
                val currentExpiry = expiries[itemId]?.toDate()?.time ?: 0L
                require(currentExpiry <= System.currentTimeMillis()) { "You already own this item. It expires first." }
                val coins = snap.getLong("coinBalance") ?: 0L
                require(coins >= item.coinPrice) { "Not enough coins. Need ${item.coinPrice} coins." }
                val durationDays = if (category == "TITLE") 15L else 7L
                val expiresAt = Timestamp(java.util.Date(System.currentTimeMillis() + durationDays * 24L * 60L * 60L * 1000L))
                val updatedExpiries = expiries.filterValues { it != null }.mapValues { it.value!! }.toMutableMap()
                updatedExpiries[itemId] = expiresAt
                tx.update(userRef, mapOf(
                    "coinBalance" to coins - item.coinPrice,
                    ownedField to if (itemId in owned) owned else FieldValue.arrayUnion(itemId),
                    expiryField to updatedExpiries,
                    activeField to itemId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun equipShopCosmetic(category: String, itemId: String?): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            require(category == "TITLE" || category == "FRAME")
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                require(snap.exists()) { "Profile not found." }
                val ownedField = if (category == "TITLE") "ownedTitleIds" else "ownedFrameIds"
                val activeField = if (category == "TITLE") "activeTitleId" else "activeFrameId"
                val expiryField = if (category == "TITLE") "titleExpiresAt" else "frameExpiresAt"
                val owned = (snap.get(ownedField) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val expiryMap = (snap.get(expiryField) as? Map<*, *>) ?: emptyMap<Any, Any>()
                val expiry = itemId?.let { expiryMap[it] as? Timestamp }
                require(itemId.isNullOrBlank() || (itemId in owned && expiry?.toDate()?.after(java.util.Date()) == true)) { "This item has expired. Purchase it again." }
                tx.update(userRef, mapOf(
                    activeField to (itemId ?: ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun upgradeVipTier(profile: UserProfile, targetTierLevel: Int): Result<Unit> {
        val docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val tier = VibeCatalogDefaults.vipTiers.firstOrNull { it.level == targetTierLevel }
                ?: throw IllegalArgumentException("Invalid VIP tier")
            val userRef = db.collection("users").document(uid)

            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val storedVip = snap.getLong("vipLevel") ?: profile.vipLevel
                val storedExpiry = snap.getTimestamp("vipExpiresAt")
                val currentVip = if (storedExpiry?.toDate()?.after(java.util.Date()) == true) storedVip else 0L
                val currentCoins = snap.getLong("coinBalance") ?: profile.coinBalance
                require(targetTierLevel > currentVip) { "You already hold this VIP tier or higher." }
                require(currentCoins >= tier.coinPrice) { "Insufficient coins. Need ${tier.coinPrice} coins." }
                tx.update(
                    userRef,
                    mapOf(
                        "coinBalance" to (currentCoins - tier.coinPrice),
                        "vipLevel" to targetTierLevel.toLong(),
                        "vipExpiresAt" to Timestamp(java.util.Date(System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L)),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        currentUser: UserProfile,
        targetUser: UserProfile,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val path = "users/${currentUser.userId}/following/${targetUser.userId}"
        return try {
            val uid = requireUserId()
            check(uid == currentUser.userId && uid != targetUser.userId)
            val myUserRef = db.collection("users").document(uid)
            val targetUserRef = db.collection("users").document(targetUser.userId)
            val myFollowingRef = myUserRef.collection("following").document(targetUser.userId)
            val theirFollowerRef = targetUserRef.collection("followers").document(uid)

            db.runTransaction { tx ->
                val mySnap = tx.get(myUserRef)
                val targetSnap = tx.get(targetUserRef)
                val myFollowingSnap = tx.get(myFollowingRef)
                val theirFollowerSnap = tx.get(theirFollowerRef)

                val myFollowingCount = mySnap.getLong("followingCount") ?: currentUser.followingCount
                val targetFollowersCount = targetSnap.getLong("followersCount") ?: targetUser.followersCount
                val isActuallyFollowing = myFollowingSnap.exists()

                if (isActuallyFollowing || (currentlyFollowing && theirFollowerSnap.exists())) {
                    if (myFollowingSnap.exists()) tx.delete(myFollowingRef)
                    if (theirFollowerSnap.exists()) tx.delete(theirFollowerRef)
                    if (myFollowingSnap.exists()) {
                        tx.update(
                            myUserRef,
                            mapOf(
                                "followingCount" to (myFollowingCount - 1L).coerceAtLeast(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (theirFollowerSnap.exists()) {
                        tx.update(
                            targetUserRef,
                            mapOf(
                                "followersCount" to (targetFollowersCount - 1L).coerceAtLeast(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else if (!myFollowingSnap.exists()) {
                    tx.set(
                        myFollowingRef,
                        mapOf(
                            "peerId" to targetUser.userId,
                            "peerName" to targetUser.username,
                            "peerAvatarUrl" to targetUser.avatarUrl,
                            "peerGender" to targetUser.gender,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        theirFollowerRef,
                        mapOf(
                            "peerId" to uid,
                            "peerName" to currentUser.username,
                            "peerAvatarUrl" to currentUser.avatarUrl,
                            "peerGender" to currentUser.gender,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        myUserRef,
                        mapOf(
                            "followingCount" to myFollowingCount + 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        targetUserRef,
                        mapOf(
                            "followersCount" to targetFollowersCount + 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val notifRef = db.collection("notifications").document()
                    tx.set(
                        notifRef,
                        mapOf(
                            "recipientId" to targetUser.userId,
                            "senderId" to uid,
                            "senderName" to currentUser.username,
                            "type" to "FOLLOW",
                            "title" to "New Follower",
                            "body" to "${currentUser.username} started following you.",
                            "targetId" to uid,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun blockOrUnblockUser(targetUserId: String, block: Boolean, currentBlocked: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/private/account"
        return try {
            val currentUid = requireUserId()
            val updated = if (block) {
                (currentBlocked + targetUserId).distinct().take(200)
            } else {
                currentBlocked.filter { it != targetUserId }
            }
            val accountRef = db.collection("users").document(currentUid).collection("private").document("account")
            db.runTransaction { tx ->
                val snap = tx.get(accountRef)
                @Suppress("UNCHECKED_CAST")
                val existingPins = (snap.get("pinnedConversationIds") as? List<String>) ?: emptyList()
                val existingEmail = snap.getString("email") ?: (auth.currentUser?.email ?: "")
                tx.set(
                    accountRef,
                    mapOf(
                        "userId" to currentUid,
                        "email" to existingEmail,
                        "blockedUserIds" to updated,
                        "pinnedConversationIds" to existingPins,
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
}
