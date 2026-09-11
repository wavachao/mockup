package com.wavachao.timeblock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.ui.icons.IconSpec
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.LocalTimeBlockTokens
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.theme.TimelineMetrics

// ---------------------------------------------------------------------------- surfaces

/** The mockup's `.card`: translucent panel, hairline stroke, 26dp radius, soft shadow. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.card),
    brush: Brush? = null,
    fill: Color = AppTokens.palette.panel,
    border: Color = AppTokens.palette.stroke,
    elevated: Boolean = true,
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backgroundModifier = if (brush != null) {
        Modifier.background(brush, shape)
    } else {
        Modifier.background(fill, shape)
    }
    Column(
        modifier = modifier
            .then(if (elevated) Modifier.shadow(18.dp, shape, clip = false) else Modifier)
            .then(backgroundModifier)
            .border(BorderStroke(1.dp, border), shape)
            .clip(shape)
            .padding(contentPadding),
        content = content,
    )
}

/** Full-screen wash: the aurora gradient plus the two blurred glows of the mockup. */
@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    showGlowA: Boolean = true,
    showGlowB: Boolean = true,
) {
    val tokens = LocalTimeBlockTokens.current
    Box(modifier = modifier.fillMaxSize().background(tokens.screenBackground)) {
        if (showGlowA) {
            Box(
                Modifier
                    .offset(x = (-90).dp, y = (-180).dp)
                    .size(300.dp)
                    .background(tokens.glowA, CircleShape),
            )
        }
        if (showGlowB) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 110.dp, y = (-150).dp)
                    .size(260.dp)
                    .background(tokens.glowB, CircleShape),
            )
        }
    }
}

// ---------------------------------------------------------------------------- labels

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = AppTokens.palette.muted) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = AppTokens.type.eyebrow,
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTokens.palette.muted,
        style = AppTokens.type.label,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, color = AppTokens.palette.text, style = AppTokens.type.sectionTitle)
}

/** Digits that do not jump around while the clock ticks. */
@Composable
fun NumberText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTokens.type.body,
    color: Color = AppTokens.palette.text,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(fontFeatureSettings = "tnum"),
    )
}

// ---------------------------------------------------------------------------- controls

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Brush? = null,
    fill: Color = AppTokens.palette.panelStrongest,
    border: Color = Color.Transparent,
    contentColor: Color = AppTokens.palette.text,
    style: TextStyle = AppTokens.type.micro,
    horizontalPadding: Dp = 11.dp,
    verticalPadding: Dp = 5.dp,
) {
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier
            .then(if (background != null) Modifier.background(background, shape) else Modifier.background(fill, shape))
            .border(BorderStroke(1.dp, border), shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(text = text, color = contentColor, style = style)
    }
}

@Composable
fun CircleIconButton(
    icon: IconSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color = AppTokens.palette.textSecondary,
    fill: Color = AppTokens.palette.panel,
    border: Color = AppTokens.palette.stroke,
    showBadge: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(BorderStroke(1.dp, border), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(icon = icon, size = size * 0.46f, tint = tint)
        if (showBadge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(7.dp)
                    .background(AppTokens.palette.danger, CircleShape),
            )
        }
    }
}

/** The bottom-of-screen primary action (`创建时间段` / `加入今天`). */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: IconSpec? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(19.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) BrandColors.brandGradient else Brush.linearGradient(
                listOf(AppTokens.palette.panelStrongest, AppTokens.palette.panelStrongest),
            ))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            TimeBlockIcon(icon = icon, size = 19.dp, tint = Color.White, strokeWidth = 2.3f)
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = label,
            color = if (enabled) Color.White else AppTokens.palette.muted,
            style = AppTokens.type.bodyStrong.copy(fontSize = 16.sp, fontWeight = FontWeight(750)),
        )
    }
}

/** Selectable chip used for durations, dates and quick ranges. */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.chip),
    height: Dp = 38.dp,
    selectedContentColor: Color = Color.White,
) {
    val background = if (selected) {
        BrandColors.brandGradient
    } else {
        Brush.linearGradient(listOf(AppTokens.palette.panelStrong, AppTokens.palette.panelStrong))
    }
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(1.dp, if (selected) Color.Transparent else AppTokens.palette.stroke),
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) selectedContentColor else AppTokens.palette.textSecondary,
            style = AppTokens.type.caption.copy(fontWeight = FontWeight(650)),
            textAlign = TextAlign.Center,
        )
    }
}

/** Horizontal meter used by the summary card, the insights rows and the progress box. */
@Composable
fun MeterBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    brush: Brush = BrandColors.brandGradient,
    track: Color = AppTokens.palette.hairline,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Radius.pill))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(Radius.pill))
                .background(brush),
        )
    }
}

@Composable
fun CategoryDot(category: BlockCategory, size: Dp = 9.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(category.color, CircleShape),
    )
}

/** The three little load bars a calendar cell can show, as in the mockup. */
@Composable
fun DayLoadDots(
    categories: List<BlockCategory>,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Row(modifier = modifier.height(4.dp), verticalAlignment = Alignment.CenterVertically) {
        categories.take(3).forEachIndexed { index, category ->
            if (index > 0) Spacer(Modifier.width(2.5.dp))
            Box(
                Modifier
                    .width(8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (selected) Color.White.copy(alpha = 0.92f) else category.color),
            )
        }
    }
}

/** Colour swatch row from the editor's `分类` row. */
@Composable
fun CategorySwatches(
    selected: BlockCategory,
    onSelect: (BlockCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        BlockCategory.entries.forEach { category ->
            val isSelected = category == selected
            Box(contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .border(BorderStroke(1.8.dp, category.color.copy(alpha = 0.75f)), CircleShape),
                    )
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .background(category.color, CircleShape)
                        .clickable { onSelect(category) },
                )
            }
        }
    }
}

/** Timeline gutter label such as `08` / `09`. */
@Composable
fun HourLabel(hour: Int, modifier: Modifier = Modifier) {
    Text(
        text = hour.toString().padStart(2, '0'),
        modifier = modifier.width(TimelineMetrics.gutterWidth),
        color = AppTokens.palette.mutedDim,
        style = AppTokens.type.micro.copy(fontSize = 10.5.sp),
        textAlign = TextAlign.End,
    )
}

@Composable
fun rememberHourHeightPx(): Float = with(LocalDensity.current) { TimelineMetrics.hourHeight.toPx() }
