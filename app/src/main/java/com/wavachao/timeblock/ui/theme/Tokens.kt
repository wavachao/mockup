package com.wavachao.timeblock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens lifted 1:1 from `ui/mockup.html`.
 *
 * The mockup is drawn at 390x844 logical pixels, so every CSS px maps directly to a
 * Compose dp, and every hex literal maps to `Color(0xFF...)`.
 */
@Immutable
data class TimeBlockPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundAlt: Color,
    val panel: Color,
    val panelStrong: Color,
    val panelStrongest: Color,
    val stroke: Color,
    val strokeStrong: Color,
    val hairline: Color,
    val text: Color,
    val textSecondary: Color,
    val muted: Color,
    val mutedDim: Color,
    val danger: Color,
)

internal val DarkPalette = TimeBlockPalette(
    isDark = true,
    background = Color(0xFF07080C),
    backgroundAlt = Color(0xFF0B0D13),
    panel = Color(0x0BFFFFFF),          // rgba(255,255,255,.045)
    panelStrong = Color(0x12FFFFFF),    // rgba(255,255,255,.07)
    panelStrongest = Color(0x1AFFFFFF), // rgba(255,255,255,.10)
    stroke = Color(0x16FFFFFF),         // rgba(255,255,255,.085)
    strokeStrong = Color(0x29FFFFFF),   // rgba(255,255,255,.16)
    hairline = Color(0x0EFFFFFF),       // rgba(255,255,255,.055)
    text = Color(0xFFF4F5FA),
    textSecondary = Color(0xFFA9AFC4),
    muted = Color(0xFF767D94),
    mutedDim = Color(0xFF565C70),
    danger = Color(0xFFFB7185),
)

internal val LightPalette = TimeBlockPalette(
    isDark = false,
    background = Color(0xFFF6F7FB),
    backgroundAlt = Color(0xFFFFFFFF),
    panel = Color(0xDBFFFFFF),          // rgba(255,255,255,.86)
    panelStrong = Color(0x0B111423),    // rgba(17,20,35,.045)
    panelStrongest = Color(0x13111423), // rgba(17,20,35,.075)
    stroke = Color(0x1311142D),         // rgba(17,20,45,.075)
    strokeStrong = Color(0x2111142D),   // rgba(17,20,45,.13)
    hairline = Color(0x1211142D),       // rgba(17,20,45,.07)
    text = Color(0xFF12142A),
    textSecondary = Color(0xFF4C5268),
    muted = Color(0xFF7A8095),
    mutedDim = Color(0xFF9BA0B4),
    danger = Color(0xFFFB7185),
)

/** Brand + category colours. Identical in both themes, exactly like the mockup. */
object BrandColors {
    val Primary = Color(0xFF6D5EF8)
    val Secondary = Color(0xFFA78BFA)
    val Cyan = Color(0xFF22D3EE)

    val Work = Color(0xFF397A6A)
    val Study = Color(0xFF577CAB)
    val Life = Color(0xFFA56E35)
    val Sport = Color(0xFFFBBF24)
    val Rest = Color(0xFFF472B6)

    val Danger = Color(0xFFFB7185)

    /** `linear-gradient(135deg,#6D5EF8 0%,#8B7BFF 45%,#A78BFA 100%)` */
    val brandGradient = Brush.linearGradient(
        colors = listOf(Primary, Color(0xFF8B7BFF), Secondary),
    )

    val ringGradient = Brush.linearGradient(
        colors = listOf(Primary, Secondary),
    )

    /** `rgba(109,94,248,.28) -> rgba(167,139,250,.16)` */
    val brandGradientSoft = Brush.linearGradient(
        colors = listOf(Primary.copy(alpha = 0.28f), Secondary.copy(alpha = 0.16f)),
    )

    val warmGradient = Brush.linearGradient(
        colors = listOf(Sport, Rest),
    )

    fun accent(isDark: Boolean): Color = if (isDark) Color(0xFFB9AEFF) else Color(0xFF5A48E0)
}

/**
 * The radius scale from the mockup: device 54 / card 26 / inner 18 / pill.
 */
object Radius {
    val card: Dp = 26.dp
    val inner: Dp = 18.dp
    val sheet: Dp = 34.dp
    val hero: Dp = 30.dp
    val chip: Dp = 13.dp
    val dateChip: Dp = 15.dp
    val tile: Dp = 22.dp
    val pill: Dp = 999.dp
}

/**
 * The timeline uses a fixed 46dp/hour row, which is what makes the mockup's absolute
 * offsets (`09:00 -> top:46px`) reproducible at runtime.
 */
object TimelineMetrics {
    val hourHeight: Dp = 96.dp
    val gutterWidth: Dp = 44.dp
    val blockInset: Dp = 58.dp
    val blockGap: Dp = 6.dp
}
