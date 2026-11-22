package com.example

import com.example.data.livekit.LiveKitVoiceEngine
import com.example.data.livekit.VoiceConnectionState
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import com.example.data.model.VibeCatalogDefaults
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun vipCatalog_hasOnlyEightLevelsWithUniqueBadgeColors() {
        val tiers = VibeCatalogDefaults.vipTiers
        assertEquals((1..8).toList(), tiers.map { it.level })
        assertEquals(8, tiers.map { it.badgeColorHex }.distinct().size)
    }

    @Test
    fun voiceStage_alwaysBuildsNineSeats_evenWhenEmpty() {
        val room = VoiceRoom(id = "r1", hostId = "h1", hostName = "Host", title = "Chill")
        val stage = VoiceStage.buildStrictNineSeatStage(room, emptyList())
        assertEquals(9, stage.totalSeats)
        assertEquals(1, listOf(stage.hostSeat).size)
        assertEquals(8, stage.speakerSeats.size)
    }

    @Test
    fun voiceStage_constructorRejectsWrongSeatCount() {
        val hostSeat = VoiceSeat(id = "0", seatIndex = 0L, role = "HOST", occupantId = "h1")
        val sevenSeats = (1..7).map { VoiceSeat(id = "$it", seatIndex = it.toLong(), role = "SPEAKER") }
        assertThrows(IllegalArgumentException::class.java) {
            VoiceStage(hostSeat = hostSeat, speakerSeats = sevenSeats)
        }
    }

    @Test
    fun liveKitVoiceEngine_remainsUnconfiguredAndNonBlockingWithoutCredentials() = runTest {
        val engine = LiveKitVoiceEngine()
        assertFalse(engine.isConfigured)
        assertTrue(engine.connectionState.value is VoiceConnectionState.Unconfigured)
        val result = engine.connectToRoom(
            roomId = "room_1",
            userId = "user_1",
            username = "Tester",
            canPublish = false
        )
        assertTrue(result.isSuccess)
        assertTrue(engine.connectionState.value is VoiceConnectionState.Unconfigured)
    }

    @Test
    fun seatAssignment_allowsOpenSeatAndReusesVacatedSeat() {
        val room = VoiceRoom(id = "r1", hostId = "h1", hostName = "Host", title = "Vibe")
        val initialStage = VoiceStage.buildStrictNineSeatStage(room, emptyList())

        val assignResult = VoiceStage.canAssignUserToSeat(
            room = room,
            stage = initialStage,
            targetSeatIndex = 3,
            candidateUserId = "u_listener_1"
        )
        assertTrue(assignResult.isSuccess)
        assertEquals(3L, assignResult.getOrNull()?.seatIndex)

        // Once vacated, the same seat index becomes available again
        val occupiedSeat3 = VoiceSeat(
            id = "3",
            seatIndex = 3L,
            role = "SPEAKER",
            occupantId = "u_listener_1",
            occupantName = "Alex"
        )
        val occupiedStage = VoiceStage.buildStrictNineSeatStage(room, listOf(occupiedSeat3))
        assertTrue(
            VoiceStage.canAssignUserToSeat(room, occupiedStage, 3, "u_listener_2").isFailure
        )

        val vacatedSeat3 = VoiceSeat(id = "3", seatIndex = 3L, role = "SPEAKER", occupantId = "")
        val vacatedStage = VoiceStage.buildStrictNineSeatStage(room, listOf(vacatedSeat3))
        assertTrue(
            VoiceStage.canAssignUserToSeat(room, vacatedStage, 3, "u_listener_2").isSuccess
        )
    }

    @Test
    fun seatAssignment_rejectsDuplicateUserSeatHostAndTenthSeat() {
        val room = VoiceRoom(id = "r1", hostId = "h1", hostName = "Host", title = "Vibe")
        val existingSeats = listOf(
            VoiceSeat(id = "1", seatIndex = 1L, role = "SPEAKER", occupantId = "u_bob", occupantName = "Bob"),
            VoiceSeat(id = "2", seatIndex = 2L, role = "SPEAKER", occupantId = "", seatLocked = true)
        )
        val stage = VoiceStage.buildStrictNineSeatStage(room, existingSeats)

        // Cannot assign same user to a second speaker seat
        assertTrue(VoiceStage.canAssignUserToSeat(room, stage, 4, "u_bob").isFailure)
        // Cannot assign two users to the same occupied seat
        assertTrue(VoiceStage.canAssignUserToSeat(room, stage, 1, "u_alice").isFailure)
        // Cannot assign host into a speaker seat (Host always stays on Seat 0)
        assertTrue(VoiceStage.canAssignUserToSeat(room, stage, 4, "h1").isFailure)
        // Cannot assign to Seat 0 or 10th Seat 9
        assertTrue(VoiceStage.canAssignUserToSeat(room, stage, 0, "u_alice").isFailure)
        assertTrue(VoiceStage.canAssignUserToSeat(room, stage, 9, "u_alice").isFailure)
        // Cannot assign to a locked seat
        assertTrue(VoiceStage.canAssignUserToSeat(room, stage, 2, "u_alice").isFailure)
    }

    @Test
    fun findAvailableSpeakerSeat_prefersRequestedSeatOrFallsBackCleanly() {
        val room = VoiceRoom(id = "r1", hostId = "h1", hostName = "Host", title = "Vibe")
        val seats = listOf(
            VoiceSeat(id = "1", seatIndex = 1L, role = "SPEAKER", occupantId = "u1"),
            VoiceSeat(id = "3", seatIndex = 3L, role = "SPEAKER", occupantId = "")
        )
        val stage = VoiceStage.buildStrictNineSeatStage(room, seats)

        // Preferred seat 3 is open -> picks 3
        assertEquals(3L, VoiceStage.findAvailableSpeakerSeat(stage, preferredSeatIndex = 3)?.seatIndex)
        // Preferred seat 1 is already taken -> falls back to open seat 2
        assertEquals(2L, VoiceStage.findAvailableSpeakerSeat(stage, preferredSeatIndex = 1)?.seatIndex)

        // When all 8 seats are occupied, returns null (never creates a 9th speaker / 10th seat)
        val fullSeats = (1..8).map { VoiceSeat(id = "$it", seatIndex = it.toLong(), role = "SPEAKER", occupantId = "u_$it") }
        val fullStage = VoiceStage.buildStrictNineSeatStage(room, fullSeats)
        assertEquals(null, VoiceStage.findAvailableSpeakerSeat(fullStage, preferredSeatIndex = 0))
    }
}
