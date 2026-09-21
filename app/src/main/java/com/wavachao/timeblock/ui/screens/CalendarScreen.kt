package com.wavachao.timeblock.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wavachao.timeblock.ui.CalendarUiState
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.icons.*
import java.time.LocalDate

@Composable
fun CalendarScreen(state: CalendarUiState, onSelectDate: (LocalDate) -> Unit, onStepMonth: (Long) -> Unit,
    onOpenBlock: (Long) -> Unit, onCreateForDate: (LocalDate) -> Unit, modifier: Modifier = Modifier,
    blocks: List<TimeBlock> = emptyList(), onToggle: ((TimeBlock) -> Unit)? = null,
    onEdit: ((Long) -> Unit)? = null, onMove: ((TimeBlock,LocalDate) -> Unit)? = null) {
    val context=LocalContext.current
    val colors=MaterialTheme.colorScheme
    val first=state.month.atDay(1)
    val start=first.minusDays((first.dayOfWeek.value-1).toLong())
    val rows=(first.dayOfWeek.value-1+state.month.lengthOfMonth()+6)/7
    var expanded by rememberSaveable {mutableStateOf(true)}
    val selected=blocks.filter {it.start<state.selectedDate.plusDays(1).atStartOfDay() && it.end>state.selectedDate.atStartOfDay()}.sortedBy {it.start}
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,16.dp,20.dp,20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text("日历",style=MaterialTheme.typography.headlineLarge);TextButton({onSelectDate(LocalDate.now())}) {Text("回到今天")}}}
        item {Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=colors.surface)) {
            Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    GlyphButton(BlockIcons.ChevronLeft,"上个月",{onStepMonth(-1)})
                    TextButton({val d=state.selectedDate;DatePickerDialog(context,{_,y,m,day->onSelectDate(LocalDate.of(y,m+1,day))},d.year,d.monthValue-1,d.dayOfMonth).show()},Modifier.weight(1f)) {Text("${state.month.year}年${state.month.monthValue}月",style=MaterialTheme.typography.titleMedium)}
                    GlyphButton(BlockIcons.ChevronRight,"下个月",{onStepMonth(1)})
                }
                Row(Modifier.padding(vertical=8.dp)) {listOf("一","二","三","四","五","六","日").forEach {Text(it,Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)}}
                val weekStart=state.selectedDate.minusDays((state.selectedDate.dayOfWeek.value-1).toLong())
                repeat(if(expanded) rows else 1) {week -> Row {
                    repeat(7) {day ->
                        val date=if(expanded) start.plusDays((week*7+day).toLong()) else weekStart.plusDays(day.toLong())
                        val hasEvent=blocks.any {it.start<date.plusDays(1).atStartOfDay() && it.end>date.atStartOfDay()}
                        val active=date==state.selectedDate
                        Column(Modifier.weight(1f).heightIn(min=46.dp).clip(RoundedCornerShape(12.dp))
                            .background(if(active) colors.primary else colors.surface)
                            .clickable {onSelectDate(date)}.semantics {contentDescription="${date}，${if(hasEvent) "有日程" else "无日程"}"}.padding(vertical=6.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text("${date.dayOfMonth}",style=MaterialTheme.typography.bodyMedium,color=if(active) colors.onPrimary else if(date.month==state.month.month) colors.onSurface else colors.outline)
                            Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(if(hasEvent) {if(active) colors.onPrimary else colors.primary} else androidx.compose.ui.graphics.Color.Transparent))
                        }
                    }
                }}
                TextButton({expanded=!expanded},Modifier.fillMaxWidth(),contentPadding=PaddingValues(0.dp)) {Text(if(expanded) "收起月历" else "展开月历",style=MaterialTheme.typography.labelSmall)}
            }
        }}
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {Text(friendlyDay(state.selectedDate),style=MaterialTheme.typography.titleMedium);Text("${selected.size} 项安排",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}
            TextButton({onCreateForDate(state.selectedDate)}) {Text("＋ 添加")}
        }}
        if(selected.isEmpty()) item {Text("这一天还没有安排，留给想做的事。",color=colors.onSurfaceVariant,modifier=Modifier.padding(vertical=12.dp))}
        items(selected,key={it.id}) {ScheduleCard(it,onOpenBlock,onToggle,onEdit,onMove)}
    }
}
