package com.wavachao.timeblock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The mockup requests weights that Inter ships as variable instances (650, 750, 800).
 * Android maps these onto the closest available real face, so we keep the semantic
 * intent instead of rounding everything to 400/700.
 */
object Weights {
    val hero = FontWeight(800)
    val title = FontWeight(750)
    val body = FontWeight(650)
    val label = FontWeight(600)
    val micro = FontWeight(700)
    val microStrong = FontWeight(800)
}

@Immutable
data class TimeBlockType(
    /** 46sp page hero -> scaled down for a real phone screen. */
    val hero: TextStyle,
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val eyebrow: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val micro: TextStyle,
    /** 44sp metric used by the insights hero. */
    val metric: TextStyle,
    val metricUnit: TextStyle,
    /** 26sp wheel digits / time readouts. */
    val wheel: TextStyle,
    val wheelFocused: TextStyle,
    val clock: TextStyle,
)

internal val DefaultType = TimeBlockType(
    hero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(800),
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.1).sp,
    ),
    screenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(750),
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    sectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(750),
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
    ),
    eyebrow = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(600),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ),
    body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(650),
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyStrong = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(750),
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.3).sp,
    ),
    label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(700),
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp,
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(600),
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    micro = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(700),
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.4.sp,
    ),
    metric = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(800),
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-2.0).sp,
    ),
    metricUnit = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(700),
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    wheel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(650),
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = (-1.0).sp,
    ),
    wheelFocused = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(800),
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-1.0).sp,
    ),
    clock = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight(800),
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-1.0).sp,
    ),
)

val LocalTimeBlockType = staticCompositionLocalOf { DefaultType }
