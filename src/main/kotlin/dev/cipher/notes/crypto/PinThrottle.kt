package dev.cipher.notes.crypto

/**
 * How long the app lock refuses input after repeated wrong PINs.
 *
 * Kept separate from the storage layer so the growth curve can be tested
 * without a DataStore. The curve mirrors the note unlock: nothing until
 * [MAX_ATTEMPTS], then a delay that doubles per further failure.
 */
object PinThrottle {
    const val MAX_ATTEMPTS = 5
    const val BASE_LOCKOUT_MS = 5_000L

    /** Lockout in milliseconds after [attempts] consecutive failures; 0 below the threshold. */
    fun lockoutMs(attempts: Int): Long {
        if (attempts < MAX_ATTEMPTS) return 0L
        val doublings = (attempts - MAX_ATTEMPTS).coerceAtMost(6)
        return BASE_LOCKOUT_MS shl doublings
    }
}
