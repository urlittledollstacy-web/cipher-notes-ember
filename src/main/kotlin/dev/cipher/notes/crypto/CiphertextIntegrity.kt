package dev.cipher.notes.crypto

import java.security.MessageDigest

/**
 * Fingerprints a note's stored ciphertext so an unlock can tell a damaged note
 * from a wrong password.
 *
 * AES-GCM authenticates the ciphertext, but a bad tag is raised identically
 * whether the key was wrong or the bytes were altered, so the exception alone
 * cannot separate the two. The digest is recorded when the note is sealed and
 * compared before decrypting: a mismatch means the stored bytes changed, which
 * is damage, not a bad password.
 */
object CiphertextIntegrity {

    fun fingerprint(ciphertext: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(ciphertext.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    /**
     * True when the stored ciphertext is known to be intact.
     *
     * A null [expected] means the note predates the integrity column. It is given
     * the benefit of the doubt so a wrong password on an old note is still
     * reported as one.
     */
    fun matches(ciphertext: String, expected: String?): Boolean =
        expected == null || expected == fingerprint(ciphertext)
}
