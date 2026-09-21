package com.wavachao.timeblock

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.wavachao.timeblock.ui.screens.AgendaScreen
import com.wavachao.timeblock.ui.screens.StatisticsScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.navigationBarsPadding
import com.wavachao.timeblock.ui.util.DraftSaver
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.data.model.TimeBlockDraft
import com.wavachao.timeblock.ui.MainViewModel
import com.wavachao.timeblock.ui.Routes
import com.wavachao.timeblock.ui.components.QuickAddFab
import com.wavachao.timeblock.ui.components.QuickAddSheet
import com.wavachao.timeblock.ui.components.ScreenBackground
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.IconSpec
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.screens.BlockDetailScreen
import com.wavachao.timeblock.ui.screens.BlockEditorScreen
import com.wavachao.timeblock.ui.screens.CalendarScreen
import com.wavachao.timeblock.ui.screens.InsightsScreen
import com.wavachao.timeblock.ui.screens.ProfileScreen
import com.wavachao.timeblock.ui.screens.TodayScreen
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.theme.TimeBlockTheme
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private var notificationBlockId by mutableStateOf(-1L)

    private val viewModel: MainViewModel by viewModels {
        val container = (application as TimeBlockApp).container
        MainViewModel.factory(container.repository, container.reminderScheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationBlockId = intent?.getLongExtra(EXTRA_BLOCK_ID, -1L) ?: -1L
        setContent {
            TimeBlockTheme {
                TimeBlockRoot(initialBlockId = notificationBlockId, viewModel = viewModel,
                    onNotificationHandled = { notificationBlockId = -1L; intent?.removeExtra(EXTRA_BLOCK_ID) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationBlockId = intent.getLongExtra(EXTRA_BLOCK_ID, -1L)
    }

    companion object {
        const val EXTRA_BLOCK_ID = "extra_block_id"
    }
}

private data class TabSpec(val route: String, val label: String, val icon: IconSpec)

private val Tabs = listOf(
    TabSpec(Routes.TODAY, "日程", BlockIcons.Today),
    TabSpec(Routes.CALENDAR, "日历", BlockIcons.Calendar),
    TabSpec(Routes.INSIGHTS, "统计", BlockIcons.Insights),
    TabSpec(Routes.PROFILE, "设置", BlockIcons.Profile),
)

@Composable
private fun TimeBlockRoot(initialBlockId: Long, viewModel: MainViewModel, onNotificationHandled: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.TODAY
    val isTabRoute = Tabs.any { it.route == currentRoute }
    val allBlocks by viewModel.allBlocks.collectAsStateWithLifecycle()
    val todayState by viewModel.todayState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
    val insightsState by viewModel.insightsState.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val toggleDone: (TimeBlock) -> Unit = { block ->
        viewModel.toggleDone(block) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showSnackbar(if(block.done) "已恢复为待办" else "已完成一项日程", actionLabel = "撤销", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                    viewModel.toggleDone(block.copy(done = !block.done))
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) scope.launch { snackbar.showSnackbar("日程已保存。开启通知权限后才能收到提醒。") }
    }
    val requestReminderPermission: (TimeBlockDraft) -> Unit = { saved ->
        if (saved.reminderMinutes >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // A reminder tap opens the block it refers to.
    LaunchedEffect(initialBlockId) {
        if (initialBlockId > 0L) {
            navController.navigate(Routes.detail(initialBlockId)) { launchSingleTop = true }
            onNotificationHandled()
        }
    }

    Scaffold(
        bottomBar = { if(isTabRoute) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            Tabs.forEach { tab -> NavigationBarItem(selected = currentRoute == tab.route,
                onClick = { navController.navigate(tab.route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                icon = { TimeBlockIcon(icon = tab.icon, size = 24.dp, tint = if(currentRoute == tab.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }, label = { Text(tab.label) }) }
        } },
        floatingActionButton = { if(currentRoute == Routes.TODAY || currentRoute == Routes.CALENDAR) ExtendedFloatingActionButton(
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            onClick = { navController.navigate(Routes.addFor(if(currentRoute == Routes.CALENDAR) calendarState.selectedDate else LocalDate.now())) }) { TimeBlockIcon(BlockIcons.Plus, tint = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)); Text("新建日程") } },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = if(isTabRoute) 0.dp else 128.dp)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
                modifier = Modifier.fillMaxSize().padding(bottom = if(currentRoute == Routes.TODAY || currentRoute == Routes.CALENDAR) 80.dp else 0.dp),
            ) {
                composable(Routes.TODAY) {
                    AgendaScreen(allBlocks, todayState.now, { navController.navigate(Routes.detail(it)) }, toggleDone,
                        onEdit = { snackbar.currentSnackbarData?.dismiss(); navController.navigate(Routes.edit(it)) }, onMove = { block, day -> viewModel.moveBlock(block, day) })
                }

                composable(Routes.CALENDAR) {
                    CalendarScreen(
                        state = calendarState,
                        blocks = allBlocks,
                        onToggle = toggleDone,
                        onEdit = { snackbar.currentSnackbarData?.dismiss(); navController.navigate(Routes.edit(it)) },
                        onMove = { block, day -> viewModel.moveBlock(block, day) },
                        onSelectDate = viewModel::selectCalendarDate,
                        onStepMonth = viewModel::stepMonth,
                        onOpenBlock = { id -> navController.navigate(Routes.detail(id)) },
                        onCreateForDate = { date ->
                            navController.navigate(Routes.addFor(date))
                        },
                    )
                }

                composable(Routes.INSIGHTS) {
                    StatisticsScreen(allBlocks, todayState.today)
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        totalBlocks = totalCount,
                        selectedDate = todayState.today,
                        onOpenToday = {
                            viewModel.goToToday()
                            navController.navigate(Routes.TODAY) { launchSingleTop = true }
                        },
                    )
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("blockId") { type = NavType.LongType }),
                ) { entry ->
                    val blockId = entry.arguments?.getLong("blockId") ?: 0L
                    val detail by remember(blockId) { viewModel.observeBlock(blockId).map { true to it } }
                        .collectAsStateWithLifecycle(initialValue = false to null)
                    BlockDetailScreen(
                        block = detail.second,
                        loading = !detail.first,
                        now = todayState.now,
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> snackbar.currentSnackbarData?.dismiss(); navController.navigate(Routes.edit(id)) },
                        onDelete = { id ->
                            viewModel.deleteBlock(id) { navController.popBackStack() }
                        },
                        onToggleDone = toggleDone,
                    )
                }

                composable(
                    route = Routes.ADD,
                    arguments = listOf(
                        navArgument("date") {
                            type = NavType.StringType
                            defaultValue = LocalDate.now().toString()
                        },
                    ),
                ) { entry ->
                    val date = entry.arguments?.getString("date")
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: LocalDate.now()
                    BlockEditorScreen(
                        initial = null,
                        initialDraft = null,
                        dateHint = date,
                        onSave = { created ->
                            viewModel.createBlock(created) { id ->
                                navController.popBackStack()
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    if (snackbar.showSnackbar("日程已保存", actionLabel = "查看", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                                        navController.navigate(Routes.detail(id))
                                    }
                                }
                                requestReminderPermission(created)
                            }
                        },
                        onDelete = {},
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.EDIT,
                    arguments = listOf(navArgument("blockId") { type = NavType.LongType }),
                ) { entry ->
                    val blockId = entry.arguments?.getLong("blockId") ?: 0L
                    var block by remember(blockId) { mutableStateOf<TimeBlock?>(null) }
                    LaunchedEffect(blockId) { block = viewModel.blockById(blockId) }
                    val loaded = block
                    if (loaded != null) {
                        BlockEditorScreen(
                            initial = loaded,
                            dateHint = loaded.date,
                            onSave = { edited ->
                                viewModel.updateBlock(edited) {
                                    navController.popBackStack()
                                    requestReminderPermission(edited)
                                }
                            },
                            onDelete = { id ->
                                viewModel.deleteBlock(id) {
                                    navController.popBackStack(Routes.TODAY, inclusive = false)
                                }
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }

    }
}
