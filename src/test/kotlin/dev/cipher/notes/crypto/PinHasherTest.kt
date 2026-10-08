package dev.cipher.notes.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `hash does not contain the pin`() {
        val hash = PinHasher.hash("1234", iterations = 1000)
        assertFalse(hash.contains("1234"))
    }

    @Test
    fun `verify accepts the correct pin`() {
        val hash = PinHasher.hash("1234", iterations = 1000)
        assertTrue(PinHasher.verify("1234", hash))
    }

    @Test
    fun `verify rejects a wrong pin`() {
        val hash = PinHasher.hash("1234", iterations = 1000)
        assertFalse(PinHasher.verify("9999", hash))
    }

    @Test
    fun `same pin hashes differently each time`() {
        val a = PinHasher.hash("1234", iterations = 1000)
        val b = PinHasher.hash("1234", iterations = 1000)
        assertNotEquals(a, b)
        assertTrue(PinHasher.verify("1234", a))
        assertTrue(PinHasher.verify("1234", b))
    }

    @Test
    fun `legacy plaintext pin is detected and not accepted as a hash`() {
        assertTrue(PinHasher.isLegacyPlaintext("1234"))
        assertFalse(PinHasher.verify("1234", "1234"))
    }

    @Test
    fun `hash is not reported as legacy plaintext`() {
        val hash = PinHasher.hash("1234", iterations = 1000)
        assertFalse(PinHasher.isLegacyPlaintext(hash))
    }

    @Test
    fun `malformed stored value is rejected`() {
        assertFalse(PinHasher.verify("1234", "v1\$1000\$notbase64!!\$alsobad"))
        assertFalse(PinHasher.verify("1234", ""))
    }
}
