package dev.cipher.notes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockLayoutTest {

    private val screenSizes = listOf(
        // label to (widthDp to heightDp)
        "small phone portrait" to (320f to 480f),
        "small phone landscape" to (480f to 320f),
        "large phone portrait" to (412f to 915f),
        "large phone landscape" to (915f to 412f),
        "tablet portrait" to (800f to 1280f),
        "tablet landscape" to (1280f to 800f),
        "split-screen short" to (412f to 260f),
        "split-screen narrow" to (320f to 900f),
        "square floating window" to (500f to 500f)
    )

    @Test
    fun `keypad never wider than the area it is given`() {
        for ((label, size) in screenSizes) {
            val (w, h) = size
            val key = LockLayout.keySizeDp(w, h)
            val needed = LockLayout.keypadWidthDp(key)
            assertTrue("$label: keypad $needed exceeds width $w", needed <= w + 0.01f)
        }
    }

    @Test
    fun `key stays within the usable range on every screen`() {
        for ((label, size) in screenSizes) {
            val (w, h) = size
            val key = LockLayout.keySizeDp(w, h)
            assertTrue(
                "$label: key $key below usable minimum",
                key >= LockLayout.MIN_KEY_DP - 0.01f
            )
            assertTrue(
                "$label: key $key above usable maximum",
                key <= LockLayout.MAX_KEY_DP + 0.01f
            )
        }
    }

    @Test
    fun `a tiny window still yields a tappable key`() {
        assertEquals(LockLayout.MIN_KEY_DP, LockLayout.keySizeDp(120f, 200f), 0.01f)
    }

    @Test
    fun `a large tablet is capped rather than ballooning`() {
        assertEquals(LockLayout.MAX_KEY_DP, LockLayout.keySizeDp(2000f, 2000f), 0.01f)
    }

    @Test
    fun `arrangement follows aspect ratio, not absolute size`() {
        assertTrue(LockLayout.isWide(915f, 412f))
        assertTrue(LockLayout.isWide(1280f, 800f))
        assertFalse(LockLayout.isWide(412f, 915f))
        assertFalse(LockLayout.isWide(320f, 900f))
        assertFalse(LockLayout.isWide(500f, 500f))
    }

    @Test
    fun `the header shrinks with the keys`() {
        val small = LockLayout.headerScale(LockLayout.MIN_KEY_DP)
        val large = LockLayout.headerScale(LockLayout.MAX_KEY_DP)
        assertTrue(small < large)
        assertTrue(small >= 0.6f)
        assertTrue(large <= 1f)
    }
}
