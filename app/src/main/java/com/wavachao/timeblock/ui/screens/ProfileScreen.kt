package com.wavachao.timeblock.ui.screens
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wavachao.timeblock.ui.icons.*
import java.time.LocalDate

@Composable
fun ProfileScreen(totalBlocks: Int, selectedDate: LocalDate, onOpenToday: () -> Unit, modifier: Modifier = Modifier) {
    val context=LocalContext.current
    val colors=MaterialTheme.colorScheme
    var notifications by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) { notifications=NotificationManagerCompat.from(context).areNotificationsEnabled(); onPauseOrDispose { } }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text("设置",style=MaterialTheme.typography.headlineLarge)
        Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=colors.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                TimeBlockIcon(BlockIcons.Calendar,size=32.dp,tint=colors.primary)
                Text("让安排井井有条",style=MaterialTheme.typography.titleLarge)
                Text("已记录 $totalBlocks 项日程",color=colors.onSurfaceVariant)
            }
        }
        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=colors.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("日程提醒",style=MaterialTheme.typography.titleMedium)
                Text(if(notifications) "通知已开启" else "通知尚未开启",color=colors.primary)
                Text("在日程中选择提醒时间。全天事项可以在当天或前一天上午 9 点提醒。",style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)
                OutlinedButton({context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))},shape=RoundedCornerShape(12.dp)) {Text("通知设置")}
            }
        }
        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=colors.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("本机保存",style=MaterialTheme.typography.titleMedium)
                Text("无需登录，关闭或重启后记录仍会保留。卸载应用或清除应用数据会删除记录。",style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)
            }
        }
        Text("时间段 · ${com.wavachao.timeblock.BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
    }
}
