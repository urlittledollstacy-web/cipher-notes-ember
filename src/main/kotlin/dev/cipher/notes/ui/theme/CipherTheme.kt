package dev.cipher.notes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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

@Composable
fun CipherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EmberTypography,
        content = content
    )
}