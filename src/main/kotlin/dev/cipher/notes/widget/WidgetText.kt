package dev.cipher.notes.widget

/**
 * Pure presentation rules for the home-screen widget. Kept free of Android
 * dependencies so the privacy behaviour can be unit tested: a widget can never
 * surface note body text, because no function here accepts it.
 */
object WidgetText {

    fun title(noteTitle: String, isTitleVisible: Boolean): String =
        if (isTitleVisible) noteTitle.ifEmpty { "Untitled" } else "•••••"

    fun status(encrypted: Boolean): String =
        if (encrypted) "Sealed" else "Open to read"
}
