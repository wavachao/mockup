package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.components.CircleIconButton
import com.wavachao.timeblock.ui.components.NumberText
import com.wavachao.timeblock.ui.components.Pill
import com.wavachao.timeblock.ui.components.SurfaceCard
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.IconSpec
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.util.TimeFormat
import java.time.LocalDateTime

/** One row of the detail screen's checkpoint list. */
private data class Checkpoint(
    val title: String,
    val time: String,
    val detail: String,
    val done: Boolean,
)

/** Screen ⑤ 时间段详情 — hero, progress, checkpoints and the two actions. */
@Composable
fun BlockDetailScreen(
    block: TimeBlock?,
    now: LocalDateTime,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleDone: (TimeBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = block
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (loaded == null) {
            BlockEmptyState()
        } else {
            BlockDetailContent(
                block = loaded,
                now = now,
                onBack = onBack,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleDone = onToggleDone,
            )
        }
    }
}

/** The only thing a null selection can say; the repository has not answered yet. */
@Composable
private fun BlockEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "正在读取时间段…",
            style = AppTokens.type.caption,
            color = AppTokens.palette.muted,
        )
    }
}

@Composable
private fun BlockDetailContent(
    block: TimeBlock,
    now: LocalDateTime,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleDone: (TimeBlock) -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }

    DetailNavBar(
        confirming = confirming,
        onBack = onBack,
        onMore = {
            if (confirming) {
                onDelete(block.id)
            } else {
                confirming = true
            }
        },
    )
    DetailHeroCard(block = block, now = now)
    DetailProgressCard(block = block, now = now)
    SubTaskTimeline(block = block, now = now, modifier = Modifier.padding(horizontal = 18.dp))
    DetailActionRow(
        block = block,
        onEdit = onEdit,
        onToggleDone = onToggleDone,
    )
    Spacer(Modifier.height(60.dp))
}

// ---------------------------------------------------------------------------- nav bar

@Composable
private fun DetailNavBar(
    confirming: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleIconButton(
                icon = BlockIcons.ChevronLeft,
                onClick = onBack,
                size = 38.dp,
            )
            Text(
                text = "时间段详情",
                style = AppTokens.type.screenTitle,
                color = AppTokens.palette.text,
            )
            CircleIconButton(
                icon = BlockIcons.MoreVertical,
                onClick = onMore,
                size = 38.dp,
                tint = if (confirming) AppTokens.palette.danger else AppTokens.palette.textSecondary,
            )
        }
        if (confirming) {
            Text(
                text = "再次点击删除",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp, end = 20.dp, bottom = 6.dp),
                style = AppTokens.type.caption,
                color = AppTokens.palette.danger,
            )
        }
    }
}

// ---------------------------------------------------------------------------- hero

@Composable
private fun DetailHeroCard(block: TimeBlock, now: LocalDateTime) {
    SurfaceCard(
        modifier = Modifier.padding(horizontal = 18.dp),
        shape = RoundedCornerShape(Radius.hero),
        brush = Brush.linearGradient(
            listOf(
                block.category.color.copy(alpha = 0.34f),
                block.category.color.copy(alpha = 0.09f),
                Color.Transparent,
            ),
        ),
        border = block.category.color.copy(alpha = 0.30f),
        contentPadding = 22.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            HeroArc(
                color = block.category.color,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(Modifier.fillMaxWidth()) {
                Pill(
                    text = "${block.category.displayName} · ${TimeFormat.statusLabel(block, now)}",
                    fill = block.category.color.copy(alpha = 0.16f),
                    border = block.category.color.copy(alpha = 0.34f),
                    contentColor = block.category.color,
                    style = AppTokens.type.micro,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = block.title,
                    modifier = Modifier.alpha(if (block.done) 0.55f else 1f),
                    style = AppTokens.type.hero.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight(800),
                        letterSpacing = (-1).sp,
                        lineHeight = 32.sp,
                        textDecoration = if (block.done) TextDecoration.LineThrough else null,
                    ),
                    color = AppTokens.palette.text,
                )
                Spacer(Modifier.height(16.dp))
                DetailTimeRangeRow(block = block)
            }
        }
    }
}

/** The mockup's two concentric rings, cropped by the card's top-right corner. */
@Composable
private fun HeroArc(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(150.dp)) {
        val ringColor = color.copy(alpha = 0.22f)
        val dash = floatArrayOf(8f, 14f)
        val available = size.minDimension
        val center = Offset(size.width * 0.62f, size.height * 0.38f)
        val outerDiameter = available * 0.98f
        drawArc(
            color = ringColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(
                center.x - outerDiameter / 2f,
                center.y - outerDiameter / 2f,
            ),
            size = Size(outerDiameter, outerDiameter),
            style = Stroke(
                width = 1.4f,
                pathEffect = PathEffect.dashPathEffect(dash),
            ),
        )
        val innerDiameter = outerDiameter * 0.70f
        drawArc(
            color = ringColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(
                center.x - innerDiameter / 2f,
                center.y - innerDiameter / 2f,
            ),
            size = Size(innerDiameter, innerDiameter),
            style = Stroke(width = 1.4f),
        )
    }
}

@Composable
private fun DetailTimeRangeRow(block: TimeBlock) {
    val rangeStyle = AppTokens.type.hero.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight(800),
        letterSpacing = (-0.8).sp,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberText(
            text = TimeFormat.time(block.start),
            style = rangeStyle,
            color = AppTokens.palette.text,
        )
        Spacer(Modifier.width(10.dp))
        RangeArrow(
            color = block.category.color.copy(alpha = 0.7f),
            modifier = Modifier.size(width = 20.dp, height = 12.dp),
        )
        Spacer(Modifier.width(10.dp))
        NumberText(
            text = TimeFormat.time(block.end),
            style = rangeStyle,
            color = AppTokens.palette.text,
        )
        Spacer(Modifier.width(10.dp))
        Pill(
            text = TimeFormat.duration(block.durationMinutes),
            fill = Color(0x38000000),
            contentColor = AppTokens.palette.textSecondary,
        )
    }
}

/** A 20x12 arrow drawn by hand so it can carry the category colour. */
@Composable
private fun RangeArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val mid = size.height / 2f
        val headX = size.width - 5f
        val stroke = 1.6f
        drawLine(
            color = color,
            start = Offset(0f, mid),
            end = Offset(headX, mid),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(headX - 4.5f, mid - 4.5f),
            end = Offset(headX, mid),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(headX, mid),
            end = Offset(headX - 4.5f, mid + 4.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------------------- progress

@Composable
private fun DetailProgressCard(block: TimeBlock, now: LocalDateTime) {
    val total = block.durationMinutes.coerceAtLeast(1)
    val clampedNow = if (now.isAfter(block.end)) block.end else now
    val elapsed = TimeFormat.minutesBetween(block.start, clampedNow)
    val progress = if (block.done) {
        1f
    } else {
        (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
    val remaining = total - elapsed
    val remainingLabel = when {
        block.done -> "已完成"
        remaining >= 0 -> "剩余 ${TimeFormat.longDuration(remaining)}"
        else -> "已超时 ${TimeFormat.longDuration(-remaining)}"
    }

    SurfaceCard(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        shape = RoundedCornerShape(Radius.card),
        contentPadding = 20.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "已进行 ${TimeFormat.percent(progress)}",
                style = AppTokens.type.bodyStrong,
                color = AppTokens.palette.text,
            )
            NumberText(
                text = remainingLabel,
                style = AppTokens.type.caption,
                color = AppTokens.palette.muted,
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(
            progress = progress,
            brush = Brush.linearGradient(
                listOf(block.category.color, block.category.color.copy(alpha = 0.55f)),
            ),
        )
        Spacer(Modifier.height(10.dp))
        val markStyle = AppTokens.type.micro.copy(fontSize = 10.5.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = TimeFormat.time(block.start),
                style = markStyle,
                color = AppTokens.palette.mutedDim,
            )
            Text(
                text = TimeFormat.time(block.start.plusMinutes((total / 2).toLong())),
                style = markStyle,
                color = AppTokens.palette.mutedDim,
            )
            Text(
                text = TimeFormat.time(block.end),
                style = markStyle,
                color = AppTokens.palette.mutedDim,
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, brush: Brush) {
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(shape)
            .background(AppTokens.palette.hairline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(brush),
        )
    }
}

// ---------------------------------------------------------------------------- checkpoints

/** The block's three derived checkpoints, drawn as a vertical timeline. */
@Composable
fun SubTaskTimeline(
    block: TimeBlock,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val total = block.durationMinutes.coerceAtLeast(1)
    val clampedNow = if (now.isAfter(block.end)) block.end else now
    val elapsed = TimeFormat.minutesBetween(block.start, clampedNow)
    val progress = if (block.done) {
        1f
    } else {
        (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    val checkpoints = listOf(
        Checkpoint(
            title = "已开始",
            time = TimeFormat.time(block.start),
            detail = "自动记录",
            done = !now.isBefore(block.start),
        ),
        Checkpoint(
            title = "进行中",
            time = TimeFormat.time(block.start.plusMinutes((total / 2).toLong())),
            detail = "已过半程",
            done = progress >= 0.5f,
        ),
        Checkpoint(
            title = "完成",
            time = TimeFormat.time(block.end),
            detail = "计划中 · 预计 ${TimeFormat.longDuration(block.durationMinutes)}",
            done = block.done,
        ),
    )

    SurfaceCard(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.card),
        contentPadding = 18.dp,
    ) {
        checkpoints.forEachIndexed { index, checkpoint ->
            CheckpointRow(
                checkpoint = checkpoint,
                color = block.category.color,
                showConnector = index != checkpoints.lastIndex,
            )
        }
    }
}

@Composable
private fun CheckpointRow(
    checkpoint: Checkpoint,
    color: Color,
    showConnector: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier.width(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(
                        if (checkpoint.done) color else AppTokens.palette.backgroundAlt,
                    )
                    .border(
                        2.dp,
                        if (checkpoint.done) color else AppTokens.palette.mutedDim,
                        CircleShape,
                    ),
            )
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .weight(1f, fill = true)
                        .background(AppTokens.palette.hairline),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.heightIn(min = 52.dp)) {
            Text(
                text = checkpoint.title,
                style = AppTokens.type.body,
                color = AppTokens.palette.text,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${checkpoint.time} · ${checkpoint.detail}",
                style = AppTokens.type.caption,
                color = AppTokens.palette.muted,
            )
        }
    }
}

// ---------------------------------------------------------------------------- actions

@Composable
private fun DetailActionRow(
    block: TimeBlock,
    onEdit: (Long) -> Unit,
    onToggleDone: (TimeBlock) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        DetailActionButton(
            label = "编辑",
            icon = BlockIcons.Edit,
            primary = false,
            onClick = { onEdit(block.id) },
            modifier = Modifier.weight(1f),
        )
        DetailActionButton(
            label = if (block.done) "标记未完成" else "提前完成",
            icon = if (block.done) BlockIcons.Repeat else BlockIcons.Check,
            primary = true,
            onClick = { onToggleDone(block) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    icon: IconSpec,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val contentColor = if (primary) Color.White else AppTokens.palette.textSecondary
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (primary) BrandColors.brandGradient else Brush.linearGradient(listOf(AppTokens.palette.panelStrong, AppTokens.palette.panelStrong)))
            .border(
                BorderStroke(1.dp, if (primary) Color.Transparent else AppTokens.palette.stroke),
                shape,
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeBlockIcon(
            icon = icon,
            modifier = Modifier.size(17.dp),
            tint = contentColor,
            strokeWidth = 1.9f,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = AppTokens.type.caption.copy(fontWeight = FontWeight(700)),
            color = contentColor,
        )
    }
}
