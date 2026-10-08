package dev.cipher.notes.crypto

/**
 * Where a note's unlock lockout state lives.
 *
 * Kept behind an interface so the policy in [LockoutController] can be tested
 * without a DataStore or a running Android device.
 */
interface LockoutStateStore {

    suspend fun attempts(noteId: String): Int

    suspend fun lockoutUntil(noteId: String): Long

    suspend fun save(noteId: String, attempts: Int, lockoutUntil: Long)

    suspend fun clear(noteId: String)
}
