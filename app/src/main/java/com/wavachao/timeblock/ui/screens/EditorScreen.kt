package com.wavachao.timeblock.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.wavachao.timeblock.data.model.*
import com.wavachao.timeblock.ui.icons.*
import com.wavachao.timeblock.ui.util.DraftSaver
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
private fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}

@Composable
fun BlockEditorScreen(initial: TimeBlock?, dateHint: LocalDate, onSave: (TimeBlockDraft) -> Unit,
    onDelete: (Long) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(), initialDraft: TimeBlockDraft? = null) {
    val context=LocalContext.current
    val preferences=remember { context.getSharedPreferences("editor_preferences",0) }
    val original=rememberSaveable(initial?.id,saver=DraftSaver) {
        val d=initial?.let(TimeBlockDraft::from) ?: initialDraft ?: TimeBlockDraft.startingAt(if(dateHint==now.toLocalDate()) now else dateHint.atTime(9,0)).copy(
            reminderMinutes=preferences.getInt("reminder",-1), category=BlockCategory.fromStorage(preferences.getString("category",null)))
        if(d.endDate==null) d.copy(endDate=if(d.allDay) d.date else d.end.toLocalDate()) else d
    }
    var draft by rememberSaveable(stateSaver=DraftSaver) { mutableStateOf(original) }
    var moreDates by rememberSaveable { mutableStateOf(original.endDate!=original.date) }
    var discard by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<Boolean?>(null) }
    var attempted by remember { mutableStateOf(false) }
    val back={if(draft!=original) discard=true else onBack()}
    BackHandler { back() }
    fun save() {
        attempted=true
        if(draft.validationError==null) {
            preferences.edit().putString("category",draft.category.name).apply()
            if(!draft.allDay) preferences.edit().putInt("reminder",draft.reminderMinutes).apply()
            onSave(draft)
        }
    }
    fun startDate(day: LocalDate) {
        draft=if(draft.allDay) draft.copy(date=day,endDate=day.plusDays(java.time.temporal.ChronoUnit.DAYS.between(draft.date,draft.endDate?:draft.date).coerceAtLeast(0))) else draft.withStart(day)
    }
    fun pickDate(start: Boolean) {
        val d=if(start) draft.date else draft.endDate?:draft.date
        DatePickerDialog(context,{_,y,m,day -> val chosen=LocalDate.of(y,m+1,day); if(start) startDate(chosen) else draft=draft.copy(endDate=chosen)},d.year,d.monthValue-1,d.dayOfMonth).show()
    }
    fun chooseTime(start: Boolean,time: LocalTime) {
        draft=if(start) draft.withStartTime(time) else draft.copy(endTime=time)
        editingTime=null
    }
    Column(modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
            GlyphButton(BlockIcons.ChevronLeft,"返回",back)
            Text(if(initial==null) "新建日程" else "编辑日程",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            if(initial!=null) TextButton({deleting=true}) { Text("删除",color=MaterialTheme.colorScheme.error) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(draft.title,{draft=draft.copy(title=it)},label={Text("日程名称")},placeholder={Text("要做什么？")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={save()}))
            EditorCard {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    TimeBlockIcon(BlockIcons.Calendar,tint=MaterialTheme.colorScheme.primary)
                    TextButton({pickDate(true)},Modifier.weight(1f).semantics { contentDescription="开始日期" }) { Text(friendlyDay(draft.date)) }
                    TimeBlockIcon(BlockIcons.ChevronDown,tint=MaterialTheme.colorScheme.onSurfaceVariant,size=16.dp)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("今天" to now.toLocalDate(),"明天" to now.toLocalDate().plusDays(1),"一周后" to now.toLocalDate().plusWeeks(1)).forEach { (label,day) ->
                        FilterChip(draft.date==day,{startDate(day)},label={Text(label)},shape=RoundedCornerShape(10.dp))
                    }
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().toggleable(draft.allDay,onValueChange={ enabled ->
                    draft=draft.copy(allDay=enabled,reminderMinutes=-1)
                    if(!enabled && !draft.end.isAfter(draft.start)) draft=draft.withDuration(60)
                }),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("全天事项",style=MaterialTheme.typography.titleMedium); Text("只记日期",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(draft.allDay,onCheckedChange=null)
                }
                if(!draft.allDay) {
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        listOf(true,false).forEach { start ->
                            Surface(modifier=Modifier.weight(1f).clickable {editingTime=start}.semantics {contentDescription=if(start) "开始时间" else "结束时间"},shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
                                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Text(if(start) "开始" else "结束",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text((if(start) draft.startTime else draft.endTime).format(DateTimeFormatter.ofPattern("HH:mm")),style=MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(30,60,120).forEach { minutes -> FilterChip(draft.durationMinutes==minutes,{draft=draft.withDuration(minutes)},label={Text(if(minutes==30) "30分" else "${minutes/60}小时")},shape=RoundedCornerShape(10.dp)) }
                    }
                }
                if(moreDates || draft.endDate!=draft.date) TextButton({pickDate(false)},Modifier.fillMaxWidth()) { Text("结束日期  ${draft.endDate}${if(draft.allDay) "（含当天）" else ""}") }
                else TextButton({moreDates=true},contentPadding=PaddingValues(0.dp)) { Text(if(draft.allDay) "＋ 连续多天" else "＋ 跨天安排",style=MaterialTheme.typography.labelMedium) }
            }
            EditorCard {
                var categories by remember {mutableStateOf(false)}
                var reminders by remember {mutableStateOf(false)}
                Box {
                    Row(Modifier.fillMaxWidth().clickable {categories=true}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                        TimeBlockIcon(draft.category.icon,tint=draft.category.color)
                        Text("分类",Modifier.weight(1f).padding(start=12.dp),style=MaterialTheme.typography.bodyMedium)
                        Text(draft.category.displayName,color=MaterialTheme.colorScheme.primary)
                        TimeBlockIcon(BlockIcons.ChevronDown,tint=MaterialTheme.colorScheme.onSurfaceVariant,size=18.dp)
                    }
                    DropdownMenu(categories,{categories=false}) {BlockCategory.entries.forEach { c -> DropdownMenuItem(text={Text(c.displayName)},onClick={draft=draft.copy(category=c);categories=false})}}
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                fun reminderLabel(n: Int)=if(n<0) "不提醒" else if(draft.allDay) {if(n==0) "当天 9 点" else "前一天 9 点"} else ReminderLead.label(n)
                Box {
                    Row(Modifier.fillMaxWidth().clickable {reminders=true}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                        TimeBlockIcon(BlockIcons.Bell,tint=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("提醒",Modifier.weight(1f).padding(start=12.dp),style=MaterialTheme.typography.bodyMedium)
                        Text(reminderLabel(draft.reminderMinutes),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(reminders,{reminders=false}) {(if(draft.allDay) listOf(-1,0,1440) else listOf(-1,0,5,10,30,60)).forEach { n -> DropdownMenuItem(text={Text(reminderLabel(n))},onClick={draft=draft.copy(reminderMinutes=n);reminders=false})}}
                }
            }
            OutlinedTextField(draft.notes.orEmpty(),{draft=draft.copy(notes=it)},label={Text("备注 / 地点")},modifier=Modifier.fillMaxWidth(),minLines=2,shape=RoundedCornerShape(14.dp))
            Spacer(Modifier.height(8.dp))
        }
        Surface(color=MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal=20.dp,vertical=12.dp)) {
                if(attempted && draft.validationError!=null) Text(draft.validationError!!,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(bottom=8.dp))
                Button({save()},Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(14.dp)) {Text("保存日程")}
            }
        }
    }
    editingTime?.let { start ->
        key(start) {
            val time=if(start) draft.startTime else draft.endTime
            var hour by remember {mutableStateOf(TextFieldValue(time.hour.toString().padStart(2,'0'),TextRange(0,2)))}
            var minute by remember {mutableStateOf(TextFieldValue(time.minute.toString().padStart(2,'0'),TextRange(0,2)))}
            val valid=hour.text.toIntOrNull() in 0..23 && minute.text.toIntOrNull() in 0..59
            val focus=LocalFocusManager.current
            val confirm={if(valid) chooseTime(start,LocalTime.of(hour.text.toInt(),minute.text.toInt()))}
            AlertDialog(onDismissRequest={editingTime=null},title={Text(if(start) "选择开始时间" else "选择结束时间")},
                text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(hour,{if(it.text.length<=2 && it.text.all(Char::isDigit)) hour=it},label={Text("小时")},modifier=Modifier.weight(1f),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),keyboardActions=KeyboardActions(onNext={focus.moveFocus(FocusDirection.Next)}))
                        OutlinedTextField(minute,{if(it.text.length<=2 && it.text.all(Char::isDigit)) minute=it},label={Text("分钟")},modifier=Modifier.weight(1f),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={confirm()}))
                    }
                    if(!valid) Text("小时 0–23，分钟 0–59",color=MaterialTheme.colorScheme.error)
                    Text("常用时间 · 点选即确定",style=MaterialTheme.typography.labelMedium)
                    listOf(listOf("08:30","09:00","12:00"),listOf("14:00","18:00","20:00")).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {row.forEach { label ->
                        OutlinedButton({chooseTime(start,LocalTime.parse(label))},Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp),shape=RoundedCornerShape(10.dp)) {Text(label,style=MaterialTheme.typography.labelMedium)}
                    }}}
                }},confirmButton={TextButton({confirm()},enabled=valid) {Text("确定")}},dismissButton={TextButton({editingTime=null}) {Text("取消")}})
        }
    }
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("放弃未保存的修改？")},confirmButton={TextButton(onBack) {Text("放弃修改")}},dismissButton={TextButton({discard=false}) {Text("继续编辑")}})
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("删除这条日程？")},confirmButton={TextButton({initial?.let {onDelete(it.id)}}) {Text("确认删除")}},dismissButton={TextButton({deleting=false}) {Text("取消")}})
}
