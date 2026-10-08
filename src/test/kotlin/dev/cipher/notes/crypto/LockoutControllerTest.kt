package dev.cipher.notes.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory stand-in for the persisted store, so the policy is testable off-device. */
private class FakeLockoutStateStore : LockoutStateStore {
    private val attempts = mutableMapOf<String, Int>()
    private val until = mutableMapOf<String, Long>()

    override suspend fun attempts(noteId: String): Int = attempts[noteId] ?: 0
    override suspend fun lockoutUntil(noteId: String): Long = until[noteId] ?: 0L

    override suspend fun save(noteId: String, attempts: Int, lockoutUntil: Long) {
        this.attempts[noteId] = attempts
        if (lockoutUntil > 0L) until[noteId] = lockoutUntil else until.remove(noteId)
    }

    override suspend fun clear(noteId: String) {
        attempts.remove(noteId)
        until.remove(noteId)
    }
}

class LockoutControllerTest {

    private val store = FakeLockoutStateStore()
    private val noteId = "note-1"

    @Test
    fun `no wait before the attempt threshold`() = kotlinx.coroutines.runBlocking {
        val controller = LockoutController(store)
        repeat(PinThrottle.MAX_ATTEMPTS - 1) {
            assertEquals(0L, controller.recordFailure(noteId, 1_000L))
        }
        assertEquals(0L, controller.remainingLockoutMs(noteId, 1_000L))
    }

    @Test
    fun `wait starts at the threshold and doubles after`() = kotlinx.coroutines.runBlocking {
        val controller = LockoutController(store)
        repeat(PinThrottle.MAX_ATTEMPTS - 1) { controller.recordFailure(noteId, 1_000L) }

        assertEquals(PinThrottle.BASE_LOCKOUT_MS, controller.recordFailure(noteId, 1_000L))
        assertEquals(PinThrottle.BASE_LOCKOUT_MS * 2, controller.recordFailure(noteId, 1_000L))
    }

    @Test
    fun `wait outlives the instance that set it`() = kotlinx.coroutines.runBlocking {
        LockoutController(store).let { first ->
            repeat(PinThrottle.MAX_ATTEMPTS) { first.recordFailure(noteId, 1_000L) }
        }

        // A fresh controller stands in for reopening the note: on the old,
        // memory-only lockout this read a clean slate and leaked the wait.
        val reopened = LockoutController(store)
        assertTrue(reopened.remainingLockoutMs(noteId, 1_000L) > 0L)
        assertEquals(0L, reopened.remainingLockoutMs(noteId, 1_000L + PinThrottle.BASE_LOCKOUT_MS + 1))
    }

    @Test
    fun `a correct password clears the wait`() = kotlinx.coroutines.runBlocking {
        val controller = LockoutController(store)
        repeat(PinThrottle.MAX_ATTEMPTS + 2) { controller.recordFailure(noteId, 1_000L) }
        assertTrue(controller.remainingLockoutMs(noteId, 1_000L) > 0L)

        controller.recordSuccess(noteId)

        assertEquals(0L, controller.remainingLockoutMs(noteId, 1_000L))
        assertEquals(0, store.attempts(noteId))
    }

    @Test
    fun `a lockout on one note does not lock another`() = kotlinx.coroutines.runBlocking {
        val controller = LockoutController(store)
        repeat(PinThrottle.MAX_ATTEMPTS) { controller.recordFailure("note-A", 1_000L) }

        assertTrue(controller.remainingLockoutMs("note-A", 1_000L) > 0L)
        assertEquals(0L, controller.remainingLockoutMs("note-B", 1_000L))
    }
}
