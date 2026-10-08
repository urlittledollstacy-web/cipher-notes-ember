package dev.cipher.notes.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinThrottleTest {

    @Test
    fun `no lockout before the attempt threshold`() {
        assertEquals(0L, PinThrottle.lockoutMs(0))
        assertEquals(0L, PinThrottle.lockoutMs(1))
        assertEquals(0L, PinThrottle.lockoutMs(4))
    }

    @Test
    fun `lockout starts at the threshold`() {
        assertEquals(5_000L, PinThrottle.lockoutMs(5))
    }

    @Test
    fun `lockout doubles per further failure`() {
        assertEquals(10_000L, PinThrottle.lockoutMs(6))
        assertEquals(20_000L, PinThrottle.lockoutMs(7))
        assertEquals(40_000L, PinThrottle.lockoutMs(8))
    }

    @Test
    fun `lockout is capped`() {
        val capped = PinThrottle.lockoutMs(5 + 6)
        assertEquals(capped, PinThrottle.lockoutMs(50))
        assertTrue(capped <= 5_000L shl 6)
    }

    @Test
    fun `lockout never decreases as attempts grow`() {
        var previous = 0L
        for (attempts in 0..40) {
            val current = PinThrottle.lockoutMs(attempts)
            assertTrue(current >= previous)
            previous = current
        }
    }
}
