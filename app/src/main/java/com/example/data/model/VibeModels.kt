package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Charms are earned from received room gifts:
 * 10 gold coins of gift value = 1 charm.
 */
object CharmProgression {
    const val COINS_PER_CHARM = 10L
    val milestones = listOf(
        1_000L, 4_000L, 12_000L, 30_000L, 80_000L, 160_000L,
        300_000L, 500_000L, 1_000_000L, 2_000_000L, 3_500_000L,
        6_000_000L, 8_500_000L, 12_000_000L, 16_000_000L,
        26_000_000L, 48_000_000L, 86_000_000L, 120_000_000L,
        240_000_000L, 360_000_000L
    )
    val levelNames = listOf(
        "Star 1", "Star 2", "Star 3",
        "Diamond 1", "Diamond 2", "Diamond 3",
        "Crown 1", "Crown 2", "Crown 3",
        "Royal 1", "Royal 2", "Royal 3",
        "Legend 1", "Legend 2", "Legend 3",
        "Mythic 1", "Mythic 2", "Mythic 3",
        "Divine 1", "Divine 2", "Divine 3"
    )

    fun nameForLevel(level: Long): String =
        if (level in 1L..levelNames.size.toLong()) levelNames[level.toInt() - 1] else "Unranked"

    fun fromReceivedGiftCoins(coins: Long): Long = coins.coerceAtLeast(0L) / COINS_PER_CHARM
    fun levelFor(charms: Long): Long = milestones.indexOfLast { charms >= it }.let { if (it < 0) 0L else it + 1L }
    fun nextMilestone(charms: Long): Long? = milestones.firstOrNull { charms < it }
    fun previousMilestone(charms: Long): Long = milestones.lastOrNull { charms >= it } ?: 0L
}

/**
 * Room progression is independent from UserProfile.xp / UserProfile.level.
 * Room gift value converts at 10 coins = 1 room EXP; each level requires
 * another 100,000 EXP, with progression capped at level 20.
 */
object RoomLevelProgression {
    const val COINS_PER_EXP = 10L
    const val EXP_PER_LEVEL = 100_000L
    const val MAX_LEVEL = 20L

    fun expFromGiftCoins(coins: Long): Long = coins.coerceAtLeast(0L) / COINS_PER_EXP
    fun levelForExp(exp: Long): Long =
        (exp.coerceAtLeast(0L) / EXP_PER_LEVEL + 1L).coerceAtMost(MAX_LEVEL)

    fun expIntoLevel(exp: Long): Long =
        if (levelForExp(exp) == MAX_LEVEL) exp.coerceAtLeast(0L) - (MAX_LEVEL - 1L) * EXP_PER_LEVEL
        else exp.coerceAtLeast(0L) % EXP_PER_LEVEL

    fun expForNextLevel(exp: Long): Long? =
        if (levelForExp(exp) >= MAX_LEVEL) null else EXP_PER_LEVEL - expIntoLevel(exp)

    fun totalExpForNextLevel(exp: Long): Long? =
        if (levelForExp(exp) >= MAX_LEVEL) null else levelForExp(exp) * EXP_PER_LEVEL
}

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
    val genderChangeCount: Long = 0L,
    val age: Long = 0L,
    val showAge: Boolean = false,
    val showGender: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val online: Boolean = true,
    val lastSeenAt: Timestamp? = null,
    val level: Long = 1L,
    val xp: Long = 0L,
    val vipLevel: Long = 0L,
    val vipExpiresAt: Timestamp? = null,
    val ownedTitleIds: List<String> = emptyList(),
    val activeTitleId: String = "",
    val activeTitleIds: List<String> = emptyList(),
    val titleExpiresAt: Map<String, Timestamp> = emptyMap(),
    val ownedFrameIds: List<String> = emptyList(),
    val activeFrameId: String = "",
    val frameExpiresAt: Map<String, Timestamp> = emptyMap(),
    val coinBalance: Long = 500L,
    val totalGiftsReceived: Long = 0L,
    val totalGiftsSent: Long = 0L,
    val totalGiftCoinsReceived: Long = 0L,
    val totalGiftCoinsSent: Long = 0L,
    val dailyGiftCoinsReceived: Long = 0L,
    val dailyGiftCoinsSent: Long = 0L,
    val dailyGiftCoinsDate: String = "",
    val weeklyGiftCoinsReceived: Long = 0L,
    val weeklyGiftCoinsSent: Long = 0L,
    val weeklyGiftCoinsWeek: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val accountStatus: String = "ACTIVE",
    val role: String = "USER",
    val lastDailyClaimDay: String = "",
    val lastDailyClaimAt: Timestamp? = null,
    val hasCreatedRoom: Boolean = false,
    val createdRoomId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isVipActive: Boolean
        get() = vipLevel > 0L && (vipExpiresAt?.toDate()?.after(java.util.Date()) == true)

    fun isTitleActive(itemId: String): Boolean =
        itemId.isNotBlank() && itemId in ownedTitleIds &&
            titleExpiresAt[itemId]?.toDate()?.after(java.util.Date()) == true

    val equippedTitleIds: List<String>
        get() = (activeTitleIds.ifEmpty { listOf(activeTitleId).filter { it.isNotBlank() } })
            .distinct().filter { isTitleActive(it) }.take(4)

    fun isFrameActive(itemId: String): Boolean =
        itemId.isNotBlank() && itemId in ownedFrameIds &&
            frameExpiresAt[itemId]?.toDate()?.after(java.util.Date()) == true

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
    val publicRoomId: String = "",
    val creatorId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatarUrl: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerOnline: Boolean = true,
    val adminIds: List<String> = emptyList(),
    val activeAdminIds: List<String> = emptyList(),
    val bannedUserIds: List<String> = emptyList(),
    val title: String = "",
    val announcement: String = "",
    val category: String = "Chill",
    val coverKey: String = "party_stage",
    val coverUrl: String = "",
    val roomBackgroundKey: String = "aurora_luxe",
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
    val occupantFrameColorArgb: Long? = null,
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
                // Keep the owner's avatar available for the faded OFFLINE host tile.
                occupantAvatarUrl = rawHost?.occupantAvatarUrl?.ifBlank { room.hostAvatarUrl } ?: room.hostAvatarUrl,
                occupantVipLevel = rawHost?.occupantVipLevel ?: 0L,
                occupantFrameColorArgb = rawHost?.occupantFrameColorArgb,
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
                    occupantFrameColorArgb = if (validOccupant) found!!.occupantFrameColorArgb else null,
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
                        isRoomAdmin = true,
                        frameColorArgb = stage.hostSeat.occupantFrameColorArgb
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
                            isRoomAdmin = room.isRoomAdmin(seat.occupantId),
                            frameColorArgb = seat.occupantFrameColorArgb
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
                            isRoomAdmin = room.isRoomAdmin(listener.userId),
                            frameColorArgb = listener.frameColorArgb
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
    val senderAvatarUrl: String = "",
    val senderVipLevel: Long = 0L,
    val senderGender: String = "UNSPECIFIED",
    val text: String = "",
    val type: String = "TEXT",
    val giftId: String = "",
    val giftName: String = "",
    val giftCoins: Long = 0L,
    val recipientName: String = "",
    val createdAt: Timestamp? = null,
    val giftQuantity: Long = 1L
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
    val joinedAt: Timestamp? = null,
    val frameColorArgb: Long? = null
)

data class RoomOnlineMember(
    val userId: String,
    val username: String,
    val avatarUrl: String,
    val vipLevel: Long = 0L,
    val gender: String = "UNSPECIFIED",
    val stageRole: String = "LISTENER",
    val seatIndex: Int? = null,
    val isRoomAdmin: Boolean = false,
    val frameColorArgb: Long? = null
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
    val imageData: String = "",
    val giftId: String = "",
    val giftName: String = "",
    val giftEmoji: String = "",
    val giftCoins: Long = 0L,
    val giftQuantity: Long = 1L,
    val recipientId: String = "",
    val recipientName: String = "",
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
data class GlobalGiftAnnouncement(
    @DocumentId val id: String = "",
    val giftTransactionId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val roomId: String = "",
    val giftName: String = "",
    val totalCoins: Long = 0L,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GiftCatalogItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Popular",
    val coinCost: Long = 10L,
    val emoji: String = "",
    val accentColorHex: String = "#EC4899",
    val active: Boolean = true,
    val updatedAt: Timestamp? = null
)

data class GiftBurstEvent(
    val id: String,
    val senderName: String,
    val recipientName: String,
    val giftName: String,
    val coinCost: Long,
    val giftId: String = "",
    val quantity: Long = 1L
)

object LevelProgression {
    // XP needed for each next level: 500, 1200, 1900, 2600, ...
    fun xpForNextLevel(level: Long): Long = 500L + ((level - 1L).coerceAtLeast(0L) * 700L)

    fun totalXpAtLevel(level: Long): Long {
        val completedLevels = (level - 1L).coerceAtLeast(0L)
        return completedLevels * 500L + 350L * completedLevels * (completedLevels - 1L)
    }

    fun levelForXp(xp: Long): Long {
        var level = 1L
        var remaining = xp.coerceAtLeast(0L)
        while (remaining >= xpForNextLevel(level)) {
            remaining -= xpForNextLevel(level)
            level++
        }
        return level
    }

    fun progressInLevel(xp: Long): Pair<Long, Long> {
        val level = levelForXp(xp)
        val start = totalXpAtLevel(level)
        return (xp - start).coerceAtLeast(0L) to xpForNextLevel(level)
    }
}

data class VipTier(
    val level: Int,
    val title: String,
    val coinPrice: Long,
    val badgeColorHex: Long,
    val entranceEffect: String,
    val perks: List<String>
)


data class ShopCosmeticItem(
    val id: String,
    val title: String,
    val coinPrice: Long,
    val colorArgb: Long,
    val subtitle: String
)

object VibeShopCatalog {
    val titles = listOf(
        ShopCosmeticItem("singer", "Singer", 12_000L, 0xFF22D3EE, "The voice of VibePlay"),
        ShopCosmeticItem("charming", "Charming", 15_000L, 0xFFF472B6, "A little sparkle, a lot of charm"),
        ShopCosmeticItem("princess", "Princess", 18_000L, 0xFFC084FC, "A royal profile identity"),
        ShopCosmeticItem("prince", "Prince", 18_000L, 0xFF60A5FA, "A royal profile identity"),
        ShopCosmeticItem("queen", "Queen", 25_000L, 0xFFFF72B6, "Own your crown"),
        ShopCosmeticItem("king", "King", 25_000L, 0xFFFBBF24, "Rule your own stage"),
        ShopCosmeticItem("boss", "Boss", 30_000L, 0xFFFB923C, "Confident. Bold. Unmissable."),
        ShopCosmeticItem("filthy_rich", "Filthy Rich", 50_000L, 0xFFFFD700, "Luxury looks good on you"),
        ShopCosmeticItem("legend", "Legend", 65_000L, 0xFF34D399, "A name worth remembering"),
        ShopCosmeticItem("royal_star", "Royal Star", 80_000L, 0xFFA78BFA, "A premium royal signature")
    )
    val frames = listOf(
        ShopCosmeticItem("neon_cyan", "Neon Aurora", 15_000L, 0xFF22D3EE, "Animated ornate neon-cyan avatar frame"),
        ShopCosmeticItem("rose_gold", "Rose Gold", 25_000L, 0xFFF472B6, "Soft rose-gold shimmer"),
        ShopCosmeticItem("royal_gold", "Royal Gold", 40_000L, 0xFFFBBF24, "A golden royal outline"),
        ShopCosmeticItem("galaxy", "Galaxy", 60_000L, 0xFFA78BFA, "Deep-space violet frame"),
        ShopCosmeticItem("aurora", "Aurora", 80_000L, 0xFF34D399, "Aurora-inspired premium frame"),
        ShopCosmeticItem("vip_dragon_1", "VIP 1 · Luxury Gold", 90_000L, 0xFFB87333, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_2", "VIP 2 · Royal Gold", 110_000L, 0xFF8C5A2B, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_3", "VIP 3 · Imperial Gold", 130_000L, 0xFF6F42C1, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_4", "VIP 4 · Diamond Gold", 150_000L, 0xFF2563EB, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_5", "VIP 5 · Elite Gold", 175_000L, 0xFF0F9D78, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_6", "VIP 6 · Prestige Gold", 200_000L, 0xFF9333EA, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_7", "VIP 7 · Supreme Gold", 230_000L, 0xFF0891B2, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_dragon_8", "VIP 8 · Ultimate Gold", 275_000L, 0xFFCA8A04, "Thick royal gold frame with jeweled crown"),
        ShopCosmeticItem("vip_gold_banner_1", "VIP 1 · Golden Banner", 90_000L, 0xFFD4AF37, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_2", "VIP 2 · Golden Banner", 110_000L, 0xFFC9A227, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_3", "VIP 3 · Golden Banner", 130_000L, 0xFFE0B84F, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_4", "VIP 4 · Golden Banner", 150_000L, 0xFFB8860B, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_5", "VIP 5 · Golden Banner", 175_000L, 0xFFF2C14E, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_6", "VIP 6 · Golden Banner", 200_000L, 0xFFAA771C, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_7", "VIP 7 · Golden Banner", 230_000L, 0xFFE6BE65, "Golden VIP frame with crown and VIP banner"),
        ShopCosmeticItem("vip_gold_banner_8", "VIP 8 · Golden Banner", 275_000L, 0xFF967117, "Golden VIP frame with crown and VIP banner")
    )
}

object VibeCatalogDefaults {
    val defaultGifts = listOf(
        GiftCatalogItem(id = "gift_sunset_love", name = "Sunset Love", category = "Love", coinCost = 19999L, accentColorHex = "#F9A8D4"),
        GiftCatalogItem(id = "gift_royal_vows", name = "Royal Vows", category = "Love", coinCost = 99999L, accentColorHex = "#D4AF37"),
        GiftCatalogItem(id = "gift_sunlit_dream", name = "Sunlit Dream", category = "Special", coinCost = 29999L, accentColorHex = "#F6C779"),
        GiftCatalogItem(id = "gift_prism_muse", name = "Prism Muse", category = "Special", coinCost = 49999L, accentColorHex = "#B78BFF")
    )

    val vipTiers = listOf(
        VipTier(1, "VIP 1 · Starlight", 100_000L, 0xFFE53935, "Cyan Ripple Entrance", listOf("VIP 1 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(2, "VIP 2 · Neon Pulse", 1_000_000L, 0xFFFB8C00, "Aurora Wave Banner", listOf("VIP 2 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(3, "VIP 3 · Coral Crown", 3_000_000L, 0xFFFDD835, "Pink Spotlight Arrival", listOf("VIP 3 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(4, "VIP 4 · Royal Gold", 7_000_000L, 0xFF43A047, "Golden Crown Shower", listOf("VIP 4 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(5, "VIP 5 · Apex Legend", 10_000_000L, 0xFF00897B, "Full-Screen Galaxy Entrance", listOf("VIP 5 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(6, "VIP 6 · Aurora", 14_000_000L, 0xFF1E88E5, "Aurora Portal", listOf("VIP 6 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(7, "VIP 7 · Eclipse", 16_000_000L, 0xFF8E24AA, "Eclipse Spotlight", listOf("VIP 7 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect")),
        VipTier(8, "VIP 8 · Stellar", 20_000_000L, 0xFFD81B60, "Stellar Burst", listOf("VIP 8 Exclusive Badge", "Premium Profile Frame", "Special Room Entrance Effect"))
    )
}


@Keep
@IgnoreExtraProperties
data class RoomMusicState(
    val trackId: String = "",
    val status: String = "STOPPED",
    val positionMs: Long = 0L,
    val updatedBy: String = "",
    val updatedAt: Timestamp? = null,
    val queueTrackIds: List<String> = emptyList()
)
