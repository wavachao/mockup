package com.wavachao.timeblock.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.TimeBlockDraft
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.util.TimeFormat
import java.time.LocalDate

/** Standard modal sheet handles back, accessibility focus and keyboard insets. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickAddSheet(
    visible: Boolean,
    dates: List<LocalDate>,
    draft: TimeBlockDraft,
    onDraftChange: (TimeBlockDraft) -> Unit,
    onSubmit: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AppTokens.palette
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.backgroundAlt,
        dragHandle = null,
    ) {
        val sheetShape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .clip(sheetShape)
                .background(palette.backgroundAlt)
                .border(1.dp, palette.strokeStrong, sheetShape)
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 30.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(palette.strokeStrong),
            )
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("快速添加时间段", color = palette.text, style = AppTokens.type.sectionTitle)
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.panelStrong)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    TimeBlockIcon(BlockIcons.Close, size = 15.dp, tint = palette.textSecondary, strokeWidth = 2.2f)
                }
            }
            Spacer(Modifier.height(16.dp))

            DateChips(dates = dates, selected = draft.date) { onDraftChange(draft.copy(date = it)) }
            Spacer(Modifier.height(14.dp))

            InputField(label = "做什么") {
                BasicTextField(
                    value = draft.title,
                    onValueChange = { onDraftChange(draft.copy(title = it)) },
                    singleLine = true,
                    textStyle = AppTokens.type.bodyStrong.copy(fontSize = 16.sp, color = palette.text),
                    cursorBrush = SolidColor(BrandColors.Secondary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (draft.title.isNotBlank()) onSubmit() }),
                    decorationBox = { inner ->
                        Box {
                            if (draft.title.isEmpty()) {
                                Text(
                                    text = "给这段时间起个名字…",
                                    color = palette.mutedDim,
                                    style = AppTokens.type.bodyStrong.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight(500),
                                    ),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))

            DurationField(draft = draft, onDraftChange = onDraftChange)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    label = "+30m",
                    selected = draft.durationMinutes == 30,
                    onClick = { onDraftChange(draft.withDuration(30)) },
                    modifier = Modifier.weight(1f),
                )
                SelectableChip(
                    label = "+1h",
                    selected = draft.durationMinutes == 60,
                    onClick = { onDraftChange(draft.withDuration(60)) },
                    modifier = Modifier.weight(1f),
                )
                SelectableChip(
                    label = "+2h",
                    selected = draft.durationMinutes == 120,
                    onClick = { onDraftChange(draft.withDuration(120)) },
                    modifier = Modifier.weight(1f),
                )
                SelectableChip(
                    label = "详细设置",
                    selected = false,
                    onClick = onExpand,
                    modifier = Modifier.weight(1.3f),
                )
            }
            Spacer(Modifier.height(14.dp))

            PrimaryButton(
                label = "创建时间段",
                icon = BlockIcons.Check,
                onClick = onSubmit,
                enabled = draft.title.isNotBlank(),
            )
        }
    }
}

@Composable
private fun DateChips(dates: List<LocalDate>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        dates.forEach { date ->
            val isSelected = date == selected
            val shape = RoundedCornerShape(Radius.dateChip)
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (isSelected) BrandColors.brandGradient else BrandColors.brandGradientSoft)
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else AppTokens.palette.stroke,
                        shape,
                    )
                    .clickable { onSelect(date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = TimeFormat.relativeDay(date),
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else AppTokens.palette.muted,
                    style = AppTokens.type.micro.copy(fontSize = 10.sp),
                )
                Spacer(Modifier.height(5.dp))
                NumberText(
                    text = TimeFormat.dayNumber(date),
                    style = AppTokens.type.sectionTitle.copy(fontSize = 17.sp, fontWeight = FontWeight(750)),
                    color = if (isSelected) Color.White else AppTokens.palette.text,
                )
            }
        }
    }
}

/** The `.field` wrapper from the mockup: label on top, content below in a soft well. */
@Composable
fun InputField(label: String, content: @Composable () -> Unit) {
    val palette = AppTokens.palette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.inner))
            .background(palette.panelStrong)
            .border(1.dp, palette.stroke, RoundedCornerShape(Radius.inner))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        FieldLabel(label)
        Spacer(Modifier.height(9.dp))
        content()
    }
}

@Composable
private fun DurationField(draft: TimeBlockDraft, onDraftChange: (TimeBlockDraft) -> Unit) {
    val palette = AppTokens.palette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.inner))
            .background(palette.panelStrong)
            .border(1.dp, palette.stroke, RoundedCornerShape(Radius.inner))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        FieldLabel("时间范围")
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                FieldLabel("开始")
                Spacer(Modifier.height(6.dp))
                NumberText(
                    text = TimeFormat.time(draft.startTime),
                    style = AppTokens.type.clock.copy(fontSize = 26.sp, fontWeight = FontWeight(750)),
                )
            }
            TimeBlockIcon(
                icon = BlockIcons.ArrowRight,
                size = 18.dp,
                tint = palette.mutedDim,
                strokeWidth = 2f,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                FieldLabel("结束")
                Spacer(Modifier.height(6.dp))
                NumberText(
                    text = TimeFormat.time(draft.endTime),
                    style = AppTokens.type.clock.copy(fontSize = 26.sp, fontWeight = FontWeight(750)),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(BrandColors.brandGradientSoft)
                .border(1.dp, Color(0x578C7CFF), RoundedCornerShape(Radius.pill))
                .padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeBlockIcon(BlockIcons.Clock, size = 13.dp, tint = BrandColors.accent(AppTokens.isDark))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "共 ${TimeFormat.longDuration(draft.durationMinutes)}",
                color = BrandColors.accent(AppTokens.isDark),
                style = AppTokens.type.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight(700)),
            )
        }
    }
}

/** The floating action button that opens the sheet. */
@Composable
fun QuickAddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .size(60.dp)
            .semantics { contentDescription = "新建时间段" }
            .clip(shape)
            .background(BrandColors.brandGradient)
            .border(1.dp, Color(0x24FFFFFF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(BlockIcons.Plus, size = 26.dp, tint = Color.White, strokeWidth = 2.4f)
    }
}
