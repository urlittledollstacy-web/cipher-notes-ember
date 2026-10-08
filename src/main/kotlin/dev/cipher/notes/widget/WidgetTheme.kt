package dev.cipher.notes.widget

import androidx.compose.ui.graphics.Color
import dev.cipher.notes.ui.theme.ThemeMode

/**
 * Colors for the home-screen widget.
 *
 * Glance cannot reach [androidx.compose.material3.MaterialTheme], and it cannot
 * read the wallpaper for dynamic colors, so the widget carries its own copy of
 * each palette. No theme follows the system any more, so the day and night
 * values are equal; the pairs are kept because [androidx.glance.color.ColorProvider]
 * expects them.
 */
data class WidgetTheme(
    val backgroundDay: Color,
    val backgroundNight: Color,
    val surfaceDay: Color,
    val surfaceNight: Color,
    val titleDay: Color,
    val titleNight: Color,
    val accentDay: Color,
    val accentNight: Color,
    val mutedDay: Color,
    val mutedNight: Color
) {
    companion object {
        // Mirrors the Compose palettes in ui/theme/CipherTheme.kt.
        private val emberBg = Color(0xFF14100D)
        private val emberSurface = Color(0xFF221C16)
        private val emberTitle = Color(0xFFF3EAE0)
        private val emberAccent = Color(0xFFD9A441)
        private val emberMuted = Color(0xFF8A7A6A)

        private val lightBg = Color(0xFFFAF6F1)
        private val lightSurface = Color(0xFFF3ECE4)
        private val lightTitle = Color(0xFF241C15)
        private val lightAccent = Color(0xFF8A6A1F)
        private val lightMuted = Color(0xFF6B5B4C)

        private val solarizedBg = Color(0xFFFDF6E3)
        private val solarizedSurface = Color(0xFFEEE8D5)
        private val solarizedTitle = Color(0xFF586E75)
        private val solarizedAccent = Color(0xFFB58900)
        private val solarizedMuted = Color(0xFF657B83)

        private val pinkBg = Color(0xFFFFF1F5)
        private val pinkSurface = Color(0xFFFDE3EC)
        private val pinkTitle = Color(0xFF3F2733)
        private val pinkAccent = Color(0xFFB03060)
        private val pinkMuted = Color(0xFF7A5666)

        fun of(
            bg: Color, surface: Color, title: Color,
            accent: Color, muted: Color
        ) = WidgetTheme(bg, bg, surface, surface, title, title, accent, accent, muted, muted)

        fun of(mode: ThemeMode): WidgetTheme = when (mode) {
            ThemeMode.EMBER -> of(emberBg, emberSurface, emberTitle, emberAccent, emberMuted)
            ThemeMode.LIGHT -> of(lightBg, lightSurface, lightTitle, lightAccent, lightMuted)
            ThemeMode.SOLARIZED -> of(solarizedBg, solarizedSurface, solarizedTitle, solarizedAccent, solarizedMuted)
            ThemeMode.PASTEL_PINK -> of(pinkBg, pinkSurface, pinkTitle, pinkAccent, pinkMuted)
        }
    }
}
