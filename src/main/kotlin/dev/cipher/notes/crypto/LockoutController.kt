package dev.cipher.notes.crypto

/**
 * Owns the note-unlock lockout: how many wrong passwords a note has seen, and
 * how long it must wait before the next try.
 *
 * The state is written straight through to [LockoutStateStore], so it survives
 * leaving the note screen and restarting the app. That is the point: a lockout
 * held only in a view model was erased by a back press, letting a guesser reset
 * the wait at will.
 */
class LockoutController(private val store: LockoutStateStore) {

    /** Remaining wait before a password may be tried, or 0 if a try is allowed. */
    suspend fun remainingLockoutMs(noteId: String, now: Long): Long =
        (store.lockoutUntil(noteId) - now).coerceAtLeast(0L)

    /** Records a wrong password and returns the wait that now applies. */
    suspend fun recordFailure(noteId: String, now: Long): Long {
        val attempts = store.attempts(noteId) + 1
        val backoff = PinThrottle.lockoutMs(attempts)
        val until = if (backoff > 0L) now + backoff else 0L
        store.save(noteId, attempts, until)
        return backoff
    }

    /** A correct password wipes the count and the wait. */
    suspend fun recordSuccess(noteId: String) = store.clear(noteId)
}
