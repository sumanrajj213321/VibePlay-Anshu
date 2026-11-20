package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VoiceRoom
import com.example.data.model.VoiceSeat
import com.example.data.model.VoiceStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read VibePlay app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VibePlay", appName)
    }

    @Test
    fun `voice stage strictly enforces 1 host seat and 8 speaker seats totaling 9`() {
        val room = VoiceRoom(
            id = "room_1",
            hostId = "host_uid",
            hostName = "DJ Nova",
            title = "Midnight Vibes"
        )
        // Even if raw Firestore data is missing seats, contains extra invalid index 9,
        // or attempts to put someone else in seat 0, VoiceStage must enforce 1 Host + 8 Speakers = 9.
        val raw = listOf(
            VoiceSeat(id = "0", seatIndex = 0L, role = "HOST", occupantId = "intruder", occupantName = "Intruder"),
            VoiceSeat(id = "1", seatIndex = 1L, role = "SPEAKER", occupantId = "speaker_1", occupantName = "Alex", muted = false),
            VoiceSeat(id = "9", seatIndex = 9L, role = "SPEAKER", occupantId = "extra", occupantName = "Extra")
        )

        val stage = VoiceStage.buildStrictNineSeatStage(room, raw)

        assertEquals(9, stage.totalSeats)
        assertEquals(8, stage.speakerSeats.size)
        assertEquals(0L, stage.hostSeat.seatIndex)
        assertEquals("HOST", stage.hostSeat.role)
        assertEquals("host_uid", stage.hostSeat.occupantId)
        assertEquals("DJ Nova", stage.hostSeat.occupantName)

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), stage.speakerSeats.map { it.seatIndex })
        assertTrue(stage.speakerSeats.all { it.role == "SPEAKER" })
        assertEquals("speaker_1", stage.speakerSeats[0].occupantId)
        assertFalse(stage.speakerSeats[0].muted)
        assertFalse(stage.speakerSeats[1].isOccupied)
    }
}
