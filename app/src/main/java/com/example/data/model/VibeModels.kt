package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class RoomCategory(val label: String) {
    ALL("All"),
    CHILL("Chill"),
    MUSIC("Music"),
    GAMING("Gaming"),
    DATING("Dating"),
    KARAOKE("Karaoke"),
    CHAT("Chat");

    companion object {
        val creatableCategories = entries.filter { it != ALL }
        fun fromString(raw: String): RoomCategory =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
                ?: CHILL
    }
}

enum class RoomCoverPreset(val key: String, val label: String) {
    PARTY_STAGE("party_stage", "Neon Aurora Stage"),
    CHILL_LOUNGE("chill_lounge", "Lo-Fi Sunset Lounge"),
    MUSIC_ARENA("music_arena", "Concert Equalizer"),
    VIP_CROWN("vip_crown", "Royal VIP Lounge");

    companion object {
        fun fromKey(key: String): RoomCoverPreset =
            entries.firstOrNull { it.key == key } ?: PARTY_STAGE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vibeId: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val gender: String = "UNSPECIFIED",
    val age: Long = 0L,
    val showAge: Boolean = false,
    val showGender: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val online: Boolean = true,
    val level: Long = 1L,
    val xp: Long = 0L,
    val vipLevel: Long = 0L,
    val coinBalance: Long = 500L,
    val totalGiftsReceived: Long = 0L,
    val totalGiftsSent: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val accountStatus: String = "ACTIVE",
    val role: String = "USER",
    val lastDailyClaimDay: String = "",
    val hasCreatedRoom: Boolean = false,
    val createdRoomId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val formattedAge: String?
        get() = if (age > 0L) "$age years" else null
}

@Keep
@IgnoreExtraProperties
data class RoomCreationLock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomId: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val blockedUserIds: List<String> = emptyList(),
    val pinnedConversationIds: List<String> = emptyList(),
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowEdge(
    @DocumentId val id: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val peerAvatarUrl: String = "",
    val peerGender: String = "UNSPECIFIED",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VoiceRoom(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatarUrl: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerOnline: Boolean = true,
    val adminIds: List<String> = emptyList(),
    val activeAdminIds: List<String> = emptyList(),
    val title: String = "",
    val announcement: String = "",
    val category: String = "Chill",
    val coverKey: String = "party_stage",
    val tags: List<String> = emptyList(),
    val listenerCount: Long = 0L,
    val activeSpeakersCount: Long = 1L,
    val locked: Boolean = false,
    val hasPassword: Boolean = false,
    val passcodeHash: String = "",
    val status: String = "LIVE",
    val featured: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val effectiveOwnerId: String get() = ownerId.ifBlank { hostId }
    val effectiveOwnerName: String get() = ownerName.ifBlank { hostName }
    val effectiveOwnerOnline: Boolean get() = ownerOnline
    fun isOwner(uid: String): Boolean = uid.isNotBlank() && (effectiveOwnerId == uid || hostId == uid)
    fun isRoomAdmin(uid: String): Boolean = uid.isNotBlank() && uid in adminIds
    fun isManager(uid: String): Boolean = isOwner(uid) || isRoomAdmin(uid)
}

@Keep
@IgnoreExtraProperties
data class VoiceSeat(
    @DocumentId val id: String = "",
    val seatIndex: Long = 0L,
    val role: String = "SPEAKER",
    val occupantId: String = "",
    val occupantName: String = "",
    val occupantAvatarUrl: String = "",
    val occupantVipLevel: Long = 0L,
    val occupantGender: String = "UNSPECIFIED",
    val muted: Boolean = true,
    val seatLocked: Boolean = false,
    val speaking: Boolean = false,
    val updatedAt: Timestamp? = null
) {
    val isHostSeat: Boolean get() = seatIndex == 0L || role == "HOST"
    val isOccupied: Boolean get() = occupantId.isNotBlank()
}

/**
 * Enforces the core VibePlay invariant:
 * Every voice room stage ALWAYS contains EXACTLY 9 seats:
 * - 1 dedicated Host/Owner seat (index 0)
 * - 8 regular speaker seats (indices 1..8)
 * Listeners never occupy speaker seats.
 */
data class VoiceStage(
    val hostSeat: VoiceSeat,
    val speakerSeats: List<VoiceSeat>
) {
    init {
        require(hostSeat.seatIndex == 0L && hostSeat.role == "HOST") {
            "Host seat must have index 0 and role HOST."
        }
        require(speakerSeats.size == SPEAKER_SEAT_COUNT) {
            "Voice room must always have exactly $SPEAKER_SEAT_COUNT regular speaker seats."
        }
    }

    val totalSeats: Int get() = 1 + speakerSeats.size

    companion object {
        const val HOST_SEAT_INDEX = 0
        const val SPEAKER_SEAT_COUNT = 8
        const val TOTAL_SEAT_COUNT = 9

        fun buildStrictNineSeatStage(
            room: VoiceRoom,
            rawSeats: List<VoiceSeat>
        ): VoiceStage {
            val byIndex = rawSeats.associateBy { it.seatIndex.toInt() }
            val rawHost = byIndex[0]
            val ownerId = room.effectiveOwnerId
            val ownerName = room.effectiveOwnerName
            val ownerOnline = room.effectiveOwnerOnline
            val hostSeat = VoiceSeat(
                id = "0",
                seatIndex = 0L,
                role = "HOST",
                occupantId = ownerId,
                occupantName = ownerName,
                occupantAvatarUrl = if (ownerOnline) {
                    rawHost?.occupantAvatarUrl?.ifBlank { room.hostAvatarUrl } ?: room.hostAvatarUrl
                } else {
                    ""
                },
                occupantVipLevel = rawHost?.occupantVipLevel ?: 0L,
                occupantGender = rawHost?.occupantGender ?: "UNSPECIFIED",
                muted = if (!ownerOnline) true else (rawHost?.muted ?: false),
                seatLocked = false,
                speaking = if (!ownerOnline) false else (rawHost?.speaking ?: false),
                updatedAt = rawHost?.updatedAt
            )
            val speakers = (1..SPEAKER_SEAT_COUNT).map { idx ->
                val found = byIndex[idx]
                val validOccupant = found != null &&
                    found.occupantId.isNotBlank() &&
                    found.occupantId != ownerId &&
                    found.occupantId != room.hostId
                VoiceSeat(
                    id = idx.toString(),
                    seatIndex = idx.toLong(),
                    role = "SPEAKER",
                    occupantId = if (validOccupant) found!!.occupantId else "",
                    occupantName = if (validOccupant) found!!.occupantName else "",
                    occupantAvatarUrl = if (validOccupant) found!!.occupantAvatarUrl else "",
                    occupantVipLevel = if (validOccupant) found!!.occupantVipLevel else 0L,
                    occupantGender = if (validOccupant) found!!.occupantGender else "UNSPECIFIED",
                    muted = found?.muted ?: true,
                    seatLocked = found?.seatLocked ?: false,
                    speaking = found?.speaking ?: false,
                    updatedAt = found?.updatedAt
                )
            }
            return VoiceStage(hostSeat = hostSeat, speakerSeats = speakers)
        }

        fun buildOnlineMembersList(
            room: VoiceRoom,
            stage: VoiceStage,
            listeners: List<RoomListener>
        ): List<RoomOnlineMember> {
            val result = mutableListOf<RoomOnlineMember>()
            val seenIds = mutableSetOf<String>()

            if (room.effectiveOwnerOnline && stage.hostSeat.occupantId.isNotBlank()) {
                seenIds.add(stage.hostSeat.occupantId)
                result.add(
                    RoomOnlineMember(
                        userId = stage.hostSeat.occupantId,
                        username = stage.hostSeat.occupantName,
                        avatarUrl = stage.hostSeat.occupantAvatarUrl,
                        vipLevel = stage.hostSeat.occupantVipLevel,
                        gender = stage.hostSeat.occupantGender,
                        stageRole = "HOST",
                        seatIndex = 0,
                        isRoomAdmin = true
                    )
                )
            }

            stage.speakerSeats.forEach { seat ->
                if (seat.isOccupied && seenIds.add(seat.occupantId)) {
                    result.add(
                        RoomOnlineMember(
                            userId = seat.occupantId,
                            username = seat.occupantName,
                            avatarUrl = seat.occupantAvatarUrl,
                            vipLevel = seat.occupantVipLevel,
                            gender = seat.occupantGender,
                            stageRole = "SPEAKER",
                            seatIndex = seat.seatIndex.toInt(),
                            isRoomAdmin = room.isRoomAdmin(seat.occupantId)
                        )
                    )
                }
            }

            val ownerId = room.effectiveOwnerId
            listeners.forEach { listener ->
                if (listener.userId.isNotBlank() &&
                    (room.effectiveOwnerOnline || listener.userId != ownerId) &&
                    seenIds.add(listener.userId)
                ) {
                    result.add(
                        RoomOnlineMember(
                            userId = listener.userId,
                            username = listener.username,
                            avatarUrl = listener.avatarUrl,
                            vipLevel = listener.vipLevel,
                            gender = listener.gender,
                            stageRole = "LISTENER",
                            seatIndex = null,
                            isRoomAdmin = room.isRoomAdmin(listener.userId)
                        )
                    )
                }
            }

            return result
        }

        fun canAssignUserToSeat(
            room: VoiceRoom,
            stage: VoiceStage,
            targetSeatIndex: Int,
            candidateUserId: String
        ): Result<VoiceSeat> {
            if (targetSeatIndex !in 1..SPEAKER_SEAT_COUNT) {
                return Result.failure(
                    IllegalArgumentException("Speakers can only be assigned to seats 1..$SPEAKER_SEAT_COUNT.")
                )
            }
            if (candidateUserId.isBlank()) {
                return Result.failure(IllegalArgumentException("Invalid user ID."))
            }
            if (candidateUserId == room.effectiveOwnerId || candidateUserId == room.hostId) {
                return Result.failure(IllegalStateException("Host already occupies dedicated Host Seat 0."))
            }
            val alreadySeated = (listOf(stage.hostSeat) + stage.speakerSeats)
                .firstOrNull { it.occupantId == candidateUserId }
            if (alreadySeated != null) {
                return Result.failure(
                    IllegalStateException("User already occupies Seat ${alreadySeated.seatIndex}.")
                )
            }
            val seat = stage.speakerSeats.firstOrNull { it.seatIndex.toInt() == targetSeatIndex }
                ?: return Result.failure(IllegalArgumentException("Invalid seat index $targetSeatIndex."))
            if (seat.isOccupied) {
                return Result.failure(
                    IllegalStateException("Seat $targetSeatIndex is already occupied by ${seat.occupantName}.")
                )
            }
            if (seat.seatLocked) {
                return Result.failure(IllegalStateException("Seat $targetSeatIndex is locked."))
            }
            return Result.success(seat)
        }

        fun findAvailableSpeakerSeat(
            stage: VoiceStage,
            preferredSeatIndex: Int = 0
        ): VoiceSeat? {
            if (preferredSeatIndex in 1..SPEAKER_SEAT_COUNT) {
                val preferred = stage.speakerSeats.firstOrNull {
                    it.seatIndex.toInt() == preferredSeatIndex && !it.isOccupied && !it.seatLocked
                }
                if (preferred != null) return preferred
            }
            return stage.speakerSeats.firstOrNull { !it.isOccupied && !it.seatLocked }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class SpeakerRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hostId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val vipLevel: Long = 0L,
    val preferredSeatIndex: Long = 0L,
    val status: String = "PENDING",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderVipLevel: Long = 0L,
    val senderGender: String = "UNSPECIFIED",
    val text: String = "",
    val type: String = "TEXT",
    val giftId: String = "",
    val giftName: String = "",
    val giftCoins: Long = 0L,
    val recipientName: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomListener(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val vipLevel: Long = 0L,
    val gender: String = "UNSPECIFIED",
    val role: String = "LISTENER",
    val joinedAt: Timestamp? = null
)

data class RoomOnlineMember(
    val userId: String,
    val username: String,
    val avatarUrl: String,
    val vipLevel: Long = 0L,
    val gender: String = "UNSPECIFIED",
    val stageRole: String = "LISTENER",
    val seatIndex: Int? = null,
    val isRoomAdmin: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val conversationType: String = "DIRECT",
    val groupName: String = "",
    val groupAvatarUrl: String = "",
    val ownerId: String = "",
    val adminIds: List<String> = emptyList(),
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val unreadBy: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isGroupChat: Boolean get() = conversationType == "GROUP"
}

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val senderGender: String = "UNSPECIFIED",
    val type: String = "TEXT",
    val text: String = "",
    val imageUri: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GiftTransaction(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val roomId: String = "",
    val giftId: String = "",
    val giftName: String = "",
    val coinCost: Long = 0L,
    val quantity: Long = 1L,
    val participantIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "SYSTEM",
    val title: String = "",
    val body: String = "",
    val targetId: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetType: String = "USER",
    val targetId: String = "",
    val targetName: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "OPEN",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GiftCatalogItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Popular",
    val coinCost: Long = 10L,
    val emoji: String = "\uD83C\uDF81",
    val accentColorHex: String = "#EC4899",
    val active: Boolean = true,
    val updatedAt: Timestamp? = null
)

data class GiftBurstEvent(
    val id: String,
    val senderName: String,
    val recipientName: String,
    val giftName: String,
    val giftEmoji: String,
    val coinCost: Long
)

data class VipTier(
    val level: Int,
    val title: String,
    val coinPrice: Long,
    val badgeColorHex: Long,
    val entranceEffect: String,
    val perks: List<String>
)

object VibeCatalogDefaults {
    val defaultGifts = listOf(
        GiftCatalogItem(id = "gift_rose", name = "Neon Rose", category = "Romantic", coinCost = 10L, emoji = "\uD83C\uDF39", accentColorHex = "#EC4899"),
        GiftCatalogItem(id = "gift_mic", name = "Golden Mic", category = "Popular", coinCost = 25L, emoji = "\uD83C\uDFA4", accentColorHex = "#F59E0B"),
        GiftCatalogItem(id = "gift_fire", name = "Vibe Fire", category = "Popular", coinCost = 50L, emoji = "\uD83D\uDD25", accentColorHex = "#F43F5E"),
        GiftCatalogItem(id = "gift_party", name = "Party Confetti", category = "Party", coinCost = 100L, emoji = "\uD83C\uDF89", accentColorHex = "#06B6D4"),
        GiftCatalogItem(id = "gift_heart", name = "Crystal Heart", category = "Romantic", coinCost = 180L, emoji = "\uD83D\uDC96", accentColorHex = "#EC4899"),
        GiftCatalogItem(id = "gift_crown", name = "Royal Crown", category = "Luxury", coinCost = 500L, emoji = "\uD83D\uDC51", accentColorHex = "#F59E0B"),
        GiftCatalogItem(id = "gift_rocket", name = "Aurora Rocket", category = "Luxury", coinCost = 999L, emoji = "\uD83D\uDE80", accentColorHex = "#7C3AED"),
        GiftCatalogItem(id = "gift_diamond", name = "Starlight Gem", category = "Luxury", coinCost = 1500L, emoji = "\uD83D\uDC8E", accentColorHex = "#22D3EE")
    )

    val vipTiers = listOf(
        VipTier(
            level = 1,
            title = "VIP 1 · Starlight",
            coinPrice = 200L,
            badgeColorHex = 0xFF06B6D4,
            entranceEffect = "Cyan Ripple Entrance",
            perks = listOf("Cyan VIP 1 Badge", "1.2x Room XP Boost", "Custom Cyan Seat Ring")
        ),
        VipTier(
            level = 2,
            title = "VIP 2 · Neon Pulse",
            coinPrice = 450L,
            badgeColorHex = 0xFF7C3AED,
            entranceEffect = "Aurora Wave Banner",
            perks = listOf("Violet VIP 2 Badge", "Animated Profile Frame", "Priority Speaker Queue")
        ),
        VipTier(
            level = 3,
            title = "VIP 3 · Coral Crown",
            coinPrice = 900L,
            badgeColorHex = 0xFFEC4899,
            entranceEffect = "Pink Spotlight Arrival",
            perks = listOf("Coral VIP 3 Badge", "Exclusive Lounge Decorations", "1.5x XP Boost")
        ),
        VipTier(
            level = 4,
            title = "VIP 4 · Royal Gold",
            coinPrice = 1800L,
            badgeColorHex = 0xFFF59E0B,
            entranceEffect = "Golden Crown Shower",
            perks = listOf("Gold VIP 4 Crown", "2x Daily Login Coins", "Royal Stage Aura")
        ),
        VipTier(
            level = 5,
            title = "VIP 5 · Apex Legend",
            coinPrice = 3500L,
            badgeColorHex = 0xFFFFD700,
            entranceEffect = "Full-Screen Galaxy Entrance",
            perks = listOf("Apex Legend Crest", "Iridescent Avatar Halo", "VIP Support & All Effects")
        )
    )
}
