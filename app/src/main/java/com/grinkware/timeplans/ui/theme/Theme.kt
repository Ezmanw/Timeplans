package com.grinkware.timeplans.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grinkware.timeplans.R

data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }

private val DarkColorScheme = darkColorScheme(
    primary = CyanSecondary,
    secondary = CyanSecondary,
    tertiary = CyanTertiary,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    tertiary = CyanTertiary,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface
)

val GoogleSansFamily = FontFamily(
    Font(R.font.googlesans_regular, FontWeight.Normal),
    Font(R.font.googlesans_medium, FontWeight.Medium),
    Font(R.font.googlesans_bold, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold)
)

@Composable
fun TimeplansTheme(
    darkMode: String = "AUTO", // "AUTO", "LIGHT", "DARK"
    amoledMode: Boolean = false,
    dynamicColor: Boolean = true,
    density: String = "NORMAL", // "COMPACT", "NORMAL", "SPACIOUS"
    fontStyle: String = "SYSTEM", // "SYSTEM", "INTER", "MONOSPACE", "SANS"
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Overrides for AMOLED mode
    if (darkTheme && amoledMode) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF070A0F),
            surfaceVariant = Color(0xFF13171F),
            onBackground = Color.White,
            onSurface = Color(0xFFE2E8F0)
        )
    }

    // Spacing configuration (Layout Density)
    val spacing = when (density) {
        "COMPACT" -> Spacing(
            extraSmall = 2.dp,
            small = 4.dp,
            medium = 8.dp,
            large = 12.dp,
            extraLarge = 18.dp
        )
        "SPACIOUS" -> Spacing(
            extraSmall = 6.dp,
            small = 12.dp,
            medium = 20.dp,
            large = 32.dp,
            extraLarge = 48.dp
        )
        else -> Spacing() // NORMAL
    }

    // Font family mapping
    val fontFamily = when (fontStyle) {
        "MONOSPACE" -> FontFamily.Monospace
        "SANS" -> FontFamily.SansSerif
        "SERIF" -> FontFamily.Serif
        "GOOGLE_SANS" -> GoogleSansFamily
        "INTER" -> InterFamily
        else -> FontFamily.Default
    }

    val typography = getTypography(fontFamily)

    CompositionLocalProvider(LocalSpacing provides spacing) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}