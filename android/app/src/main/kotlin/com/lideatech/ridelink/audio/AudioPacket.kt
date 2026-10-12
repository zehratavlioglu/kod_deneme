package com.lideatech.ridelink.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

data class AudioPacket(
    val sessionId: UUID,
    val sequence: Int,
    val timestampMicros: Long,
    val opus: ByteArray,
) {
    fun encode(): ByteArray = ByteBuffer.allocate(HEADER_SIZE + opus.size)
        .order(ByteOrder.BIG_ENDIAN)
        .put(MAGIC_0)
        .put(MAGIC_1)
        .put(VERSION)
        .put(TYPE_AUDIO)
        .putLong(sessionId.mostSignificantBits)
        .putLong(sessionId.leastSignificantBits)
        .putInt(sequence)
        .putLong(timestampMicros)
        .putShort(opus.size.toShort())
        .put(opus)
        .array()

    companion object {
        private const val HEADER_SIZE = 34
        private const val MAGIC_0: Byte = 0x52
        private const val MAGIC_1: Byte = 0x4C
        private const val VERSION: Byte = 1
        private const val TYPE_AUDIO: Byte = 1

        fun looksLikeAudio(bytes: ByteArray): Boolean =
            bytes.size >= HEADER_SIZE && bytes[0] == MAGIC_0 && bytes[1] == MAGIC_1

        fun decode(bytes: ByteArray): AudioPacket? = runCatching {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            require(buffer.get() == MAGIC_0 && buffer.get() == MAGIC_1)
            require(buffer.get() == VERSION && buffer.get() == TYPE_AUDIO)
            val session = UUID(buffer.long, buffer.long)
            val sequence = buffer.int
            val timestamp = buffer.long
            val size = buffer.short.toInt() and 0xffff
            require(size == buffer.remaining())
            AudioPacket(session, sequence, timestamp, ByteArray(size).also(buffer::get))
        }.getOrNull()
    }
}
