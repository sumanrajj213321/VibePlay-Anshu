package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftTransaction
import com.example.data.model.LevelProgression
import com.example.data.model.RoomCreationLock
import com.example.data.model.RoomListener
import com.example.data.model.RoomMessage
import com.example.data.model.SpeakerRequest
import com.example.data.model.UserProfile
import com.example.data.model.VibeShopCatalog
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class RoomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val createRoomMutex = Mutex()
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun hashPasscode(raw: String): String {
        if (raw.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun observeLiveRooms(): Flow<List<VoiceRoom>> =
        db.collection("rooms")
            .whereEqualTo("status", "LIVE")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VoiceRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .flatMapLatest { rooms ->
                if (rooms.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val onlineUsersFlow = db.collection("users")
                        .whereEqualTo("accountStatus", "ACTIVE")
                        .whereEqualTo("online", true)
                        .snapshots()
                        .map { users -> users.documents.map { it.id }.toSet() }
                    val roomCountFlows = rooms.map { room ->
                        val roomRef = db.collection("rooms").document(room.id)
                        val sessionsFlow = roomRef.collection("room_sessions").snapshots()
                            .map { sessions -> sessions.documents.map { it.id }.toSet() }
                        val seatsFlow = roomRef.collection("seats").snapshots()
                            .map { seats -> seats.documents.mapNotNull { it.getString("occupantId") }.filter { it.isNotBlank() }.toSet() }
                        combine(sessionsFlow, seatsFlow, onlineUsersFlow) { sessionIds, seatedIds, onlineUserIds ->
                            // A stored session/seat alone is not proof that the user is currently online.
                            val activeSessionIds = sessionIds.intersect(onlineUserIds)
                            val activeSeatedIds = seatedIds.intersect(activeSessionIds)
                            val speakerCount = activeSeatedIds.size.toLong()
                            val listenerCount = (activeSessionIds - activeSeatedIds).size.toLong()
                            room.copy(
                                listenerCount = listenerCount,
                                activeSpeakersCount = speakerCount
                            )
                        }
                    }
                    combine(roomCountFlows) { updatedRooms ->
                        updatedRooms.toList().sortedByDescending {
                            (if (it.featured) 100000L else 0L) + it.listenerCount + it.activeSpeakersCount
                        }
                    }
                }
            }

    fun observeRoom(roomId: String): Flow<VoiceRoom?> =
        db.collection("rooms").document(roomId)
            .snapshots()
            .map { it.toObject(VoiceRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeNineSeats(roomId: String): Flow<List<VoiceSeat>> {
        val roomRef = db.collection("rooms").document(roomId)
        val seatsFlow = roomRef.collection("seats")
            .whereGreaterThanOrEqualTo("seatIndex", 0L)
            .whereLessThanOrEqualTo("seatIndex", 8L)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VoiceSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        val sessionsFlow = roomRef.collection("room_sessions").snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        val onlineUsersFlow = db.collection("users")
            .whereEqualTo("accountStatus", "ACTIVE")
            .whereEqualTo("online", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    val profile = document.toObject(UserProfile::class.java) ?: return@mapNotNull null
                    val activeFrame = VibeShopCatalog.frames.firstOrNull {
                        it.id == profile.activeFrameId && profile.isFrameActive(it.id)
                    }
                    document.id to (activeFrame?.colorArgb)
                }.toMap()
            }
        return combine(seatsFlow, sessionsFlow, onlineUsersFlow, observeRoom(roomId)) { seats, sessionIds, onlineFrameColors, room ->
            val activeSessionIds = sessionIds.intersect(onlineFrameColors.keys)
            seats.filter { seat ->
                seat.isHostSeat ||
                    seat.occupantId.isBlank() ||
                    seat.occupantId in activeSessionIds ||
                    (room != null && !room.effectiveOwnerOnline && seat.occupantId == room.effectiveOwnerId)
            }.map { seat ->
                seat.copy(occupantFrameColorArgb = onlineFrameColors[seat.occupantId] ?: seat.occupantFrameColorArgb)
            }
        }
    }

    fun observeSpeakerRequests(roomId: String): Flow<List<SpeakerRequest>> =
        db.collection("rooms").document(roomId).collection("speaker_requests")
            .whereEqualTo("status", "PENDING")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SpeakerRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeMySpeakerRequest(roomId: String, userId: String): Flow<SpeakerRequest?> =
        db.collection("rooms").document(roomId).collection("speaker_requests")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SpeakerRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .firstOrNull { it.status == "PENDING" || it.status == "INVITED" }
            }

    fun observeRoomMessages(roomId: String): Flow<List<RoomMessage>> = flow {
        val uid = requireUserId()
        val sessionSnapshot = db.collection("rooms").document(roomId)
            .collection("room_sessions").document(uid)
            .get().await()
        if (!sessionSnapshot.exists()) return@flow
        val joinedAt = sessionSnapshot.getTimestamp("joinedAt") ?: return@flow

        // Query from the latest session boundary. A new session overwrites joinedAt,
        // so messages from every earlier visit stay hidden after rejoining.
        emitAll(
            db.collection("rooms").document(roomId).collection("messages")
                .whereGreaterThanOrEqualTo("createdAt", joinedAt)
                .whereIn("type", listOf("TEXT", "GIFT", "SYSTEM"))
                .orderBy("createdAt")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RoomMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { message ->
                            message.type in listOf("TEXT", "GIFT", "SYSTEM") &&
                                message.createdAt != null &&
                                message.createdAt.compareTo(joinedAt) >= 0
                        }
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    fun observeListeners(roomId: String): Flow<List<RoomListener>> {
        val listenersFlow = db.collection("rooms").document(roomId).collection("listeners")
            .whereEqualTo("role", "LISTENER")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(RoomListener::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        val onlineUsersFlow = db.collection("users")
            .whereEqualTo("accountStatus", "ACTIVE")
            .whereEqualTo("online", true)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }

        val sessionsFlow = db.collection("rooms").document(roomId).collection("room_sessions")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }

        return combine(listenersFlow, onlineUsersFlow, sessionsFlow) { listeners, onlineIds, activeSessionIds ->
            listeners.filter { it.userId in onlineIds && it.userId in activeSessionIds }
        }
    }

    fun observeActiveGifts(): Flow<List<GiftCatalogItem>> =
        db.collection("gifts_catalog")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GiftCatalogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.coinCost }
            }

    fun observeGiftHistory(userId: String): Flow<List<GiftTransaction>> =
        db.collection("gift_transactions")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GiftTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    suspend fun syncExistingRoomCreationLock(userId: String): Result<RoomCreationLock?> {
        val lockRef = db.collection("room_creation_locks").document(userId)
        val userRef = db.collection("users").document(userId)
        return try {
            requireUserId()
            val existingLockSnap = lockRef.get().await()
            if (existingLockSnap.exists()) {
                val lock = existingLockSnap.toObject(
                    RoomCreationLock::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val userSnap = userRef.get().await()
                if (userSnap.exists() && userSnap.getBoolean("hasCreatedRoom") != true && lock != null && lock.roomId.isNotBlank()) {
                    userRef.update(
                        mapOf(
                            "hasCreatedRoom" to true,
                            "createdRoomId" to lock.roomId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                return Result.success(lock)
            }

            // Handle pre-existing users who created rooms prior to room_creation_locks tracking
            val byHost = db.collection("rooms")
                .whereEqualTo("status", "LIVE")
                .whereEqualTo("hostId", userId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
            val preExistingRoomDoc = byHost ?: db.collection("rooms")
                .whereEqualTo("status", "LIVE")
                .whereEqualTo("ownerId", userId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (preExistingRoomDoc != null) {
                val existingRoomId = preExistingRoomDoc.id
                db.runTransaction { tx ->
                    val freshLock = tx.get(lockRef)
                    val freshUser = tx.get(userRef)
                    if (!freshLock.exists()) {
                        tx.set(
                            lockRef,
                            mapOf(
                                "userId" to userId,
                                "roomId" to existingRoomId,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (freshUser.exists() && freshUser.getBoolean("hasCreatedRoom") != true) {
                        tx.update(
                            userRef,
                            mapOf(
                                "hasCreatedRoom" to true,
                                "createdRoomId" to existingRoomId,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
                val synced = lockRef.get().await().toObject(
                    RoomCreationLock::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                return Result.success(synced)
            }

            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, lockRef.path)
            Result.failure(e)
        }
    }

    suspend fun createVoiceRoom(
        host: UserProfile,
        title: String,
        announcement: String,
        category: String,
        coverKey: String,
        locked: Boolean,
        rawPassword: String
    ): Result<String> = createRoomMutex.withLock {
        val roomRef = db.collection("rooms").document()
        val path = roomRef.path
        try {
            val uid = requireUserId()
            check(uid == host.userId)
            val cleanTitle = title.trim().take(60)
            require(cleanTitle.length >= 2) { "Room title must be at least 2 characters." }
            val hasPassword = rawPassword.isNotBlank()
            val passcodeHash = hashPasscode(rawPassword)

            val existingLock = syncExistingRoomCreationLock(uid).getOrNull()
            if (existingLock != null || host.hasCreatedRoom) {
                return@withLock Result.failure(IllegalStateException(ONE_ROOM_PER_USER_MESSAGE))
            }

            val userRef = db.collection("users").document(uid)
            val lockRef = db.collection("room_creation_locks").document(uid)

            db.runTransaction { tx ->
                val lockSnap = tx.get(lockRef)
                val userSnap = tx.get(userRef)
                if (lockSnap.exists() || userSnap.getBoolean("hasCreatedRoom") == true) {
                    throw IllegalStateException(ONE_ROOM_PER_USER_MESSAGE)
                }

                // 1. Permanently claim one-time room creation lock keyed by authenticated UID
                tx.set(
                    lockRef,
                    mapOf(
                        "userId" to uid,
                        "roomId" to roomRef.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Persist lifetime flag on user document in the same atomic transaction
                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "hasCreatedRoom" to true,
                            "createdRoomId" to roomRef.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 3. Create the single VoiceRoom document
                tx.set(
                    roomRef,
                    mapOf(
                        "creatorId" to uid,
                        "hostId" to uid,
                        "hostName" to host.username,
                        "hostAvatarUrl" to host.avatarUrl,
                        "ownerId" to uid,
                        "ownerName" to host.username,
                        "ownerOnline" to true,
                        "adminIds" to emptyList<String>(),
                        "activeAdminIds" to emptyList<String>(),
                        "title" to cleanTitle,
                        "announcement" to announcement.trim().take(200),
                        "category" to category,
                        "coverKey" to coverKey,
                        "tags" to listOf(category, "Live"),
                        "listenerCount" to 0L,
                        "activeSpeakersCount" to 1L,
                        "locked" to locked,
                        "hasPassword" to hasPassword,
                        "passcodeHash" to passcodeHash,
                        "status" to "LIVE",
                        "featured" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 4. Seat 0: Dedicated Host/Owner Seat
                tx.set(
                    roomRef.collection("seats").document("0"),
                    mapOf(
                        "seatIndex" to 0L,
                        "role" to "HOST",
                        "occupantId" to uid,
                        "occupantName" to host.username,
                        "occupantAvatarUrl" to host.avatarUrl,
                        "occupantVipLevel" to host.vipLevel,
                        "occupantGender" to host.gender,
                        "muted" to false,
                        "seatLocked" to false,
                        "speaking" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 5. Seats 1..8: Strictly 8 regular speaker seats
                for (index in 1..VoiceStage.SPEAKER_SEAT_COUNT) {
                    tx.set(
                        roomRef.collection("seats").document(index.toString()),
                        mapOf(
                            "seatIndex" to index.toLong(),
                            "role" to "SPEAKER",
                            "occupantId" to "",
                            "occupantName" to "",
                            "occupantAvatarUrl" to "",
                            "occupantVipLevel" to 0L,
                            "occupantGender" to "UNSPECIFIED",
                            "muted" to true,
                            "seatLocked" to false,
                            "speaking" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 6. Create the host's room session in this transaction.
                // The message rule validates this with existsAfter().
                tx.set(
                    roomRef.collection("room_sessions").document(uid),
                    mapOf(
                        "userId" to uid,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 7. System welcome chat message
                tx.set(
                    roomRef.collection("messages").document(),
                    mapOf(
                        "senderId" to uid,
                        "senderName" to host.username,
                        "senderVipLevel" to host.vipLevel,
                        "senderGender" to host.gender,
                        "text" to "Room created! Welcome to $cleanTitle.",
                        "type" to "SYSTEM",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(roomRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            val uid = auth.currentUser?.uid
            val alreadyLocked = if (!uid.isNullOrBlank()) {
                runCatching { db.collection("room_creation_locks").document(uid).get().await().exists() }.getOrDefault(false)
            } else {
                false
            }
            if (alreadyLocked) {
                Result.failure(IllegalStateException(ONE_ROOM_PER_USER_MESSAGE))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun joinAsListener(room: VoiceRoom, user: UserProfile): Result<Unit> {
        val listenerPath = "rooms/${room.id}/listeners/${user.userId}"
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            val roomRef = db.collection("rooms").document(room.id)
            val listenerRef = roomRef.collection("listeners").document(uid)
            val sessionRef = roomRef.collection("room_sessions").document(uid)
            val seat0Ref = roomRef.collection("seats").document("0")

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                if (!roomSnap.exists()) return@runTransaction
                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java) ?: room
                if (loadedRoom.status != "LIVE") return@runTransaction
                val listenerSnap = tx.get(listenerRef)
                val seatSnaps = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }

                tx.set(
                    sessionRef,
                    mapOf(
                        "userId" to uid,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )

                // The room-entry event is the only automatic presence message shown in chat.
                tx.set(
                    roomRef.collection("messages").document(),
                    mapOf(
                        "senderId" to uid,
                        "senderName" to user.username,
                        "senderAvatarUrl" to user.avatarUrl,
                        "senderVipLevel" to user.vipLevel,
                        "senderGender" to user.gender,
                        "text" to "${user.username} entered the room.",
                        "type" to "SYSTEM",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                if (loadedRoom.isOwner(uid)) {
                    if (listenerSnap.exists()) {
                        tx.delete(listenerRef)
                    }
                    val occupiedSpeakers = seatSnaps.count { !(it.getString("occupantId").orEmpty()).isBlank() }
                    val updatedListeners = if (listenerSnap.exists()) {
                        (loadedRoom.listenerCount - 1L).coerceAtLeast(0L)
                    } else {
                        loadedRoom.listenerCount
                    }
                    tx.update(
                        seat0Ref,
                        mapOf(
                            "occupantId" to uid,
                            "occupantName" to user.username,
                            "occupantAvatarUrl" to user.avatarUrl,
                            "occupantVipLevel" to user.vipLevel,
                            "occupantGender" to user.gender,
                            "muted" to false,
                            "speaking" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        roomRef,
                        mapOf(
                            "ownerOnline" to true,
                            "ownerName" to user.username,
                            "hostName" to user.username,
                            "hostAvatarUrl" to user.avatarUrl,
                            "listenerCount" to updatedListeners,
                            "activeSpeakersCount" to (1L + occupiedSpeakers).coerceIn(1L, 9L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val alreadySeated = seatSnaps.any { it.getString("occupantId") == uid }
                    var updatedListenerCount = loadedRoom.listenerCount
                    if (!alreadySeated) {
                        tx.set(
                            listenerRef,
                            mapOf(
                                "userId" to uid,
                                "username" to user.username,
                                "avatarUrl" to user.avatarUrl,
                                "vipLevel" to user.vipLevel,
                                "gender" to user.gender,
                                "role" to "LISTENER",
                                "joinedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        if (!listenerSnap.exists()) {
                            updatedListenerCount += 1L
                        }
                    } else if (listenerSnap.exists()) {
                        tx.delete(listenerRef)
                        updatedListenerCount = (updatedListenerCount - 1L).coerceAtLeast(0L)
                    }

                    val nextActiveAdmins = if (uid in loadedRoom.adminIds && uid !in loadedRoom.activeAdminIds) {
                        (loadedRoom.activeAdminIds + uid).distinct()
                    } else {
                        loadedRoom.activeAdminIds
                    }
                    if (!listenerSnap.exists() || nextActiveAdmins != loadedRoom.activeAdminIds) {
                        tx.update(
                            roomRef,
                            mapOf(
                                "listenerCount" to updatedListenerCount,
                                "activeAdminIds" to nextActiveAdmins,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, listenerPath)
            Result.failure(e)
        }
    }

    suspend fun cleanupCurrentUserPresenceInLiveRooms(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val liveRooms = db.collection("rooms")
                .whereEqualTo("status", "LIVE")
                .get()
                .await()

            for (roomDoc in liveRooms.documents) {
                val room = roomDoc.toObject(VoiceRoom::class.java) ?: continue
                val roomRef = roomDoc.reference
                val listenerExists = roomRef.collection("listeners").document(uid).get().await().exists()
                val sessionExists = roomRef.collection("room_sessions").document(uid).get().await().exists()
                val seats = roomRef.collection("seats").get().await()
                val occupiesSeat = seats.documents.any { it.getString("occupantId") == uid }
                val isOnlineOwner = room.isOwner(uid) && room.effectiveOwnerOnline

                if (listenerExists || sessionExists || occupiesSeat || isOnlineOwner) {
                    leaveRoom(room, uid, emptyList()).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(room: VoiceRoom, userId: String, currentSeats: List<VoiceSeat>): Result<Unit> {
        val path = "rooms/${room.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val roomRef = db.collection("rooms").document(room.id)
            val listenerRef = roomRef.collection("listeners").document(uid)
            val sessionRef = roomRef.collection("room_sessions").document(uid)
            val reqRef = roomRef.collection("speaker_requests").document(uid)
            val seat0Ref = roomRef.collection("seats").document("0")

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                if (!roomSnap.exists()) return@runTransaction
                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java) ?: room
                val listenerSnap = tx.get(listenerRef)
                val reqSnap = tx.get(reqRef)
                val seat0Snap = tx.get(seat0Ref)
                val seatSnaps = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }

                if (loadedRoom.status != "LIVE") return@runTransaction

                tx.delete(sessionRef)

                var updatedListenerCount = loadedRoom.listenerCount
                var occupiedSpeakers = seatSnaps.count { !(it.getString("occupantId").orEmpty()).isBlank() }
                val remainingActiveAdmins = loadedRoom.activeAdminIds
                    .filter { it != uid && it in loadedRoom.adminIds }

                if (listenerSnap.exists()) {
                    tx.delete(listenerRef)
                    updatedListenerCount = (updatedListenerCount - 1L).coerceAtLeast(0L)
                }

                if (reqSnap.exists() && reqSnap.getString("status") == "PENDING") {
                    tx.delete(reqRef)
                }

                if (loadedRoom.isOwner(uid)) {
                    val hasActiveAdmin = remainingActiveAdmins.isNotEmpty()
                    val ownerName = loadedRoom.effectiveOwnerName
                    if (seat0Snap.exists()) {
                        tx.update(
                            seat0Ref,
                            mapOf(
                                "occupantId" to loadedRoom.effectiveOwnerId,
                                "occupantName" to ownerName,
                                // Keep the last known host photo in the permanent host seat.
                                // The UI fades it and overlays OFFLINE while ownerOnline=false.
                                "occupantAvatarUrl" to loadedRoom.hostAvatarUrl,
                                "occupantVipLevel" to (seat0Snap.getLong("occupantVipLevel") ?: 0L),
                                "occupantGender" to (seat0Snap.getString("occupantGender") ?: "UNSPECIFIED"),
                                "muted" to true,
                                "speaking" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (!hasActiveAdmin) {
                        // Keep the permanent owner room LIVE and joinable without an online admin.
                        seatSnaps.forEach { snap ->
                            if (!snap.getString("occupantId").isNullOrBlank()) {
                                tx.update(
                                    snap.reference,
                                    mapOf(
                                        "occupantId" to "",
                                        "occupantName" to "",
                                        "occupantAvatarUrl" to "",
                                        "occupantVipLevel" to 0L,
                                        "occupantGender" to "UNSPECIFIED",
                                        "muted" to true,
                                        "speaking" to false,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }
                    tx.update(
                        roomRef,
                        mapOf(
                            "ownerOnline" to false,
                            "status" to "LIVE",
                            "listenerCount" to updatedListenerCount,
                            "activeSpeakersCount" to if (hasActiveAdmin) occupiedSpeakers.toLong().coerceIn(0L, 9L) else 0L,
                            "activeAdminIds" to remainingActiveAdmins,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val myOccupiedSnap = seatSnaps.firstOrNull { it.getString("occupantId") == uid }
                    if (myOccupiedSnap != null) {
                        tx.update(
                            myOccupiedSnap.reference,
                            mapOf(
                                "occupantId" to "",
                                "occupantName" to "",
                                "occupantAvatarUrl" to "",
                                "occupantVipLevel" to 0L,
                                "occupantGender" to "UNSPECIFIED",
                                "muted" to true,
                                "speaking" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        occupiedSpeakers = (occupiedSpeakers - 1).coerceAtLeast(0)
                    }

                    val noActiveManager = !loadedRoom.effectiveOwnerOnline && remainingActiveAdmins.isEmpty()
                    if (noActiveManager && loadedRoom.isRoomAdmin(uid)) {
                        seatSnaps.forEach { snap ->
                            if (snap.id != myOccupiedSnap?.id && !snap.getString("occupantId").isNullOrBlank()) {
                                tx.update(
                                    snap.reference,
                                    mapOf(
                                        "occupantId" to "",
                                        "occupantName" to "",
                                        "occupantAvatarUrl" to "",
                                        "occupantVipLevel" to 0L,
                                        "occupantGender" to "UNSPECIFIED",
                                        "muted" to true,
                                        "speaking" to false,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    val hostSeatedCount = if (loadedRoom.effectiveOwnerOnline) 1L else 0L
                    tx.update(
                        roomRef,
                        mapOf(
                            "listenerCount" to updatedListenerCount,
                            "activeSpeakersCount" to (hostSeatedCount + if (noActiveManager && loadedRoom.isRoomAdmin(uid)) 0L else occupiedSpeakers.toLong()).coerceIn(0L, 9L),
                            "activeAdminIds" to remainingActiveAdmins,
                            "status" to "LIVE",
                            "updatedAt" to FieldValue.serverTimestamp()
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

    suspend fun promoteOrDemoteRoomAdmin(
        roomId: String,
        targetUserId: String,
        targetUsername: String,
        promote: Boolean
    ): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(targetUserId.isNotBlank() && targetUserId != uid) { "Select another user in the room." }
            val roomRef = db.collection("rooms").document(roomId)
            val listenerRef = roomRef.collection("listeners").document(targetUserId)

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java)
                    ?: throw IllegalStateException("Room not found.")
                check(loadedRoom.isOwner(uid)) { "Only the Room Owner can manage Room Admins." }
                val listenerSnap = tx.get(listenerRef)
                val seatSnaps = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }
                val targetInRoom = listenerSnap.exists() ||
                    seatSnaps.any { it.getString("occupantId") == targetUserId }

                val nextAdminIds = if (promote) {
                    (loadedRoom.adminIds + targetUserId).distinct().take(10)
                } else {
                    loadedRoom.adminIds.filter { it != targetUserId }
                }
                val nextActiveAdminIds = if (promote && targetInRoom) {
                    (loadedRoom.activeAdminIds + targetUserId).filter { it in nextAdminIds }.distinct()
                } else {
                    loadedRoom.activeAdminIds.filter { it in nextAdminIds }
                }

                tx.update(
                    roomRef,
                    mapOf(
                        "adminIds" to nextAdminIds,
                        "activeAdminIds" to nextActiveAdminIds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    roomRef.collection("messages").document(),
                    mapOf(
                        "senderId" to uid,
                        "senderName" to loadedRoom.effectiveOwnerName,
                        "senderVipLevel" to 0L,
                        "senderGender" to "UNSPECIFIED",
                        "text" to if (promote) {
                            "$targetUsername is now a Room Admin."
                        } else {
                            "$targetUsername is no longer a Room Admin."
                        },
                        "type" to "SYSTEM",
                        "createdAt" to FieldValue.serverTimestamp()
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

    suspend fun joinSpeakerSeat(
        room: VoiceRoom,
        seatIndex: Int,
        user: UserProfile
    ): Result<Unit> {
        val path = "rooms/${room.id}/seats/$seatIndex"
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            require(seatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) {
                "Speakers can only join speaker seats 1..8."
            }
            require(!room.isOwner(uid)) { "Host already occupies dedicated Host Seat 0." }
            require(!room.locked) { "Room is locked. Request to speak from the Host." }

            val roomRef = db.collection("rooms").document(room.id)
            val listenerRef = roomRef.collection("listeners").document(uid)
            val reqRef = roomRef.collection("speaker_requests").document(uid)

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val seatSnaps = (0..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }
                val listenerSnap = tx.get(listenerRef)
                val reqSnap = tx.get(reqRef)

                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java) ?: room
                check(!loadedRoom.locked) { "Room is locked. Request to speak from the Host." }
                val seats = seatSnaps.mapNotNull { it.toObject(VoiceSeat::class.java) }
                val stage = VoiceStage.buildStrictNineSeatStage(loadedRoom, seats)

                VoiceStage.canAssignUserToSeat(
                    room = loadedRoom,
                    stage = stage,
                    targetSeatIndex = seatIndex,
                    candidateUserId = uid
                ).getOrThrow()

                tx.update(
                    roomRef.collection("seats").document(seatIndex.toString()),
                    mapOf(
                        "occupantId" to uid,
                        "occupantName" to user.username,
                        "occupantAvatarUrl" to user.avatarUrl,
                        "occupantVipLevel" to user.vipLevel,
                        "occupantGender" to user.gender,
                        "muted" to false,
                        "speaking" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                var nextListenerCount = loadedRoom.listenerCount
                if (listenerSnap.exists()) {
                    tx.delete(listenerRef)
                    nextListenerCount = (nextListenerCount - 1L).coerceAtLeast(0L)
                }
                if (reqSnap.exists()) {
                    tx.delete(reqRef)
                }

                val hostCount = if (loadedRoom.effectiveOwnerOnline) 1L else 0L
                val nextActiveSpeakers = (hostCount + stage.speakerSeats.count { it.isOccupied } + 1L).coerceIn(1L, 9L)
                tx.update(
                    roomRef,
                    mapOf(
                        "listenerCount" to nextListenerCount,
                        "activeSpeakersCount" to nextActiveSpeakers,
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

    suspend fun sendSeatInvitation(
        room: VoiceRoom,
        seatIndex: Int,
        userId: String,
        username: String,
        avatarUrl: String,
        vipLevel: Long,
        gender: String = "UNSPECIFIED"
    ): Result<Unit> {
        val path = "rooms/${room.id}/speaker_requests/$userId"
        return try {
            val uid = requireUserId()
            check(room.isManager(uid)) { "Only the Room Owner or a Room Admin can invite users to seats." }
            require(seatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) { "Invalid speaker seat." }
            require(userId.isNotBlank() && userId != uid) { "Choose another room member." }

            val roomRef = db.collection("rooms").document(room.id)
            val requestRef = roomRef.collection("speaker_requests").document(userId)
            val userRef = db.collection("users").document(userId)
            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val requestSnap = tx.get(requestRef)
                val seatSnaps = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { tx.get(roomRef.collection("seats").document(it.toString())) }
                val userSnap = if (gender == "UNSPECIFIED") tx.get(userRef) else null
                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java) ?: room
                check(loadedRoom.isManager(uid)) { "Only the Room Owner or a Room Admin can invite users to seats." }
                check(loadedRoom.status == "LIVE" && !loadedRoom.locked) { "This room is not accepting seat invitations." }
                check(seatSnaps.firstOrNull { it.id == seatIndex.toString() }?.let {
                    it.getString("occupantId").isNullOrBlank() && it.getBoolean("seatLocked") != true
                } == true) { "Seat $seatIndex is occupied or locked." }
                check(seatSnaps.none { it.getString("occupantId") == userId }) { "This user is already on a speaker seat." }
                val resolvedGender = if (gender != "UNSPECIFIED") gender else userSnap?.getString("gender") ?: "UNSPECIFIED"
                val payload = mapOf(
                    "userId" to userId,
                    "hostId" to loadedRoom.hostId,
                    "username" to username,
                    "avatarUrl" to avatarUrl,
                    "vipLevel" to vipLevel,
                    "preferredSeatIndex" to seatIndex.toLong(),
                    "status" to "INVITED",
                    "createdAt" to (if (requestSnap.exists()) requestSnap.get("createdAt") ?: FieldValue.serverTimestamp() else FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (requestSnap.exists()) tx.update(requestRef, payload) else tx.set(requestRef, payload)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToSeatInvitation(roomId: String, request: SpeakerRequest, accept: Boolean): Result<Unit> {
        val path = "rooms/$roomId/speaker_requests/${request.userId}"
        return try {
            val uid = requireUserId()
            check(uid == request.userId) { "This invitation belongs to another user." }
            val roomRef = db.collection("rooms").document(roomId)
            val requestRef = roomRef.collection("speaker_requests").document(uid)
            val listenerRef = roomRef.collection("listeners").document(uid)
            val userRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val requestSnap = tx.get(requestRef)
                check(requestSnap.exists() && requestSnap.getString("status") == "INVITED") {
                    "This seat invitation is no longer active."
                }
                if (!accept) {
                    tx.update(requestRef, mapOf("status" to "REJECTED", "updatedAt" to FieldValue.serverTimestamp()))
                } else {
                    val loadedRoom = roomSnap.toObject(VoiceRoom::class.java) ?: throw IllegalStateException("Room not found.")
                    check(loadedRoom.status == "LIVE" && !loadedRoom.locked) { "This room is not accepting seat invitations." }
                    val seatIndex = requestSnap.getLong("preferredSeatIndex")?.toInt() ?: 0
                    require(seatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) { "Invalid invited seat." }
                    val seatRefs = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { roomRef.collection("seats").document(it.toString()) }
                    val seatSnaps = seatRefs.map { tx.get(it) }
                    val listenerSnap = tx.get(listenerRef)
                    val userSnap = tx.get(userRef)
                    val targetSeat = seatSnaps.firstOrNull { it.id == seatIndex.toString() }
                        ?: throw IllegalStateException("Invited seat no longer exists.")
                    check(targetSeat.getString("occupantId").isNullOrBlank() && targetSeat.getBoolean("seatLocked") != true) {
                        "Seat $seatIndex is no longer available."
                    }
                    check(seatSnaps.none { it.getString("occupantId") == uid }) { "You are already seated." }
                    val resolvedGender = userSnap.getString("gender") ?: "UNSPECIFIED"
                    tx.update(roomRef.collection("seats").document(seatIndex.toString()), mapOf(
                        "occupantId" to uid,
                        "occupantName" to requestSnap.getString("username").orEmpty(),
                        "occupantAvatarUrl" to requestSnap.getString("avatarUrl").orEmpty(),
                        "occupantVipLevel" to (requestSnap.getLong("vipLevel") ?: 0L),
                        "occupantGender" to resolvedGender,
                        "muted" to false,
                        "speaking" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                    if (listenerSnap.exists()) tx.delete(listenerRef)
                    val currentListeners = loadedRoom.listenerCount
                    val currentSpeakers = loadedRoom.activeSpeakersCount
                    tx.update(roomRef, mapOf(
                        "listenerCount" to (currentListeners - if (listenerSnap.exists()) 1L else 0L).coerceAtLeast(0L),
                        "activeSpeakersCount" to (currentSpeakers + 1L).coerceAtMost(9L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                    tx.update(requestRef, mapOf("status" to "ACCEPTED", "updatedAt" to FieldValue.serverTimestamp()))
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

    suspend fun assignUserToSeatByHost(
        room: VoiceRoom,
        seatIndex: Int,
        userId: String,
        username: String,
        avatarUrl: String,
        vipLevel: Long,
        gender: String = "UNSPECIFIED"
    ): Result<Unit> = sendSeatInvitation(
        room = room,
        seatIndex = seatIndex,
        userId = userId,
        username = username,
        avatarUrl = avatarUrl,
        vipLevel = vipLevel,
        gender = gender
    )

    suspend fun vacateSpeakerSeat(roomId: String, seatIndex: Int, user: UserProfile): Result<Unit> {
        val path = "rooms/$roomId/seats/$seatIndex"
        return try {
            val uid = requireUserId()
            require(seatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) { "Host cannot vacate Seat 0 without transferring ownership." }
            val roomRef = db.collection("rooms").document(roomId)
            val targetSeatRef = roomRef.collection("seats").document(seatIndex.toString())
            val listenerRef = roomRef.collection("listeners").document(uid)

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val seatSnaps = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }
                val listenerSnap = tx.get(listenerRef)

                val targetSnap = seatSnaps.firstOrNull { it.id == seatIndex.toString() }
                check(targetSnap != null && targetSnap.getString("occupantId") == uid) {
                    "You are no longer seated in Seat $seatIndex."
                }

                tx.update(
                    targetSeatRef,
                    mapOf(
                        "occupantId" to "",
                        "occupantName" to "",
                        "occupantAvatarUrl" to "",
                        "occupantVipLevel" to 0L,
                        "occupantGender" to "UNSPECIFIED",
                        "muted" to true,
                        "speaking" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    listenerRef,
                    mapOf(
                        "userId" to uid,
                        "username" to user.username,
                        "avatarUrl" to user.avatarUrl,
                        "vipLevel" to user.vipLevel,
                        "gender" to user.gender,
                        "role" to "LISTENER",
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )

                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java)
                val currentListeners = roomSnap.getLong("listenerCount") ?: 0L
                val nextListeners = if (listenerSnap.exists()) currentListeners else currentListeners + 1L
                val remainingSpeakers = seatSnaps.count {
                    it.id != seatIndex.toString() && !it.getString("occupantId").isNullOrBlank()
                }
                val hostCount = if (loadedRoom?.effectiveOwnerOnline != false) 1L else 0L
                tx.update(
                    roomRef,
                    mapOf(
                        "listenerCount" to nextListeners,
                        "activeSpeakersCount" to (hostCount + remainingSpeakers).coerceIn(0L, 9L),
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

    suspend fun toggleSeatMute(roomId: String, seat: VoiceSeat, muted: Boolean): Result<Unit> {
        val path = "rooms/$roomId/seats/${seat.seatIndex}"
        return try {
            requireUserId()
            db.collection("rooms").document(roomId)
                .collection("seats").document(seat.seatIndex.toString())
                .update(
                    mapOf(
                        "muted" to muted,
                        "speaking" to !muted,
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

    suspend fun toggleSeatLock(roomId: String, seatIndex: Int, locked: Boolean): Result<Unit> {
        val path = "rooms/$roomId/seats/$seatIndex"
        return try {
            requireUserId()
            require(seatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) {
                "Only regular speaker seats 1..8 can be locked."
            }
            db.collection("rooms").document(roomId)
                .collection("seats").document(seatIndex.toString())
                .update(
                    mapOf(
                        "seatLocked" to locked,
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

    suspend fun muteAllSpeakers(roomId: String): Result<Unit> {
        val path = "rooms/$roomId/seats"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("rooms").document(roomId)
            val roomSnap = roomRef.get().await()
            val loadedRoom = roomSnap.toObject(VoiceRoom::class.java)
            check(loadedRoom?.isManager(uid) == true) { "Only the Room Owner or a Room Admin can mute all speakers." }
            val batch = db.batch()
            for (idx in 1..VoiceStage.SPEAKER_SEAT_COUNT) {
                batch.update(
                    roomRef.collection("seats").document(idx.toString()),
                    mapOf(
                        "muted" to true,
                        "speaking" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun hostRemoveSpeaker(roomId: String, seatIndex: Int): Result<Unit> {
        val path = "rooms/$roomId/seats/$seatIndex"
        return try {
            val uid = requireUserId()
            require(seatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT)
            val roomRef = db.collection("rooms").document(roomId)
            val targetSeatRef = roomRef.collection("seats").document(seatIndex.toString())

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java)
                check(loadedRoom?.isManager(uid) == true) { "Only the Room Owner or a Room Admin can remove speakers." }
                val seatSnaps = (1..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }
                val targetSnap = seatSnaps.firstOrNull { it.id == seatIndex.toString() }
                if (targetSnap != null && !targetSnap.getString("occupantId").isNullOrBlank()) {
                    tx.update(
                        targetSeatRef,
                        mapOf(
                            "occupantId" to "",
                            "occupantName" to "",
                            "occupantAvatarUrl" to "",
                            "occupantVipLevel" to 0L,
                            "occupantGender" to "UNSPECIFIED",
                            "muted" to true,
                            "speaking" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val remainingSpeakers = seatSnaps.count {
                        it.id != seatIndex.toString() && !it.getString("occupantId").isNullOrBlank()
                    }
                    val hostCount = if (loadedRoom?.effectiveOwnerOnline != false) 1L else 0L
                    tx.update(
                        roomRef,
                        mapOf(
                            "activeSpeakersCount" to (hostCount + remainingSpeakers).coerceIn(0L, 9L),
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

    suspend fun requestToSpeak(
        room: VoiceRoom,
        user: UserProfile,
        preferredSeatIndex: Int = 0
    ): Result<Unit> {
        val path = "rooms/${room.id}/speaker_requests/${user.userId}"
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            require(!room.isOwner(uid)) { "Host already occupies dedicated Host Seat 0." }
            require(preferredSeatIndex in 0..VoiceStage.SPEAKER_SEAT_COUNT) {
                "Invalid seat index $preferredSeatIndex."
            }

            val roomRef = db.collection("rooms").document(room.id)
            val reqRef = roomRef.collection("speaker_requests").document(uid)

            db.runTransaction { tx ->
                val seatSnaps = (0..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }
                val reqSnap = tx.get(reqRef)

                val existingSeat = seatSnaps.firstOrNull { it.getString("occupantId") == uid }
                if (existingSeat != null) {
                    throw IllegalStateException("You are already on Seat ${existingSeat.id}.")
                }

                if (preferredSeatIndex in 1..VoiceStage.SPEAKER_SEAT_COUNT) {
                    val targetSeat = seatSnaps.firstOrNull { it.id == preferredSeatIndex.toString() }
                    val targetOccupant = targetSeat?.getString("occupantId").orEmpty()
                    if (targetOccupant.isNotBlank()) {
                        throw IllegalStateException("Seat $preferredSeatIndex is already occupied.")
                    }
                    if (targetSeat?.getBoolean("seatLocked") == true) {
                        throw IllegalStateException("Seat $preferredSeatIndex is locked by the Host.")
                    }
                }

                if (reqSnap.exists()) {
                    val currentStatus = reqSnap.getString("status")
                    if (currentStatus == "PENDING") {
                        throw IllegalStateException("Your speak request is already pending approval.")
                    }
                    if (currentStatus == "INVITED") {
                        throw IllegalStateException("You have a seat invitation. Accept or reject it first.")
                    }
                    tx.update(
                        reqRef,
                        mapOf(
                            "username" to user.username,
                            "avatarUrl" to user.avatarUrl,
                            "vipLevel" to user.vipLevel,
                            "preferredSeatIndex" to preferredSeatIndex.toLong(),
                            "status" to "PENDING",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        reqRef,
                        mapOf(
                            "userId" to uid,
                            "hostId" to room.hostId,
                            "username" to user.username,
                            "avatarUrl" to user.avatarUrl,
                            "vipLevel" to user.vipLevel,
                            "preferredSeatIndex" to preferredSeatIndex.toLong(),
                            "status" to "PENDING",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelSpeakerRequest(roomId: String, userId: String): Result<Unit> {
        val path = "rooms/$roomId/speaker_requests/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("rooms").document(roomId)
                .collection("speaker_requests").document(uid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveSpeakerRequest(
        room: VoiceRoom,
        request: SpeakerRequest,
        approve: Boolean
    ): Result<Unit> {
        val path = "rooms/${room.id}/speaker_requests/${request.id}"
        return try {
            val uid = requireUserId()
            check(room.isManager(uid)) { "Only the Room Owner or a Room Admin can resolve speaker requests." }
            val roomRef = db.collection("rooms").document(room.id)
            val reqRef = roomRef.collection("speaker_requests").document(request.id)

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val reqSnap = tx.get(reqRef)
                val seatSnaps = if (approve) {
                    (1..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                        tx.get(roomRef.collection("seats").document(idx.toString()))
                    }
                } else {
                    emptyList()
                }

                check(reqSnap.exists() && reqSnap.getString("status") == "PENDING") {
                    "This speaker request is no longer pending."
                }

                if (approve) {
                    val loadedRoom = roomSnap.toObject(VoiceRoom::class.java)
                        ?: throw IllegalStateException("Room not found.")
                    check(loadedRoom.isManager(uid)) {
                        "Only the Room Owner or a Room Admin can resolve speaker requests."
                    }
                    check(loadedRoom.status == "LIVE" && !loadedRoom.locked) {
                        "This room is not accepting seat invitations."
                    }
                    check(seatSnaps.none { it.getString("occupantId") == request.userId }) {
                        "${request.username} is already on a speaker seat."
                    }
                    val seats = seatSnaps.mapNotNull { it.toObject(VoiceSeat::class.java) }
                    val stage = VoiceStage.buildStrictNineSeatStage(loadedRoom, seats)
                    val openSeat = VoiceStage.findAvailableSpeakerSeat(
                        stage = stage,
                        preferredSeatIndex = request.preferredSeatIndex.toInt()
                    ) ?: throw IllegalStateException("All 8 speaker seats are currently occupied or locked.")

                    tx.update(
                        reqRef,
                        mapOf(
                            "status" to "INVITED",
                            "preferredSeatIndex" to openSeat.seatIndex,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.update(
                        reqRef,
                        mapOf(
                            "status" to "REJECTED",
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

    suspend fun updateRoomSettings(
        roomId: String,
        title: String,
        announcement: String,
        category: String? = null,
        coverKey: String,
        keepCustomCover: Boolean,
        locked: Boolean,
        newRawPassword: String?
    ): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(60)
            require(cleanTitle.length >= 2) { "Room title must be at least 2 characters." }
            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "announcement" to announcement.trim().take(200),
                "locked" to locked,
                "coverKey" to coverKey,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!category.isNullOrBlank()) {
                updates["category"] = category
            }
            if (!keepCustomCover) {
                updates["coverUrl"] = ""
            }
            if (newRawPassword != null) {
                updates["hasPassword"] = newRawPassword.isNotBlank()
                updates["passcodeHash"] = hashPasscode(newRawPassword)
            }
            db.collection("rooms").document(roomId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomCoverUrl(roomId: String, coverUrl: String): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            require(coverUrl.isNotBlank() && coverUrl.length <= 1000) { "Invalid room cover URL." }
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "coverUrl" to coverUrl,
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

    suspend fun transferOwnershipSafely(
        room: VoiceRoom,
        targetSeat: VoiceSeat
    ): Result<Unit> {
        val path = "rooms/${room.id}"
        return try {
            val uid = requireUserId()
            check(room.isOwner(uid)) { "Only the current host can transfer ownership." }
            require(!targetSeat.isHostSeat && targetSeat.isOccupied && targetSeat.occupantId != uid) {
                "Select an occupied regular speaker seat to transfer host ownership."
            }

            val roomRef = db.collection("rooms").document(room.id)
            val batch = db.batch()
            // 1. Update room hostId & ownerId
            batch.update(
                roomRef,
                mapOf(
                    "hostId" to targetSeat.occupantId,
                    "hostName" to targetSeat.occupantName,
                    "hostAvatarUrl" to targetSeat.occupantAvatarUrl,
                    "ownerId" to targetSeat.occupantId,
                    "ownerName" to targetSeat.occupantName,
                    "ownerOnline" to true,
                    "adminIds" to room.adminIds.filter { it != targetSeat.occupantId },
                    "activeAdminIds" to room.activeAdminIds.filter { it != targetSeat.occupantId },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // 2. Move new host into Seat 0
            batch.update(
                roomRef.collection("seats").document("0"),
                mapOf(
                    "occupantId" to targetSeat.occupantId,
                    "occupantName" to targetSeat.occupantName,
                    "occupantAvatarUrl" to targetSeat.occupantAvatarUrl,
                    "occupantVipLevel" to targetSeat.occupantVipLevel,
                    "occupantGender" to targetSeat.occupantGender,
                    "muted" to false,
                    "speaking" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // 3. Vacate the speaker's previous seat so user isn't duplicated
            batch.update(
                roomRef.collection("seats").document(targetSeat.seatIndex.toString()),
                mapOf(
                    "occupantId" to "",
                    "occupantName" to "",
                    "occupantAvatarUrl" to "",
                    "occupantVipLevel" to 0L,
                    "occupantGender" to "UNSPECIFIED",
                    "muted" to true,
                    "speaking" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // 4. Post system notice
            batch.set(
                roomRef.collection("messages").document(),
                mapOf(
                    "senderId" to uid,
                    "senderName" to room.effectiveOwnerName,
                    "senderVipLevel" to 0L,
                    "senderGender" to "UNSPECIFIED",
                    "text" to "Host ownership transferred to ${targetSeat.occupantName}.",
                    "type" to "SYSTEM",
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

    suspend fun endRoom(roomId: String): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("rooms").document(roomId)
            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val loadedRoom = roomSnap.toObject(VoiceRoom::class.java)
                    ?: throw IllegalStateException("Room not found.")
                check(loadedRoom.isOwner(uid) || (loadedRoom.isRoomAdmin(uid) && !loadedRoom.effectiveOwnerOnline)) {
                    "Only the Room Owner (or a Room Admin when the Owner is away) can close this room."
                }
                val seatSnaps = (0..VoiceStage.SPEAKER_SEAT_COUNT).map { idx ->
                    tx.get(roomRef.collection("seats").document(idx.toString()))
                }
                seatSnaps.forEach { snap ->
                    if (snap.id == "0") {
                        tx.update(
                            snap.reference,
                            mapOf(
                                "occupantAvatarUrl" to "",
                                "muted" to true,
                                "speaking" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    } else if (!snap.getString("occupantId").isNullOrBlank()) {
                        tx.update(
                            snap.reference,
                            mapOf(
                                "occupantId" to "",
                                "occupantName" to "",
                                "occupantAvatarUrl" to "",
                                "occupantVipLevel" to 0L,
                                "occupantGender" to "UNSPECIFIED",
                                "muted" to true,
                                "speaking" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                val updates = mutableMapOf<String, Any>(
                    // Ending a live session must not end the permanent room itself.
                    "status" to "LIVE",
                    "listenerCount" to 0L,
                    "activeSpeakersCount" to 0L,
                    "activeAdminIds" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (loadedRoom.isOwner(uid)) {
                    updates["ownerOnline"] = false
                }
                tx.update(roomRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendRoomMessage(roomId: String, sender: UserProfile, text: String): Result<Unit> {
        val path = "rooms/$roomId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(400)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }
            db.collection("rooms").document(roomId).collection("messages").document().set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to sender.username,
                    "senderAvatarUrl" to sender.avatarUrl,
                    "senderVipLevel" to sender.vipLevel,
                    "senderGender" to sender.gender,
                    "text" to cleanText,
                    "type" to "TEXT",
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

    suspend fun sendVirtualGift(
        roomId: String,
        sender: UserProfile,
        recipientId: String,
        recipientName: String,
        gift: GiftCatalogItem,
        quantity: Long = 1L
    ): Result<Unit> {
        val path = "gift_transactions"
        return try {
            val uid = requireUserId()
            check(uid == sender.userId)
            require(recipientId.isNotBlank()) { "Please select a recipient on stage." }
            val safeQty = quantity.coerceIn(1L, 999L)
            val totalCost = gift.coinCost * safeQty

            val senderRef = db.collection("users").document(uid)
            val recipientRef = db.collection("users").document(recipientId)

            db.runTransaction { tx ->
                val senderSnap = tx.get(senderRef)
                val recipientSnap = if (recipientId != uid) tx.get(recipientRef) else null

                val liveCoins = senderSnap.getLong("coinBalance") ?: sender.coinBalance
                val liveSent = senderSnap.getLong("totalGiftsSent") ?: sender.totalGiftsSent
                val liveSenderXp = senderSnap.getLong("xp") ?: sender.xp
                require(liveCoins >= totalCost) { "Not enough coins for ${safeQty}x ${gift.name} ($totalCost needed)." }

                val newSenderXp = liveSenderXp + (totalCost / 5L)
                tx.update(
                    senderRef,
                    mapOf(
                        "coinBalance" to (liveCoins - totalCost),
                        "totalGiftsSent" to (liveSent + safeQty),
                        "totalGiftCoinsSent" to ((senderSnap.getLong("totalGiftCoinsSent") ?: 0L) + totalCost),
                        "xp" to newSenderXp,
                        "level" to LevelProgression.levelForXp(newSenderXp),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (recipientSnap != null && recipientSnap.exists()) {
                    val recReceived = recipientSnap.getLong("totalGiftsReceived") ?: 0L
                    val recXp = recipientSnap.getLong("xp") ?: 0L
                    val updatedRecXp = recXp + (totalCost / 5L)
                    tx.update(
                        recipientRef,
                        mapOf(
                            "totalGiftsReceived" to (recReceived + safeQty),
                            "totalGiftCoinsReceived" to ((recipientSnap.getLong("totalGiftCoinsReceived") ?: 0L) + totalCost),
                            "xp" to updatedRecXp,
                            "level" to LevelProgression.levelForXp(updatedRecXp),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    db.collection("gift_transactions").document(),
                    mapOf(
                        "senderId" to uid,
                        "senderName" to sender.username,
                        "recipientId" to recipientId,
                        "recipientName" to recipientName,
                        "roomId" to roomId,
                        "giftId" to gift.id,
                        "giftName" to gift.name,
                        "coinCost" to totalCost,
                        "quantity" to safeQty,
                        "participantIds" to listOf(uid, recipientId).distinct().let {
                            if (it.size == 1) listOf(uid, uid) else it
                        },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                val qtyLabel = if (safeQty > 1L) "${safeQty}x " else ""
                tx.set(
                    db.collection("rooms").document(roomId).collection("messages").document(),
                    mapOf(
                        "senderId" to uid,
                        "senderName" to sender.username,
                        "senderAvatarUrl" to sender.avatarUrl,
                        "senderVipLevel" to sender.vipLevel,
                        "senderGender" to sender.gender,
                        "text" to "sent $qtyLabel${gift.emoji} ${gift.name} to $recipientName!",
                        "type" to "GIFT",
                        "giftId" to gift.id,
                        "giftName" to "$qtyLabel${gift.name}",
                        "giftCoins" to totalCost,
                        "recipientName" to recipientName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                if (recipientId != uid) {
                    tx.set(
                        db.collection("notifications").document(),
                        mapOf(
                            "recipientId" to recipientId,
                            "senderId" to uid,
                            "senderName" to sender.username,
                            "type" to "GIFT",
                            "title" to "Gift Received ${gift.emoji}",
                            "body" to "${sender.username} sent you $qtyLabel${gift.name} ($totalCost coins).",
                            "targetId" to roomId,
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

    companion object {
        const val ONE_ROOM_PER_USER_MESSAGE =
            "You have already used your one-time room creation privilege. Each account can create only one room permanently."
    }
}
