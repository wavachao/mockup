package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.icons.*
import java.time.LocalDateTime

@Composable
fun BlockDetailScreen(block: TimeBlock?, now: LocalDateTime, onBack: () -> Unit, onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit, onToggleDone: (TimeBlock) -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    var deleting by remember {mutableStateOf(false)}
    val colors=MaterialTheme.colorScheme
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            GlyphButton(BlockIcons.ChevronLeft,"返回",onBack)
            Text("日程详情",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
            if(block!=null) TextButton({deleting=true}) {Text("删除日程",color=colors.error)}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            if(block==null) Text(if(loading) "正在读取日程…" else "日程不存在或已被删除") else {
                Surface(shape=RoundedCornerShape(10.dp),color=block.category.color.copy(alpha=.1f)) {
                    Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {TimeBlockIcon(block.category.icon,tint=block.category.color,size=18.dp);Text(block.category.displayName,style=MaterialTheme.typography.labelLarge,color=colors.onSurface)}
                }
                Text(block.title,style=MaterialTheme.typography.headlineLarge)
                Text(if(block.done) "已完成" else if(block.end<now) "已过期 · 尚未完成" else "待完成",color=colors.onSurfaceVariant)
                Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=colors.surface)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {TimeBlockIcon(BlockIcons.Calendar,tint=colors.primary);Text(friendlyDay(block.date),style=MaterialTheme.typography.titleMedium)}
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {TimeBlockIcon(BlockIcons.Clock,tint=colors.primary);Text(scheduleTime(block),style=MaterialTheme.typography.titleMedium)}
                        HorizontalDivider(color=colors.outlineVariant)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {TimeBlockIcon(BlockIcons.Bell,tint=colors.onSurfaceVariant);Text(if(block.reminderMinutes<0) "不提醒" else if(block.allDay) {if(block.reminderMinutes==0) "当天上午 9 点提醒" else "前一天上午 9 点提醒"} else if(block.reminderMinutes==0) "开始时提醒" else "开始前 ${block.reminderMinutes} 分钟提醒",color=colors.onSurfaceVariant)}
                    }
                }
                if(!block.notes.isNullOrBlank()) {
                    Text("备注 / 地点",style=MaterialTheme.typography.labelLarge,color=colors.onSurfaceVariant)
                    Text(block.notes,style=MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if(block!=null) Column(Modifier.padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Button({onEdit(block.id)},Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)) {Text("编辑日程")}
            OutlinedButton({onToggleDone(block)},Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)) {Text(if(block.done) "标记为未完成" else "标记为已完成")}
        }
    }
    if(deleting && block!=null) AlertDialog(onDismissRequest={deleting=false},title={Text("删除这条日程？")},confirmButton={TextButton({deleting=false;onDelete(block.id)}) {Text("确认删除")}},dismissButton={TextButton({deleting=false}) {Text("取消")}})
}
