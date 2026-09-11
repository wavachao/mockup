package com.wavachao.timeblock.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Everything a screen needs to paint: palette, type scale and the aurora glows. */
@Immutable
data class TimeBlockTokens(
    val palette: TimeBlockPalette,
    val type: TimeBlockType,
    val glowA: Brush,
    val glowB: Brush,
    val screenBackground: Brush,
) {
    val isDark: Boolean get() = palette.isDark
}

private val DarkGlowA = Brush.radialGradient(listOf(Color(0x6B6D5EF8), Color(0x006D5EF8)))
private val DarkGlowB = Brush.radialGradient(listOf(Color(0x3322D3EE), Color(0x0022D3EE)))
private val LightGlowA = Brush.radialGradient(listOf(Color(0x336D5EF8), Color(0x006D5EF8)))
private val LightGlowB = Brush.radialGradient(listOf(Color(0x2422D3EE), Color(0x0022D3EE)))

private val DarkScreenBackground = Brush.radialGradient(
    colors = listOf(Color(0xFF1A1B2E), Color(0xFF0A0B11), Color(0xFF07080C)),
    radius = 1400f,
)
private val LightScreenBackground = Brush.radialGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF4F5FB), Color(0xFFEDEFF7)),
    radius = 1400f,
)

private fun tokensFor(palette: TimeBlockPalette): TimeBlockTokens = TimeBlockTokens(
    palette = palette,
    type = DefaultType,
    glowA = if (palette.isDark) DarkGlowA else LightGlowA,
    glowB = if (palette.isDark) DarkGlowB else LightGlowB,
    screenBackground = if (palette.isDark) DarkScreenBackground else LightScreenBackground,
)

val LocalTimeBlockTokens = staticCompositionLocalOf { tokensFor(DarkPalette) }

/** Shorthand used throughout the UI: `AppTokens.palette.text`, `AppTokens.type.body`. */
object AppTokens {
    val palette: TimeBlockPalette
        @Composable get() = LocalTimeBlockTokens.current.palette

    val type: TimeBlockType
        @Composable get() = LocalTimeBlockTokens.current.type

    val isDark: Boolean
        @Composable get() = LocalTimeBlockTokens.current.isDark
}

/**
 * The prototype is dark-first; the light palette only exists in the mockup for
 * evaluation. Flip this to preview the light design.
 */
const val USE_LIGHT_PALETTE = false

private val DarkMaterialScheme = darkColorScheme(
    primary = BrandColors.Primary,
    onPrimary = Color.White,
    secondary = BrandColors.Secondary,
    background = DarkPalette.background,
    onBackground = DarkPalette.text,
    surface = DarkPalette.backgroundAlt,
    onSurface = DarkPalette.text,
    surfaceVariant = DarkPalette.panelStrong,
    onSurfaceVariant = DarkPalette.textSecondary,
    error = DarkPalette.danger,
)

private val LightMaterialScheme = lightColorScheme(
    primary = BrandColors.Primary,
    onPrimary = Color.White,
    secondary = BrandColors.Secondary,
    background = LightPalette.background,
    onBackground = LightPalette.text,
    surface = LightPalette.backgroundAlt,
    onSurface = LightPalette.text,
    surfaceVariant = LightPalette.panelStrong,
    onSurfaceVariant = LightPalette.textSecondary,
    error = LightPalette.danger,
)

private val MaterialTypography = Typography(
    headlineLarge = DefaultType.hero,
    titleMedium = DefaultType.sectionTitle,
    bodyMedium = DefaultType.body,
    labelSmall = DefaultType.micro,
)

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun TimeBlockTheme(
    darkTheme: Boolean = if (USE_LIGHT_PALETTE) false else isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The mockup is a dark design: it stays dark unless the user asks otherwise.
    val resolvedDark = darkTheme
    val palette = if (resolvedDark) DarkPalette else LightPalette
    val tokens = remember(resolvedDark) { tokensFor(palette) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !resolvedDark
                isAppearanceLightNavigationBars = !resolvedDark
            }
        }
    }

    CompositionLocalProvider(LocalTimeBlockTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (resolvedDark) DarkMaterialScheme else LightMaterialScheme,
            typography = MaterialTypography,
            content = content,
        )
    }
}
