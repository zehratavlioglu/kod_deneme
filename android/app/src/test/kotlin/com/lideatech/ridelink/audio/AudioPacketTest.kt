package com.lideatech.ridelink.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class AudioPacketTest {
    @Test
    fun `binary packet round trips`() {
        val original = AudioPacket(UUID.randomUUID(), 42, 123456L, byteArrayOf(1, 2, 3, 4))
        val decoded = AudioPacket.decode(original.encode())!!
        assertEquals(original.sessionId, decoded.sessionId)
        assertEquals(42, decoded.sequence)
        assertEquals(123456L, decoded.timestampMicros)
        assertArrayEquals(original.opus, decoded.opus)
    }

    @Test
    fun `invalid packet is rejected`() {
        assertNull(AudioPacket.decode(byteArrayOf(1, 2, 3)))
    }
}
