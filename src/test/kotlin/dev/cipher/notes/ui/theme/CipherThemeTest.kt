package dev.cipher.notes.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CipherThemeTest {

    private val dynamicSentinel = lightColorScheme(background = Color(0xFF123456))

    @Test
    fun `unknown key falls back to the default theme`() {
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey("nonsense"))
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey(null))
    }

    @Test
    fun `keys round trip`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromKey(mode.key))
        }
    }

    @Test
    fun `ember resolves to the dark scheme`() {
        val scheme = resolve(ThemeMode.EMBER, darkTheme = false)
        assertEquals(Ink, scheme.background)
    }

    @Test
    fun `light resolves to the light scheme when the system is light`() {
        val scheme = resolve(ThemeMode.LIGHT, darkTheme = false)
        assertEquals(Color(0xFFFAF6F1), scheme.background)
    }

    @Test
    fun `light stays light on a dark system`() {
        assertEquals(Color(0xFFFAF6F1), resolve(ThemeMode.LIGHT, darkTheme = true).background)
    }

    @Test
    fun `solarized and pastel pink stay light on a dark system`() {
        assertEquals(Color(0xFFFDF6E3), resolve(ThemeMode.SOLARIZED, darkTheme = true).background)
        assertEquals(Color(0xFFFFF1F5), resolve(ThemeMode.PASTEL_PINK, darkTheme = true).background)
    }

    @Test
    fun `dynamic colors win over the theme when available`() {
        val scheme = cipherColorScheme(
            themeMode = ThemeMode.EMBER,
            darkTheme = true,
            dynamicColors = true,
            dynamicColorsAvailable = true,
            dynamicDarkScheme = { dynamicSentinel },
            dynamicLightScheme = { dynamicSentinel }
        )
        assertEquals(dynamicSentinel.background, scheme.background)
    }

    @Test
    fun `dynamic colors are ignored when unavailable`() {
        val scheme = cipherColorScheme(
            themeMode = ThemeMode.PASTEL_PINK,
            darkTheme = true,
            dynamicColors = true,
            dynamicColorsAvailable = false,
            dynamicDarkScheme = { dynamicSentinel },
            dynamicLightScheme = { dynamicSentinel }
        )
        assertEquals(Color(0xFFFFF1F5), scheme.background)
    }

    @Test
    fun `every theme offers three swatches`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(3, themeSwatches(mode).size)
        }
    }

    private fun resolve(mode: ThemeMode, darkTheme: Boolean) = cipherColorScheme(
        themeMode = mode,
        darkTheme = darkTheme,
        dynamicColors = false,
        dynamicColorsAvailable = true
    )
}
