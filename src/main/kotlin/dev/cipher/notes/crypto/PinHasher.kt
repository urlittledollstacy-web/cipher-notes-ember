package dev.cipher.notes.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores app-lock PINs as salted PBKDF2 hashes instead of plaintext.
 *
 * Format: `v1$<iterations>$<base64 salt>$<base64 hash>`. Storing the version and
 * iteration count alongside the hash lets either be raised later without
 * invalidating PINs already on disk.
 *
 * PINs are only 4 digits, so a hash alone does not make them strong - the
 * attempt throttle on the lock screen is what does that. This removes the
 * "read the file, learn the PIN" step.
 */
object PinHasher {
    private const val VERSION = "v1"
    private const val ITERATIONS = 200_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val SEPARATOR = "$"

    fun hash(pin: String, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).apply { SecureRandom().nextBytes(this) }
        val digest = derive(pin, salt, iterations)
        val encoder = Base64.getEncoder()
        return listOf(
            VERSION,
            iterations.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(digest)
        ).joinToString(SEPARATOR)
    }

    /**
     * Verifies [pin] against a stored hash. The comparison is constant-time so a
     * wrong PIN cannot be narrowed down by how long the check takes.
     */
    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 4 || parts[0] != VERSION) return false
        return try {
            val decoder = Base64.getDecoder()
            val expected = decoder.decode(parts[3])
            val actual = derive(pin, decoder.decode(parts[2]), parts[1].toInt())
            constantTimeEquals(expected, actual)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** True when [stored] predates hashing and still holds the raw PIN. */
    fun isLegacyPlaintext(stored: String): Boolean = !stored.startsWith(VERSION + SEPARATOR)

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
