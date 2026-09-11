package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.RecurrenceRule
import com.wavachao.timeblock.data.model.ReminderLead
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.data.model.TimeBlockDraft
import com.wavachao.timeblock.ui.components.CategorySwatches
import com.wavachao.timeblock.ui.components.FieldLabel
import com.wavachao.timeblock.ui.components.PrimaryButton
import com.wavachao.timeblock.ui.components.SectionTitle
import com.wavachao.timeblock.ui.components.SelectableChip
import com.wavachao.timeblock.ui.components.SurfaceCard
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.IconSpec
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.util.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

private val DurationChoices = listOf(15, 30, 45, 60, 90, 120)

/** Reminder lead times offered by the picker, in minutes (0 = at start, -1 = off). */
private val ReminderOptions = listOf(-1, 0, 5, 10, 30, 60)

private fun reminderLabel(minutes: Int): String =
    if (minutes < 0) "不提醒" else ReminderLead.label(minutes)

private fun reminderIndex(minutes: Int): Int =
    ReminderOptions.indexOf(minutes).takeIf { it >= 0 } ?: ReminderOptions.indexOf(10)

/**
 * Screen 2 · 新建 / 编辑.
 *
 * The two scroll wheels are the centrepiece: they edit the start `LocalTime` directly and
 * the end time plus the duration readout are always derived from it, so the three values
 * can never disagree.
 */
@Composable
fun BlockEditorScreen(
    initial: TimeBlock?,
    dateHint: LocalDate,
    onSave: (TimeBlockDraft) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(),
) {
    var draft by remember(initial?.id, dateHint) {
        mutableStateOf(
            initial?.let { TimeBlockDraft.from(it) }
                ?: TimeBlockDraft.startingAt(dateHint.atTime(defaultStartTime(now)), minutes = 60),
        )
    }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showRecurrenceSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        EditorNavBar(
            isNew = initial == null,
            onBack = onBack,
            onSave = { onSave(draft) },
        )
        TitleInput(
            value = draft.title,
            onValueChange = { draft = draft.copy(title = it) },
        )
        TimeModule(draft = draft, onChange = { draft = it })
        SettingsList(
            draft = draft,
            onChange = { draft = it },
            onPickReminder = { showReminderSheet = true },
            onPickRecurrence = { showRecurrenceSheet = true },
        )
        SeamHint(draft = draft)

        if (initial != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(Radius.inner))
                    .border(1.dp, AppTokens.palette.stroke, RoundedCornerShape(Radius.inner))
                    .clickable { onDelete(initial.id) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "删除这个时间段",
                    color = AppTokens.palette.danger,
                    style = AppTokens.type.bodyStrong.copy(fontSize = 13.5.sp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            label = if (initial == null) "加入今天" else "保存修改",
            icon = if (initial == null) BlockIcons.Plus else BlockIcons.Check,
            onClick = { onSave(draft) },
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
        )
        Spacer(Modifier.height(40.dp))
    }

    if (showReminderSheet) {
        OptionSheet(
            title = "提醒",
            options = ReminderOptions.map { reminderLabel(it) },
            selectedIndex = reminderIndex(draft.reminderMinutes),
            onDismiss = { showReminderSheet = false },
            onSelect = { index ->
                draft = draft.copy(reminderMinutes = ReminderOptions[index])
                showReminderSheet = false
            },
        )
    }
    if (showRecurrenceSheet) {
        OptionSheet(
            title = "重复",
            options = RecurrenceRule.entries.map { it.displayName },
            selectedIndex = RecurrenceRule.entries.indexOf(draft.recurrence),
            onDismiss = { showRecurrenceSheet = false },
            onSelect = { index ->
                draft = draft.copy(recurrence = RecurrenceRule.entries[index])
                showRecurrenceSheet = false
            },
        )
    }
}

private fun defaultStartTime(now: LocalDateTime): java.time.LocalTime {
    val rounded = now.withSecond(0).withNano(0)
    val add = (15 - rounded.minute % 15) % 15
    return rounded.plusMinutes(add.toLong()).toLocalTime()
}

@Composable
private fun EditorNavBar(isNew: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    val palette = AppTokens.palette
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(palette.panel)
                .border(1.dp, palette.stroke, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            TimeBlockIcon(BlockIcons.Close, size = 17.dp, tint = palette.textSecondary, strokeWidth = 2.1f)
        }
        Text(
            text = if (isNew) "新建时间段" else "编辑时间段",
            color = palette.text,
            style = AppTokens.type.screenTitle,
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(BrandColors.brandGradientSoft)
                .border(1.dp, Color(0x668C7CFF), RoundedCornerShape(Radius.pill))
                .clickable(onClick = onSave)
                .padding(horizontal = 15.dp, vertical = 8.dp),
        ) {
            Text(
                text = "保存",
                color = BrandColors.accent(AppTokens.isDark),
                style = AppTokens.type.caption.copy(fontWeight = FontWeight(700)),
            )
        }
    }
}

@Composable
private fun TitleInput(value: String, onValueChange: (String) -> Unit) {
    val palette = AppTokens.palette
    val titleStyle = AppTokens.type.hero.copy(fontSize = 25.sp, lineHeight = 32.sp, color = palette.text)
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp)) {
        FieldLabel("标题")
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = titleStyle,
            cursorBrush = SolidColor(BrandColors.Secondary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "给这段时间起个名字…",
                            color = palette.mutedDim,
                            style = titleStyle.copy(fontWeight = FontWeight(500)),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(64.dp)
                .height(1.5.dp)
                .background(BrandColors.brandGradient),
        )
    }
}

@Composable
private fun TimeModule(draft: TimeBlockDraft, onChange: (TimeBlockDraft) -> Unit) {
    val palette = AppTokens.palette
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBlockIcon(BlockIcons.Clock, size = 13.dp, tint = palette.muted)
            Spacer(Modifier.width(7.dp))
            FieldLabel("开始时间")
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeWheel(
                value = draft.startTime.hour,
                range = 0..23,
                onValueChange = { hour -> onChange(draft.copy(startTime = draft.startTime.withHour(hour))) },
            )
            Text(
                ":",
                color = palette.muted,
                style = AppTokens.type.clock,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            TimeWheel(
                value = draft.startTime.minute,
                range = 0..59,
                onValueChange = { minute -> onChange(draft.copy(startTime = draft.startTime.withMinute(minute))) },
            )
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("时长")
                Text(
                    text = TimeFormat.compactDuration(draft.durationMinutes),
                    color = BrandColors.accent(AppTokens.isDark),
                    style = AppTokens.type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight(750)),
                )
                FieldLabel("结束")
                Text(
                    text = TimeFormat.time(draft.endTime),
                    color = palette.text,
                    style = AppTokens.type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight(750)),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.hairline))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationChoices.forEach { minutes ->
                SelectableChip(
                    label = if (minutes % 60 == 0) "${minutes / 60}h" else "${minutes}m",
                    selected = draft.durationMinutes == minutes,
                    onClick = { onChange(draft.withDuration(minutes)) },
                    modifier = Modifier.weight(1f),
                    height = 36.dp,
                )
            }
        }
    }
}

/**
 * A one-column scroll wheel. The selected row is the one crossing the highlight band, so
 * the component reports `firstVisibleItemIndex + 1` while snapping in single-row steps.
 */
@Composable
private fun TimeWheel(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    val palette = AppTokens.palette
    val items = remember(range) { range.toList() }
    val initialIndex = remember(range) { items.indexOf(value).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()

    val centeredIndex by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceIn(items.first(), items.last())
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }.collect { index ->
            if (index != value) onValueChange(index)
        }
    }

    Box(
        Modifier.size(width = 84.dp, height = 88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BrandColors.brandGradientSoft)
                .border(1.dp, Color(0x4D8C7CFF), RoundedCornerShape(14.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Spacer(Modifier.height(22.dp)) }
            items(count = items.size) { index ->
                val isSelected = items[index] == value
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable {
                            scope.launch { listState.animateScrollToItem((index - 1).coerceAtLeast(0)) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = items[index].toString().padStart(2, '0'),
                        color = if (isSelected) palette.text else palette.mutedDim,
                        style = if (isSelected) AppTokens.type.wheelFocused else AppTokens.type.wheel,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
        }
    }
}

@Composable
private fun SettingsList(
    draft: TimeBlockDraft,
    onChange: (TimeBlockDraft) -> Unit,
    onPickReminder: () -> Unit,
    onPickRecurrence: () -> Unit,
) {
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
    ) {
        SettingRow(
            icon = BlockIcons.Briefcase,
            label = "分类",
            trailing = {
                CategorySwatches(
                    selected = draft.category,
                    onSelect = { onChange(draft.copy(category = it)) },
                )
            },
        )
        SettingRow(
            icon = BlockIcons.Bell,
            label = "提醒",
            value = reminderLabel(draft.reminderMinutes),
            showChevron = true,
            onClick = onPickReminder,
        )
        SettingRow(
            icon = BlockIcons.Repeat,
            label = "重复",
            value = draft.recurrence.displayName,
            showChevron = true,
            onClick = onPickRecurrence,
        )
        SettingRow(
            icon = BlockIcons.Notes,
            label = "备注",
            value = draft.notes?.takeIf { it.isNotBlank() } ?: "可选",
            showChevron = true,
            onClick = { onChange(draft.copy(notes = draft.notes ?: "")) },
            showDivider = false,
        )
    }
}

@Composable
private fun SettingRow(
    icon: IconSpec,
    label: String,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = AppTokens.palette
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.panelStrongest),
                contentAlignment = Alignment.Center,
            ) {
                TimeBlockIcon(icon = icon, size = 16.dp, tint = palette.textSecondary)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                color = palette.text,
                style = AppTokens.type.body,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                trailing()
            } else if (value != null) {
                Text(
                    text = value,
                    color = palette.muted,
                    style = AppTokens.type.caption.copy(fontSize = 12.5.sp),
                )
                if (showChevron) {
                    Spacer(Modifier.width(7.dp))
                    TimeBlockIcon(
                        icon = BlockIcons.ChevronRight,
                        size = 14.dp,
                        tint = palette.mutedDim,
                        strokeWidth = 2f,
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 64.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.hairline),
            )
        }
    }
}

/** `无缝衔接 · 15:30 结束后…` — the tip strip under the settings list. */
@Composable
private fun SeamHint(draft: TimeBlockDraft) {
    val palette = AppTokens.palette
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0x2422D3EE), Color(0x1A6D5EF8))))
            .border(1.dp, Color(0x3D22D3EE), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0x2E22D3EE)),
            contentAlignment = Alignment.Center,
        ) {
            TimeBlockIcon(BlockIcons.Sparkle, size = 14.dp, tint = BrandColors.Cyan)
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = "无缝衔接 · 这块结束后，下一段时间从 ${TimeFormat.time(draft.endTime)} 起接上",
            color = palette.textSecondary,
            style = AppTokens.type.caption,
        )
    }
}

/** A small scrim + bottom card list used by the reminder / recurrence pickers. */
@Composable
private fun OptionSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val palette = AppTokens.palette
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x9903040A))
            .clickable(onClick = onDismiss),
    ) {
        SurfaceCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .pointerInput(Unit) { /* swallow taps so the card itself does not dismiss */ },
            shape = RoundedCornerShape(Radius.sheet),
            contentPadding = 18.dp,
        ) {
            SectionTitle(title)
            Spacer(Modifier.height(14.dp))
            options.forEachIndexed { index, label ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.inner))
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        color = if (index == selectedIndex) palette.text else palette.textSecondary,
                        style = AppTokens.type.body.copy(
                            fontWeight = if (index == selectedIndex) FontWeight(750) else FontWeight(650),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (index == selectedIndex) {
                        TimeBlockIcon(
                            BlockIcons.Check,
                            size = 16.dp,
                            tint = BrandColors.accent(AppTokens.isDark),
                            strokeWidth = 2.4f,
                        )
                    }
                }
            }
        }
    }
}
