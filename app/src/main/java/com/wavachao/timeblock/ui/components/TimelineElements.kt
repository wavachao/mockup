package com.wavachao.timeblock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.util.TimeFormat

/** The progress ring of the summary card: `63%` with a gradient arc. */
@Composable
fun ProgressRing(
    percent: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 76.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 7.dp,
    label: String,
    caption: String,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            drawArc(
                color = Color.White.copy(alpha = 0.055f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - stroke,
                    this.size.height - stroke,
                ),
            )
            drawArc(
                brush = BrandColors.ringGradient,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0f, 1f),
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - stroke,
                    this.size.height - stroke,
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NumberText(
                text = label,
                style = AppTokens.type.sectionTitle.copy(fontSize = 19.sp, fontWeight = FontWeight(800)),
            )
            Spacer(Modifier.height(4.dp))
            Text(caption, color = AppTokens.palette.muted, style = AppTokens.type.micro.copy(fontSize = 9.5.sp))
        }
    }
}

/**
 * One block on the 24h timeline. Mirrors `.blk` in the mockup: category tinted glass,
 * a 3dp accent rail, the emoji tile, the title, and a tappable completion ring.
 */
@Composable
fun TimelineBlockCard(
    block: TimeBlock,
    live: Boolean,
    /** `还有 20 分钟` / `已进行 34 分钟` / the duration once finished. */
    timingLabel: String,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = block.category.color
    val shape = RoundedCornerShape(Radius.inner)
    val background = Brush.linearGradient(
        listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.09f)),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (block.done) 0.46f else 1f)
            .clip(shape)
            .background(background)
            .border(1.dp, accent.copy(alpha = 0.34f), shape)
            .clickable(onClick = onClick)
            .padding(start = 15.dp, end = 13.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // accent rail
        Box(
            Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.26f))
                .border(1.dp, accent.copy(alpha = 0.36f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            TimeBlockIcon(icon = block.category.icon, size = 15.dp, tint = accent)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = block.title,
                    color = AppTokens.palette.text,
                    style = AppTokens.type.body.copy(
                        fontWeight = FontWeight(650),
                        textDecoration = if (block.done) TextDecoration.LineThrough else null,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (live && !block.done) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "进行中",
                        color = accent,
                        style = AppTokens.type.micro.copy(fontSize = 9.5.sp, fontWeight = FontWeight(800)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(accent.copy(alpha = 0.20f))
                            .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(Radius.pill))
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberText(
                    text = TimeFormat.range(block),
                    style = AppTokens.type.caption.copy(fontSize = 11.sp),
                    color = AppTokens.palette.textSecondary,
                )
                Text(" · ", color = AppTokens.palette.textSecondary, style = AppTokens.type.caption.copy(fontSize = 11.sp))
                NumberText(
                    text = timingLabel,
                    style = AppTokens.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight(700)),
                    color = accent,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        CompletionRing(done = block.done, accent = accent, onClick = onToggleDone)
    }
}

/** The circular check on a timeline block. */
@Composable
fun CompletionRing(
    done: Boolean,
    accent: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 22.dp,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (done) accent else Color.Transparent)
            .border(1.6.dp, accent.copy(alpha = if (done) 1f else 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(
            icon = BlockIcons.Check,
            size = size * 0.54f,
            tint = if (done) Color.White else Color.Transparent,
            strokeWidth = 2.6f,
        )
    }
}

/** The dashed `空档 30 分钟` hint drawn between two blocks. */
@Composable
fun GapHint(label: String, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 24.dp) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                AppTokens.palette.strokeStrong,
                RoundedCornerShape(14.dp),
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeBlockIcon(
            icon = BlockIcons.Plus,
            size = 11.dp,
            tint = AppTokens.palette.muted,
            strokeWidth = 2.2f,
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = AppTokens.palette.muted, style = AppTokens.type.micro.copy(fontSize = 10.5.sp, fontWeight = FontWeight(600)))
    }
}

/** The live red line with its pulsing dot and time pill. */
@Composable
fun NowLine(clockLabel: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(12.dp)) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(11.dp)
                .background(AppTokens.palette.danger, CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.6.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(AppTokens.palette.danger.copy(alpha = 0.15f), AppTokens.palette.danger),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Brush.linearGradient(listOf(Color(0xFFFB7185), Color(0xFFF472B6))))
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            NumberText(
                text = clockLabel,
                style = AppTokens.type.micro.copy(fontSize = 10.5.sp, fontWeight = FontWeight(700)),
                color = Color.White,
            )
        }
    }
}
