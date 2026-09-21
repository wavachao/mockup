package com.wavachao.timeblock.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.icons.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun scheduleTime(block: TimeBlock): String = if (block.allDay) {
    val last = block.end.toLocalDate().minusDays(1)
    if (last == block.date) "全天" else "全天 · 至 $last"
} else block.start.format(DateTimeFormatter.ofPattern("HH:mm")) + " – " +
    block.end.format(DateTimeFormatter.ofPattern(if (block.date == block.end.toLocalDate()) "HH:mm" else "M月d日 HH:mm"))

fun friendlyDay(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val prefix = when(date) { today -> "今天"; today.plusDays(1) -> "明天"; today.minusDays(1) -> "昨天"; else -> date.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.CHINA)) }
    return "$prefix · ${date.format(DateTimeFormatter.ofPattern(if(date.year == today.year) "M月d日" else "yyyy年M月d日"))}"
}

@Composable
fun GlyphButton(icon: IconSpec, description: String, onClick: () -> Unit) {
    IconButton(onClick, Modifier.semantics { contentDescription = description }) {
        TimeBlockIcon(icon, size = 21.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ScheduleCard(block: TimeBlock, onOpen: (Long) -> Unit, onToggle: ((TimeBlock) -> Unit)? = null,
    onEdit: ((Long) -> Unit)? = null, onMove: ((TimeBlock, LocalDate) -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { onOpen(block.id) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
        Row(Modifier.fillMaxWidth().padding(start=14.dp, top=12.dp, end=4.dp, bottom=12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(block.category.color))
            Column(Modifier.weight(1f).padding(horizontal=12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(block.title, style = MaterialTheme.typography.titleMedium, textDecoration = if(block.done) TextDecoration.LineThrough else null, color = if(block.done) colors.onSurfaceVariant else colors.onSurface)
                Text(scheduleTime(block), style = MaterialTheme.typography.bodyMedium, color = colors.primary)
                Text(block.category.displayName + if(block.done) " · 已完成" else "", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                if(!block.notes.isNullOrBlank()) Text(block.notes, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if(onToggle != null) Checkbox(block.done, { onToggle(block) }, Modifier.semantics { contentDescription = if(block.done) "恢复未完成：${block.title}" else "完成：${block.title}" })
                if(onEdit != null) Box {
                    GlyphButton(BlockIcons.MoreVertical, "更多操作：${block.title}", { menu = true })
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("编辑日程") }, onClick = { menu = false; onEdit(block.id) })
                        if(onMove != null) {
                            DropdownMenuItem(text = { Text("移到明天") }, onClick = { menu=false; onMove(block, LocalDate.now().plusDays(1)) })
                            DropdownMenuItem(text = { Text("选择日期改期") }, onClick = {
                                menu=false
                                val d=block.date
                                DatePickerDialog(context, {_, y,m,day -> onMove(block, LocalDate.of(y,m+1,day))},d.year,d.monthValue-1,d.dayOfMonth).show()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaScreen(blocks: List<TimeBlock>, now: LocalDateTime, onOpen: (Long) -> Unit,
    onToggle: (TimeBlock) -> Unit, onEdit: (Long) -> Unit = {}, onMove: (TimeBlock,LocalDate) -> Unit = {_,_->}) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }
    val today=now.toLocalDate()
    val todayBlocks=blocks.filter { it.start < today.plusDays(1).atStartOfDay() && it.end > today.atStartOfDay() }
    val completed=todayBlocks.count { it.done }
    val shown=blocks.filter { b ->
        (when(filter) { 0 -> !b.done; 1 -> b in todayBlocks; 2 -> b.done; else -> true }) &&
            (b.title.contains(query,true) || b.notes.orEmpty().contains(query,true))
    }.let { if(filter==2) it.sortedByDescending { b -> b.start } else it.sortedBy { b -> b.start } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp,16.dp,20.dp,20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text(today.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", java.util.Locale.CHINA)), color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.labelLarge) }
        item { Text("我的日程", style=MaterialTheme.typography.headlineLarge) }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text("今天 ${todayBlocks.size} 项安排",style=MaterialTheme.typography.titleMedium)
                        Text("已完成 $completed",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress={if(todayBlocks.isEmpty()) 0f else completed.toFloat()/todayBlocks.size}, modifier=Modifier.fillMaxWidth().height(4.dp), color=MaterialTheme.colorScheme.primary, trackColor=MaterialTheme.colorScheme.surface.copy(alpha=.55f))
                }
            }
        }
        item {
            OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("搜索日程或备注")}, singleLine=true,
                leadingIcon={TimeBlockIcon(BlockIcons.Search,tint=MaterialTheme.colorScheme.onSurfaceVariant)},
                trailingIcon={if(query.isNotEmpty()) GlyphButton(BlockIcons.Close,"清空搜索",{query=""})},
                shape=RoundedCornerShape(14.dp), colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant,unfocusedContainerColor=MaterialTheme.colorScheme.surface))
        }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("待办","今天","已完成","全部").forEachIndexed { i,label ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if(filter==i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .selectable(selected=filter==i,role=Role.Tab,onClick={filter=i})
                    .heightIn(min=44.dp).padding(horizontal=4.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
                    Text(label,style=MaterialTheme.typography.labelLarge,color=if(filter==i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } }
        if(shown.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                TimeBlockIcon(BlockIcons.Calendar,size=42.dp,tint=MaterialTheme.colorScheme.primary)
                Text(if(query.isNotBlank()) "没有找到匹配的日程" else if(filter==2) "完成一项，就会出现在这里" else "给接下来的日子留点安排",style=MaterialTheme.typography.titleMedium)
                Text(if(query.isNotBlank()) "试试更短的关键词" else "点「新建日程」，记下第一件事",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
            }
        }
        shown.groupBy { it.date }.forEach { (date, group) ->
            item(key="date$date") { Text(friendlyDay(date,today) + if(filter==0 && date<today) " · 待处理" else "", Modifier.padding(top=8.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            items(group,key={it.id}) { ScheduleCard(it,onOpen,onToggle,onEdit,onMove) }
        }
    }
}
