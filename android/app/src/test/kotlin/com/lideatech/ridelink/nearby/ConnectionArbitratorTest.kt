package com.lideatech.ridelink.nearby

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionArbitratorTest {
    @Test
    fun `exactly one peer initiates`() {
        assertTrue(ConnectionArbitrator.shouldInitiate("aaa", "bbb"))
        assertFalse(ConnectionArbitrator.shouldInitiate("bbb", "aaa"))
    }
}
