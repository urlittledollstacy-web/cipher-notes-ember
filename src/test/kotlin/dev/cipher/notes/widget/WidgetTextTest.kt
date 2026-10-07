package dev.cipher.notes.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WidgetTextTest {

    @Test
    fun `title is hidden when visibility is off`() {
        assertEquals("•••••", WidgetText.title("Bank vault code", isTitleVisible = false))
    }

    @Test
    fun `title falls back to Untitled when blank and visible`() {
        assertEquals("Untitled", WidgetText.title("", isTitleVisible = true))
    }

    @Test
    fun `title is shown when visibility is on`() {
        assertEquals("Recovery phrases", WidgetText.title("Recovery phrases", isTitleVisible = true))
    }

    @Test
    fun `encrypted notes report Sealed`() {
        assertEquals("Sealed", WidgetText.status(encrypted = true))
    }

    @Test
    fun `plaintext notes never expose a preview`() {
        val status = WidgetText.status(encrypted = false)
        assertEquals("Open to read", status)
        assertFalse(status.contains("content"))
    }
}
