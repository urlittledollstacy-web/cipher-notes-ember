package dev.cipher.notes.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CiphertextIntegrityTest {

    @Test
    fun `fingerprint matches the known SHA-256 of the input`() {
        // SHA-256 of the empty string, to pin the algorithm and encoding.
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            CiphertextIntegrity.fingerprint("")
        )
    }

    @Test
    fun `fingerprint is stable for the same input`() {
        val value = "Q2lwaGVyTm90ZXM="
        assertEquals(CiphertextIntegrity.fingerprint(value), CiphertextIntegrity.fingerprint(value))
    }

    @Test
    fun `fingerprint changes when a single character changes`() {
        assertNotEquals(
            CiphertextIntegrity.fingerprint("abcdef"),
            CiphertextIntegrity.fingerprint("abcdeg")
        )
    }

    @Test
    fun `intact ciphertext matches its recorded fingerprint`() {
        val ciphertext = "AAECAwQFBgcICQoLDA0ODw=="
        val recorded = CiphertextIntegrity.fingerprint(ciphertext)
        assertTrue(CiphertextIntegrity.matches(ciphertext, recorded))
    }

    @Test
    fun `altered ciphertext does not match`() {
        val recorded = CiphertextIntegrity.fingerprint("original-bytes")
        assertFalse(CiphertextIntegrity.matches("altered-bytes", recorded))
    }

    @Test
    fun `a note without a recorded fingerprint is not treated as damaged`() {
        // Legacy rows predate the column: give them the benefit of the doubt so
        // a wrong password is still reported as a wrong password.
        assertTrue(CiphertextIntegrity.matches("anything", null))
    }
}
