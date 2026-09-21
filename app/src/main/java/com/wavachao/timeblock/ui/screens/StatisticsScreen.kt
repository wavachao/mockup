package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.data.summarizeSchedules
import com.wavachao.timeblock.ui.icons.BlockIcons
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(blocks: List<TimeBlock>, today: LocalDate = LocalDate.now()) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    var offset by rememberSaveable { mutableStateOf(0L) }
    val from=if(monthly) today.withDayOfMonth(1).plusMonths(offset) else today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset)
    val through=if(monthly) from.withDayOfMonth(from.lengthOfMonth()) else from.plusDays(6)
    val summary=remember(blocks,from,through) { summarizeSchedules(blocks,from,through) }
    val colors=MaterialTheme.colorScheme
    fun hours(minutes: Long) = String.format(java.util.Locale.CHINA,"%.1f",minutes/60.0)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,20.dp,20.dp,28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("统计",style=MaterialTheme.typography.headlineLarge); Text("看看时间去了哪里",color=colors.onSurfaceVariant,modifier=Modifier.padding(top=6.dp)) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(!monthly,{monthly=false;offset=0},label={Text("按周")},shape=RoundedCornerShape(12.dp))
            FilterChip(monthly,{monthly=true;offset=0},label={Text("按月")},shape=RoundedCornerShape(12.dp))
            Spacer(Modifier.weight(1f))
            TextButton({offset=0}) {Text(if(monthly) "本月" else "本周")}
        } }
        item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            GlyphButton(BlockIcons.ChevronLeft,"上一${if(monthly) "月" else "周"}",{offset--})
            Text(if(monthly) "${from.year}年${from.monthValue}月" else "${from.monthValue}.${from.dayOfMonth} — ${through.monthValue}.${through.dayOfMonth}",Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.titleMedium)
            GlyphButton(BlockIcons.ChevronRight,"下一${if(monthly) "月" else "周"}",{offset++})
        } }
        item { Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=colors.primary)) {
            Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("日程完成率",color=colors.onPrimary.copy(alpha=.8f))
                Text("${(summary.completionRate*100).roundToInt()}%",style=MaterialTheme.typography.headlineLarge,color=colors.onPrimary)
                LinearProgressIndicator(progress={summary.completionRate},modifier=Modifier.fillMaxWidth().height(6.dp),color=colors.onPrimary,trackColor=colors.onPrimary.copy(alpha=.2f))
                Text("${summary.completed} 项已完成 / ${summary.total} 项安排",color=colors.onPrimary,style=MaterialTheme.typography.bodyMedium)
            }
        } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            StatTile("计划时长",hours(summary.plannedMinutes),"小时",Modifier.weight(1f))
            StatTile("已完成时长",hours(summary.completedMinutes),"小时",Modifier.weight(1f))
        } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            StatTile("待完成","${summary.total-summary.completed}","项",Modifier.weight(1f))
            StatTile("全天事项","${summary.allDay}","项",Modifier.weight(1f))
        } }
        item { Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=colors.surface)) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("每日安排",style=MaterialTheme.typography.titleMedium)
                val max=summary.dailyCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
                Row(Modifier.fillMaxWidth().height(130.dp),horizontalArrangement=Arrangement.spacedBy(if(monthly) 3.dp else 10.dp),verticalAlignment=Alignment.Bottom) {
                    summary.dailyCounts.forEachIndexed { i,count ->
                        Column(Modifier.weight(1f).semantics { contentDescription="${from.plusDays(i.toLong())}：$count 项日程" },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
                            if(!monthly) Text("$count",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth().height(if(count==0) 3.dp else (86f*count/max).dp).clip(RoundedCornerShape(topStart=5.dp,topEnd=5.dp)).background(if(from.plusDays(i.toLong())==today) colors.primary else colors.primaryContainer))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    (if(monthly) listOf("1日","${from.lengthOfMonth()/2}日","${from.lengthOfMonth()}日") else listOf("一","二","三","四","五","六","日")).forEach { Text(it,style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant) }
                }
                if(summary.total==0) Text("这段时间还没有日程",color=colors.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
            }
        } }
        item { Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=colors.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("分类分布",style=MaterialTheme.typography.titleMedium)
                if(summary.categories.isEmpty()) Text("添加日程后，这里会显示分类分布",color=colors.onSurfaceVariant)
                summary.categories.entries.sortedByDescending {it.value}.forEach { (category,count) ->
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(category.displayName);Text("$count 项",color=colors.onSurfaceVariant)}
                        LinearProgressIndicator(progress={count.toFloat()/summary.total.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(5.dp),color=category.color,trackColor=colors.surfaceVariant)
                    }
                }
            }
        } }
        item { Text("按所选时间内涉及的日程统计。全天事项只计数量；跨天时长按区间截取，重叠日程分别计时。已完成时长是计划时长，不是实际计时。",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant) }
    }
}

@Composable
private fun StatTile(label: String,value: String,unit: String,modifier: Modifier) {
    Card(modifier,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(label,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$value $unit",style=MaterialTheme.typography.titleLarge)
        }
    }
}
