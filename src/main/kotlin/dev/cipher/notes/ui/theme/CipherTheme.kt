package dev.cipher.notes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.cipher.notes.R

// Ember Archive palette
val Ink = Color(0xFF14100D)
val Ink2 = Color(0xFF1B1611)
val Surface1 = Color(0xFF221C16)
val Surface2 = Color(0xFF2A2219)
val Line = Color(0xFF33291F)
val LineStrong = Color(0xFF4A3B2C)
val Paper = Color(0xFFF3EAE0)
val Body = Color(0xFFC8B8A8)
val Dim = Color(0xFF8A7A6A)
val Faint = Color(0xFF5E5347)
val Ember = Color(0xFFE8703A)
val EmberSoft = Color(0xFFF09A6E)
val Brass = Color(0xFFD9A441)
val Wax = Color(0xFFC2402D)
val Sage = Color(0xFF8FA968)

private val DarkScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Ink,
    primaryContainer = Surface2,
    onPrimaryContainer = EmberSoft,
    secondary = Brass,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A2E1A),
    onSecondaryContainer = Brass,
    tertiary = Sage,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF2A3320),
    onTertiaryContainer = Sage,
    background = Ink,
    onBackground = Body,
    surface = Surface1,
    onSurface = Paper,
    surfaceVariant = Surface2,
    onSurfaceVariant = Dim,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Ink2,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Line,
    surfaceTint = Ember,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    error = Wax,
    onError = Paper,
    errorContainer = Color(0xFF3A1C14),
    onErrorContainer = Color(0xFFF0A08A),
    outline = Line,
    outlineVariant = LineStrong,
    scrim = Color(0xFF000000)
)

// Ember by daylight: warm paper, burnt-ember accent, dark brass for sealed notes.
private val LightScheme = lightColorScheme(
    primary = Color(0xFFB84A18),
    onPrimary = Color(0xFFFFFBF7),
    primaryContainer = Color(0xFFF6DCCB),
    onPrimaryContainer = Color(0xFF5A2410),
    secondary = Color(0xFF8A6A1F),
    onSecondary = Color(0xFFFFFBF7),
    secondaryContainer = Color(0xFFF1E2BC),
    onSecondaryContainer = Color(0xFF3E2F07),
    tertiary = Color(0xFF4F6B3A),
    onTertiary = Color(0xFFFFFBF7),
    tertiaryContainer = Color(0xFFDDE9CE),
    onTertiaryContainer = Color(0xFF22350F),
    background = Color(0xFFFAF6F1),
    onBackground = Color(0xFF241C15),
    surface = Color(0xFFF3ECE4),
    onSurface = Color(0xFF241C15),
    surfaceVariant = Color(0xFFEADFD2),
    onSurfaceVariant = Color(0xFF6B5B4C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1EA),
    surfaceContainer = Color(0xFFF3ECE4),
    surfaceContainerHigh = Color(0xFFEEE4D9),
    surfaceContainerHighest = Color(0xFFE7DBCB),
    surfaceTint = Color(0xFFB84A18),
    inverseSurface = Color(0xFF241C15),
    inverseOnSurface = Color(0xFFFAF6F1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFFD6C6B6),
    outlineVariant = Color(0xFFBCA893),
    scrim = Color(0xFF000000)
)

// Solarized Light (Ethan Schoonover). Neutrals are the canonical base3/base2/base1/base01,
// accents the canonical blue/green/yellow/red. A touch soft by design.
private val SolarizedScheme = lightColorScheme(
    primary = Color(0xFF268BD2),
    onPrimary = Color(0xFFFDF6E3),
    primaryContainer = Color(0xFFD5E6F4),
    onPrimaryContainer = Color(0xFF0F3A56),
    secondary = Color(0xFFB58900),
    onSecondary = Color(0xFF1A1500),
    secondaryContainer = Color(0xFFF1E2B8),
    onSecondaryContainer = Color(0xFF4A3800),
    tertiary = Color(0xFF859900),
    onTertiary = Color(0xFF1A1C00),
    tertiaryContainer = Color(0xFFE2E6C4),
    onTertiaryContainer = Color(0xFF373D00),
    background = Color(0xFFFDF6E3),
    onBackground = Color(0xFF586E75),
    surface = Color(0xFFEEE8D5),
    onSurface = Color(0xFF586E75),
    surfaceVariant = Color(0xFFEEE8D5),
    onSurfaceVariant = Color(0xFF657B83),
    surfaceContainerLowest = Color(0xFFFDF6E3),
    surfaceContainerLow = Color(0xFFEEE8D5),
    surfaceContainer = Color(0xFFEEE8D5),
    surfaceContainerHigh = Color(0xFFE4DDC8),
    surfaceContainerHighest = Color(0xFFD9D2BC),
    surfaceTint = Color(0xFF268BD2),
    inverseSurface = Color(0xFF586E75),
    inverseOnSurface = Color(0xFFFDF6E3),
    error = Color(0xFFDC322F),
    onError = Color(0xFFFDF6E3),
    errorContainer = Color(0xFFF5D0CE),
    onErrorContainer = Color(0xFF4A0E0D),
    outline = Color(0xFF93A1A1),
    outlineVariant = Color(0xFFB9C3C3),
    scrim = Color(0xFF000000)
)

// Pastel Pink: blush background, plum text, rose accent. Text is kept dark plum
// rather than grey so it stays readable on the pink surface.
private val PastelPinkScheme = lightColorScheme(
    primary = Color(0xFFB03060),
    onPrimary = Color(0xFFFFF7FA),
    primaryContainer = Color(0xFFFBD3E0),
    onPrimaryContainer = Color(0xFF5C1533),
    secondary = Color(0xFFB5628A),
    onSecondary = Color(0xFFFFF7FA),
    secondaryContainer = Color(0xFFF9DCE8),
    onSecondaryContainer = Color(0xFF4C2038),
    tertiary = Color(0xFF6E8B6B),
    onTertiary = Color(0xFFFFF7FA),
    tertiaryContainer = Color(0xFFDDE8DA),
    onTertiaryContainer = Color(0xFF2A3A28),
    background = Color(0xFFFFF1F5),
    onBackground = Color(0xFF3F2733),
    surface = Color(0xFFFDE3EC),
    onSurface = Color(0xFF3F2733),
    surfaceVariant = Color(0xFFF9DCE8),
    onSurfaceVariant = Color(0xFF7A5666),
    surfaceContainerLowest = Color(0xFFFFF7FA),
    surfaceContainerLow = Color(0xFFFDEBF1),
    surfaceContainer = Color(0xFFFDE3EC),
    surfaceContainerHigh = Color(0xFFF9DCE8),
    surfaceContainerHighest = Color(0xFFF5D2E1),
    surfaceTint = Color(0xFFB03060),
    inverseSurface = Color(0xFF3F2733),
    inverseOnSurface = Color(0xFFFFF1F5),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFFE6C2D0),
    outlineVariant = Color(0xFFD3A8BA),
    scrim = Color(0xFF000000)
)

val Fraunces = FontFamily(
    Font(R.font.fraunces, FontWeight.Normal),
    Font(R.font.fraunces, FontWeight.Medium),
    Font(R.font.fraunces, FontWeight.SemiBold),
    Font(R.font.fraunces, FontWeight.Bold)
)

val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk, FontWeight.Normal),
    Font(R.font.hanken_grotesk, FontWeight.Medium),
    Font(R.font.hanken_grotesk, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk, FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

private val EmberTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 58.sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 14.sp)
)

/**
 * A stored theme choice. [EMBER] is always dark. [LIGHT], [SOLARIZED] and
 * [PASTEL_PINK] are light-only and ignore the system setting.
 */
enum class ThemeMode(val key: String, val label: String, val summary: String) {
    EMBER("ember", "Ember Archive", "Warm dark paper, always dark"),
    LIGHT("light", "Light", "Warm light only"),
    SOLARIZED("solarized", "Solarized", "Soft contrast, classic palette"),
    PASTEL_PINK("pastel_pink", "Pastel Pink", "Blush background, rose accent");

    companion object {
        val DEFAULT = EMBER

        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** Background, accent and secondary swatches, for the settings theme picker. */
fun themeSwatches(mode: ThemeMode): List<Color> = when (mode) {
    ThemeMode.EMBER -> listOf(Ink, Ember, Brass)
    ThemeMode.LIGHT -> listOf(Color(0xFFFAF6F1), Color(0xFFB84A18), Color(0xFF8A6A1F))
    ThemeMode.SOLARIZED -> listOf(Color(0xFFFDF6E3), Color(0xFF268BD2), Color(0xFFB58900))
    ThemeMode.PASTEL_PINK -> listOf(Color(0xFFFFF1F5), Color(0xFFB03060), Color(0xFFB5628A))
}

/**
 * Switch colours shared by every settings toggle.
 *
 * Material's off defaults are `outline` for the thumb on `surfaceVariant` for
 * the track. In Ember those are both nearly the same dark brown (#33291F on
 * #2A2219), so the off thumb disappeared into its track and the switch read as
 * on. A mid-tone [androidx.compose.material3.ColorScheme.onSurfaceVariant] thumb
 * stays visible against the track in every theme.
 */
@Composable
fun cipherSwitchColors(
    checkedColor: Color = MaterialTheme.colorScheme.primary
): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = checkedColor,
    checkedTrackColor = checkedColor.copy(alpha = 0.3f),
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

/**
 * The one place that maps a theme choice onto a [ColorScheme]. Kept free of
 * Compose so the widget can resolve the same theme from DataStore.
 *
 * [dynamicColorsAvailable] is false on Android 11 and below, where dynamic
 * colors cannot be read, and false for the widget, which cannot resolve
 * wallpaper colors at all. Dynamic colors win over the [ThemeMode] selection
 * because the Appearance screen presents that switch as the stronger choice.
 */
fun cipherColorScheme(
    themeMode: ThemeMode,
    darkTheme: Boolean,
    dynamicColors: Boolean,
    dynamicColorsAvailable: Boolean,
    dynamicDarkScheme: () -> ColorScheme = { DarkScheme },
    dynamicLightScheme: () -> ColorScheme = { LightScheme }
): ColorScheme {
    if (dynamicColors && dynamicColorsAvailable) {
        return if (darkTheme) dynamicDarkScheme() else dynamicLightScheme()
    }
    return when (themeMode) {
        ThemeMode.EMBER -> DarkScheme
        ThemeMode.LIGHT -> LightScheme
        ThemeMode.SOLARIZED -> SolarizedScheme
        ThemeMode.PASTEL_PINK -> PastelPinkScheme
    }
}

@Composable
fun CipherTheme(
    themeMode: ThemeMode = ThemeMode.DEFAULT,
    dynamicColors: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = cipherColorScheme(
        themeMode = themeMode,
        darkTheme = darkTheme,
        dynamicColors = dynamicColors,
        dynamicColorsAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        dynamicDarkScheme = { dynamicDarkColorScheme(context) },
        dynamicLightScheme = { dynamicLightColorScheme(context) }
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EmberTypography,
        content = content
    )
}