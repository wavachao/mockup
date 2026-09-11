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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The mockup draws every glyph as a 1.9px round-capped stroke on a 24x24 grid.
 *
 * Instead of pulling in `material-icons-extended` (which adds megabytes for five
 * icons we would actually use), the exact path data from `ui/mockup.html` is parsed
 * at runtime by a tiny SVG path reader. Visual parity for free, zero dependencies.
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
    strokeWidth: Float = 1.9f,
) {
    val paths = remember(icon) { icon.pathData.map { PathParser.parse(it) } }
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.minDimension / icon.viewport
        val stroke = Stroke(
            width = strokeWidth * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        withTransform({
            // Slight inset so 1.9px round caps are not clipped at the edges.
            translate(1.4f * scale, 1.4f * scale)
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
    val Today = IconSpec(listOf("M4 6.5h16M4 12h16M4 17.5h10"))
    val Calendar = IconSpec(listOf("M3 8.5 a3.5 3.5 0 0 1 3.5 -3.5 h11 a3.5 3.5 0 0 1 3.5 3.5 v9 a3.5 3.5 0 0 1 -3.5 3.5 h-11 a3.5 3.5 0 0 1 -3.5 -3.5 z", "M8 3v4M16 3v4M3 10h18"))
    val Insights = IconSpec(listOf("M5 20V11M12 20V4M19 20v-6"))
    val Profile = IconSpec(listOf("M12 11.6 a3.6 3.6 0 1 0 0 -7.2 a3.6 3.6 0 0 0 0 7.2 z", "M4.6 20c1.2-3.6 4-5.4 7.4-5.4s6.2 1.8 7.4 5.4"))

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
    val Edit = IconSpec(listOf("M12 20h9M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z"))
    val Clock = IconSpec(listOf("M12 20.5 a8.5 8.5 0 1 0 0 -17 a8.5 8.5 0 0 0 0 17 z", "M12 7.5V12l3 2"))
    val Bell = IconSpec(listOf("M18 16.5V11a6 6 0 10-12 0v5.5L4.4 19h15.2L18 16.5z", "M10 21.5h4"))
    val Repeat = IconSpec(listOf("M4 9V8a3 3 0 013-3h10l-3-3M20 15v1a3 3 0 01-3 3H7l3 3"))
    val Notes = IconSpec(listOf("M4 6h16M4 12h16M4 18h9"))
    val Sparkle = IconSpec(listOf("M12 3.2l2.3 4.7 5.2.8-3.8 3.6.9 5.2-4.6-2.5-4.6 2.5.9-5.2L4.5 8.7l5.2-.8z"))
    val Flame = IconSpec(listOf("M12 2.8c3.4 3.9 5.6 6.3 5.6 9.3a5.6 5.6 0 01-11.2 0c0-1.6.7-2.9 1.9-4.2.4 1.5 1.2 2.3 2.2 2.5-.4-2.6.2-5 1.5-7.6z"))
    val Trend = IconSpec(listOf("M6 15l5-5 4 4 5-6"))

    // category glyphs
    val Briefcase = IconSpec(listOf("M4 20h16M6 20V9l6-4 6 4v11", "M10 20v-5h4v5"))
    val Book = IconSpec(listOf("M9 6 a3 3 0 1 0 0 6 a3 3 0 0 0 0 -6 z", "M16.5 7.5 a2.3 2.3 0 1 0 0 4.6 a2.3 2.3 0 0 0 0 -4.6 z", "M3.5 19c.7-2.6 2.9-4 5.5-4s4.8 1.4 5.5 4"))
    val Meal = IconSpec(listOf("M6 3v7a3 3 0 006 0V3M9 10v11M17 3c-1.4 1.6-2 3.2-2 5s.6 3.4 2 5v8"))
    val Dumbbell = IconSpec(listOf("M6.5 9v6M17.5 9v6M3 10.5v3M21 10.5v3M6.5 12h11"))
    val Moon = IconSpec(listOf("M20.4 14.6A8.6 8.6 0 019.4 3.6a8.6 8.6 0 1011 11z"))
    val Code = IconSpec(listOf("M8 6l-5 6 5 6M16 6l5 6-5 6"))
    val Run = IconSpec(
        listOf("M13.6 5.2a1.6 1.6 0 1 0 0 -3.2a1.6 1.6 0 0 0 0 3.2z", "M8.4 21.5l2.2-5.6-2.8-2.4.9-5.4 3.6-1 3.4 3.1 3.1 1.3", "M8.7 13.5L4.5 15.6 3 20"),
    )
    val Trophy = IconSpec(listOf("M8 4h8v4.5a4 4 0 01-8 0z", "M8 5.5H5.5a2.5 2.5 0 002.5 4M16 5.5h2.5a2.5 2.5 0 01-2.5 4", "M12 12.5V16M9 19.5h6"))
}

/**
 * Minimal SVG path reader supporting the subset used above: `M m L l H h V v C c
 * A a Z z` plus implicit repeats and every arc flag spelling (`a1.5 1.5 0 1 0` and
 * `a1.5,1.5,0,1,0`).
 */
object PathParser {
    fun parse(data: String): Path {
        val path = Path()
        val p = Cursor(data)
        var cur = Offset.Zero
        var start = Offset.Zero
        var command = ' '
        while (true) {
            p.skipSeparators()
            if (p.done) break
            val c = p.peek()
            command = if (c.isLetter()) {
                p.next()
                c
            } else {
                // implicit repeat: after M/m the repeated pairs are L/l
                when (command) {
                    'M' -> 'L'
                    'm' -> 'l'
                    else -> command
                }
            }
            when (command) {
                'M', 'm' -> {
                    val x = p.readNumber()
                    val y = p.readNumber()
                    cur = if (command == 'm') Offset(cur.x + x, cur.y + y) else Offset(x, y)
                    start = cur
                    path.moveTo(cur.x, cur.y)
                }
                'L', 'l' -> {
                    val x = p.readNumber()
                    val y = p.readNumber()
                    cur = if (command == 'l') Offset(cur.x + x, cur.y + y) else Offset(x, y)
                    path.lineTo(cur.x, cur.y)
                }
                'H', 'h' -> {
                    val x = p.readNumber()
                    cur = Offset(if (command == 'h') cur.x + x else x, cur.y)
                    path.lineTo(cur.x, cur.y)
                }
                'V', 'v' -> {
                    val y = p.readNumber()
                    cur = Offset(cur.x, if (command == 'v') cur.y + y else y)
                    path.lineTo(cur.x, cur.y)
                }
                'C', 'c' -> {
                    val x1 = p.readNumber()
                    val y1 = p.readNumber()
                    val x2 = p.readNumber()
                    val y2 = p.readNumber()
                    val x = p.readNumber()
                    val y = p.readNumber()
                    val base = if (command == 'c') cur else Offset.Zero
                    val c1 = Offset(base.x + x1, base.y + y1)
                    val c2 = Offset(base.x + x2, base.y + y2)
                    val end = Offset(base.x + x, base.y + y)
                    path.cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
                    cur = end
                }
                'A', 'a' -> {
                    val rx = p.readNumber()
                    val ry = p.readNumber()
                    val rot = p.readNumber()
                    val largeArc = p.readFlag()
                    val sweep = p.readFlag()
                    val x = p.readNumber()
                    val y = p.readNumber()
                    val end = if (command == 'a') Offset(cur.x + x, cur.y + y) else Offset(x, y)
                    arcTo(path, cur, end, rx, ry, rot, largeArc, sweep)
                    cur = end
                }
                'Z', 'z' -> {
                    path.close()
                    cur = start
                }
                else -> p.next()
            }
        }
        return path
    }

    private fun arcTo(
        path: Path,
        from: Offset,
        to: Offset,
        rxIn: Float,
        ryIn: Float,
        rotationDeg: Float,
        largeArc: Boolean,
        sweep: Boolean,
    ) {
        if (rxIn == 0f || ryIn == 0f || from == to) {
            path.lineTo(to.x, to.y)
            return
        }
        val phi = Math.toRadians(rotationDeg.toDouble())
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val rx = abs(rxIn)
        val ry = abs(ryIn)
        val dx = (from.x - to.x) / 2.0
        val dy = (from.y - to.y) / 2.0
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy
        val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        val scale = if (lambda > 1.0) sqrt(lambda) else 1.0
        val rxS = rx * scale
        val ryS = ry * scale
        val sign = if (largeArc != sweep) 1.0 else -1.0
        val numerator = rxS * rxS * ryS * ryS - rxS * rxS * y1p * y1p - ryS * ryS * x1p * x1p
        val denominator = rxS * rxS * y1p * y1p + ryS * ryS * x1p * x1p
        val coef = sign * sqrt((numerator / denominator).coerceAtLeast(0.0))
        val cxp = coef * (rxS * y1p / ryS)
        val cyp = coef * (-(ryS * x1p / rxS))
        val cx = cosPhi * cxp - sinPhi * cyp + (from.x + to.x) / 2.0
        val cy = sinPhi * cxp + cosPhi * cyp + (from.y + to.y) / 2.0

        val theta1 = angle(1.0, 0.0, (x1p - cxp) / rxS, (y1p - cyp) / ryS)
        var delta = angle(
            (x1p - cxp) / rxS,
            (y1p - cyp) / ryS,
            (-x1p - cxp) / rxS,
            (-y1p - cyp) / ryS,
        )
        if (!sweep && delta > 0) delta -= 2 * Math.PI
        if (sweep && delta < 0) delta += 2 * Math.PI

        // Cubic approximation: one segment per <=90 degrees.
        val segments = ceil(abs(delta) / (Math.PI / 2)).toInt().coerceAtLeast(1)
        val step = delta / segments
        var theta = theta1
        repeat(segments) {
            val nextTheta = theta + step
            val c1 = controlPoint(cx, cy, rxS, ryS, cosPhi, sinPhi, theta, step)
            val c2 = controlPoint(cx, cy, rxS, ryS, cosPhi, sinPhi, nextTheta, -step)
            val endPoint = pointOnArc(cx, cy, rxS, ryS, cosPhi, sinPhi, nextTheta)
            path.cubicTo(c1.x, c1.y, c2.x, c2.y, endPoint.x, endPoint.y)
            theta = nextTheta
        }
    }

    private fun pointOnArc(
        cx: Double,
        cy: Double,
        rx: Double,
        ry: Double,
        cosPhi: Double,
        sinPhi: Double,
        theta: Double,
    ): Offset {
        val x = rx * cos(theta)
        val y = ry * sin(theta)
        return Offset(
            (cosPhi * x - sinPhi * y + cx).toFloat(),
            (sinPhi * x + cosPhi * y + cy).toFloat(),
        )
    }

    private fun controlPoint(
        cx: Double,
        cy: Double,
        rx: Double,
        ry: Double,
        cosPhi: Double,
        sinPhi: Double,
        theta: Double,
        delta: Double,
    ): Offset {
        val alpha = 4.0 / 3.0 * kotlin.math.tan(delta / 4.0)
        val x = rx * (cos(theta) - alpha * sin(theta))
        val y = ry * (sin(theta) + alpha * cos(theta))
        return Offset(
            (cosPhi * x - sinPhi * y + cx).toFloat(),
            (sinPhi * x + cosPhi * y + cy).toFloat(),
        )
    }

    private fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        val dot = ux * vx + uy * vy
        val len = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        val a = kotlin.math.acos((dot / len).coerceIn(-1.0, 1.0))
        return if (ux * vy - uy * vx < 0) -a else a
    }

    private class Cursor(private val s: String) {
        var index = 0
        val done: Boolean get() = index >= s.length

        fun peek(): Char = s[index]
        fun next(): Char = s[index++]

        fun skipSeparators() {
            while (index < s.length) {
                val c = s[index]
                if (c == ' ' || c == ',' || c == '\n' || c == '\t' || c == '\r') index++ else break
            }
        }

        fun readNumber(): Float {
            skipSeparators()
            val startPos = index
            if (index < s.length && (s[index] == '-' || s[index] == '+')) index++
            while (index < s.length && (s[index].isDigit() || s[index] == '.')) index++
            if (index < s.length && (s[index] == 'e' || s[index] == 'E')) {
                index++
                if (index < s.length && (s[index] == '-' || s[index] == '+')) index++
                while (index < s.length && s[index].isDigit()) index++
            }
            if (index == startPos) {
                index++
                return 0f
            }
            return s.substring(startPos, index).toFloatOrNull() ?: 0f
        }

        fun readFlag(): Boolean {
            skipSeparators()
            val c = if (index < s.length) s[index] else '0'
            index++
            return c == '1'
        }
    }
}
