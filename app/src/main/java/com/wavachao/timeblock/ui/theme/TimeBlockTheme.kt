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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
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

private val DarkMaterialScheme = darkColorScheme(
    primary = Color(0xFFB9ADFF),
    onPrimary = Color(0xFF251650),
    secondary = BrandColors.Secondary,
    background = DarkPalette.background,
    onBackground = DarkPalette.text,
    surface = DarkPalette.backgroundAlt,
    onSurface = DarkPalette.text,
    surfaceVariant = Color(0xFF292C36),
    onSurfaceVariant = DarkPalette.textSecondary,
    error = DarkPalette.danger,
)

private val LightMaterialScheme = lightColorScheme(
    primary = Color(0xFF176B60), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEEE8), onPrimaryContainer = Color(0xFF154E46),
    secondary = Color(0xFF667E73), secondaryContainer = Color(0xFFE5EEE8),
    background = Color(0xFFF5F6F2), onBackground = Color(0xFF202D29),
    surface = Color.White, onSurface = Color(0xFF202D29),
    surfaceVariant = Color(0xFFEDF1EC), onSurfaceVariant = Color(0xFF68756F),
    surfaceContainer = Color(0xFFF0F3EE), surfaceContainerHigh = Color(0xFFEDF1EC),
    outline = Color(0xFF89968F), outlineVariant = Color(0xFFE1E7E1),
    error = Color(0xFFB44940),
)

private val MaterialTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 23.sp),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
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
