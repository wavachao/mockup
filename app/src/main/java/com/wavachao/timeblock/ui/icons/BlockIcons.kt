package com.wavachao.timeblock.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The mockup draws every glyph as a 1.9px round-capped stroke on a 24x24 grid.
 *
 * Instead of pulling in `material-icons-extended` (which adds megabytes for five
 * icons we would actually use), the path data from `ui/mockup.html` is parsed by Compose
 * with a single viewport transform for consistent stroke widths across screen densities.
 */
@Immutable
data class IconSpec(
    val pathData: List<String>,
    val filled: Boolean = false,
    val viewport: Float = 24f,
)

@Composable
fun TimeBlockIcon(
    icon: IconSpec,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color.White,
    strokeWidth: Float = 1.65f,
) {
    val paths = remember(icon) { icon.pathData.map { PathParser.parse(it) } }
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.minDimension / icon.viewport
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            paths.forEach { path ->
                if (icon.filled) {
                    drawPath(path, color = tint)
                } else {
                    drawPath(path, color = tint, style = stroke)
                }
            }
        }
    }
}

/** All glyphs used by the five screens, transcribed from the mockup. */
object BlockIcons {
    val Today = IconSpec(listOf("M8 3h8l4 4v13H4V3h4", "M8 9h8M8 13h8M8 17h5"))
    val Calendar = IconSpec(listOf("M3 8.5 a3.5 3.5 0 0 1 3.5 -3.5 h11 a3.5 3.5 0 0 1 3.5 3.5 v9 a3.5 3.5 0 0 1 -3.5 3.5 h-11 a3.5 3.5 0 0 1 -3.5 -3.5 z", "M8 3v4M16 3v4M3 10h18"))
    val Insights = IconSpec(listOf("M5 20V11M12 20V4M19 20v-6"))
    val Profile = IconSpec(listOf("M4 7h16M4 17h16", "M9 4v6M15 14v6"))
    val Search = IconSpec(listOf("M10.5 17a6.5 6.5 0 1 0 0 -13a6.5 6.5 0 0 0 0 13", "M15.5 15.5L21 21"))
    val Move = IconSpec(listOf("M4 5h10M4 10h7M4 15h5", "M13 14h8M17 10l4 4-4 4"))
    val Copy = IconSpec(listOf("M9 8h11v13H9z", "M15 8V3H4v13h5"))


    val Plus = IconSpec(listOf("M12 5v14M5 12h14"))
    val Minus = IconSpec(listOf("M5 12h14"))
    val Close = IconSpec(listOf("M6 6l12 12M18 6L6 18"))
    val Check = IconSpec(listOf("M5 12.5l4.5 4.5L19 7"))
    val ChevronRight = IconSpec(listOf("M9 5l7 7-7 7"))
    val ChevronLeft = IconSpec(listOf("M15 5l-7 7 7 7"))
    val ChevronDown = IconSpec(listOf("M6 9.5l6 6 6-6"))
    val ArrowRight = IconSpec(listOf("M4 12h15M14 6.5l5.5 5.5L14 17.5"))
    val MoreVertical = IconSpec(
        listOf("M6 12 a1.5 1.5 0 1 0 0.001 0 z", "M12 12 a1.5 1.5 0 1 0 0.001 0 z", "M18 12 a1.5 1.5 0 1 0 0.001 0 z"),
        filled = true,
    )
    val Edit = IconSpec(listOf("M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"))
    val Clock = IconSpec(listOf("M12 20.5 a8.5 8.5 0 1 0 0 -17 a8.5 8.5 0 0 0 0 17 z", "M12 7.5V12l3 2"))
    val Bell = IconSpec(listOf("M18 16.5V11a6 6 0 1 0 -12 0v5.5L4.4 19h15.2L18 16.5z", "M10 21.5h4"))
    val Repeat = IconSpec(listOf("M4 9V8a3 3 0 0 1 3 -3h10l-3-3M20 15v1a3 3 0 0 1 -3 3H7l3 3"))
    val Notes = IconSpec(listOf("M4 6h16M4 12h16M4 18h9"))
    val Sparkle = IconSpec(listOf("M12 3.2l2.3 4.7 5.2.8-3.8 3.6.9 5.2-4.6-2.5-4.6 2.5.9-5.2L4.5 8.7l5.2-.8z"))
    val Flame = IconSpec(listOf("M12 2.8c3.4 3.9 5.6 6.3 5.6 9.3a5.6 5.6 0 0 1 -11.2 0c0-1.6.7-2.9 1.9-4.2.4 1.5 1.2 2.3 2.2 2.5-.4-2.6.2-5 1.5-7.6z"))
    val Trend = IconSpec(listOf("M6 15l5-5 4 4 5-6"))

    // category glyphs
    val Briefcase = IconSpec(listOf("M4 20h16M6 20V9l6-4 6 4v11", "M10 20v-5h4v5"))
    val Book = IconSpec(listOf("M12 5C9 3 6 3 3 4v15c3-1 6-1 9 1c3-2 6-2 9-1V4c-3-1-6-1-9 1v15"))
    val Meal = IconSpec(listOf("M6 3v7a3 3 0 0 0 6 0V3M9 10v11M17 3c-1.4 1.6-2 3.2-2 5s.6 3.4 2 5v8"))
    val Dumbbell = IconSpec(listOf("M6.5 9v6M17.5 9v6M3 10.5v3M21 10.5v3M6.5 12h11"))
    val Moon = IconSpec(listOf("M20.4 14.6A8.6 8.6 0 0 1 9.4 3.6a8.6 8.6 0 1 0 11 11z"))
    val Code = IconSpec(listOf("M8 6l-5 6 5 6M16 6l5 6-5 6"))
    val Run = IconSpec(
        listOf("M13.6 5.2a1.6 1.6 0 1 0 0 -3.2a1.6 1.6 0 0 0 0 3.2z", "M8.4 21.5l2.2-5.6-2.8-2.4.9-5.4 3.6-1 3.4 3.1 3.1 1.3", "M8.7 13.5L4.5 15.6 3 20"),
    )
    val Trophy = IconSpec(listOf("M8 4h8v4.5a4 4 0 0 1 -8 0z", "M8 5.5H5.5a2.5 2.5 0 0 0 2.5 4M16 5.5h2.5a2.5 2.5 0 0 1 -2.5 4", "M12 12.5V16M9 19.5h6"))
}

/** Use Compose's SVG parser, including smooth curves and compact arc flags. */
object PathParser {
    fun parse(data: String): Path = androidx.compose.ui.graphics.vector.PathParser()
        .parsePathString(data).toPath()
}
