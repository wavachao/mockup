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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    private val viewModel: MainViewModel by viewModels {
        val container = (application as TimeBlockApp).container
        MainViewModel.factory(container.repository, container.reminderScheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialBlockId = intent?.getLongExtra(EXTRA_BLOCK_ID, -1L) ?: -1L
        setContent {
            TimeBlockTheme {
                TimeBlockRoot(initialBlockId = initialBlockId, viewModel = viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_BLOCK_ID = "extra_block_id"
    }
}

private data class TabSpec(val route: String, val label: String, val icon: IconSpec)

private val Tabs = listOf(
    TabSpec(Routes.TODAY, "今天", BlockIcons.Today),
    TabSpec(Routes.CALENDAR, "日历", BlockIcons.Calendar),
    TabSpec(Routes.INSIGHTS, "回顾", BlockIcons.Insights),
    TabSpec(Routes.PROFILE, "我的", BlockIcons.Profile),
)

@Composable
private fun TimeBlockRoot(initialBlockId: Long, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.TODAY
    val isTabRoute = Tabs.any { it.route == currentRoute }

    val todayState by viewModel.todayState.collectAsState()
    val calendarState by viewModel.calendarState.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()

    var sheetOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TimeBlockDraft.startingAt(LocalDateTime.now())) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* reminders simply stay silent if the user declines */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // A reminder tap opens the block it refers to.
    LaunchedEffect(initialBlockId) {
        if (initialBlockId > 0L) navController.navigate(Routes.detail(initialBlockId))
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.TODAY) {
                    TodayScreen(
                        state = todayState,
                        onToggleDone = viewModel::toggleDone,
                        onOpenBlock = { id -> navController.navigate(Routes.detail(id)) },
                        onStepDate = viewModel::stepSelectedDate,
                        onAddForDate = { date ->
                            draft = TimeBlockDraft.startingAt(date.atTime(9, 0))
                            sheetOpen = true
                        },
                    )
                }

                composable(Routes.CALENDAR) {
                    CalendarScreen(
                        state = calendarState,
                        onSelectDate = viewModel::selectCalendarDate,
                        onStepMonth = viewModel::stepMonth,
                        onOpenBlock = { id -> navController.navigate(Routes.detail(id)) },
                        onCreateForDate = { date ->
                            draft = TimeBlockDraft.startingAt(date.atTime(9, 0))
                            sheetOpen = true
                        },
                    )
                }

                composable(Routes.INSIGHTS) {
                    InsightsScreen(
                        state = insightsState,
                        onStepWeek = viewModel::stepWeek,
                        onCurrentWeek = viewModel::currentWeek,
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        totalBlocks = todayState.blocks.size,
                        selectedDate = todayState.selectedDate,
                        onOpenToday = {
                            navController.navigate(Routes.TODAY) { launchSingleTop = true }
                        },
                    )
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("blockId") { type = NavType.LongType }),
                ) { entry ->
                    val blockId = entry.arguments?.getLong("blockId") ?: 0L
                    var block by remember(blockId) { mutableStateOf<TimeBlock?>(null) }
                    LaunchedEffect(blockId, todayState.blocks) {
                        block = viewModel.blockById(blockId)
                    }
                    BlockDetailScreen(
                        block = block,
                        now = todayState.now,
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Routes.edit(id)) },
                        onDelete = { id ->
                            viewModel.deleteBlock(id)
                            navController.popBackStack()
                        },
                        onToggleDone = { viewModel.toggleDone(it) },
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
                        dateHint = date,
                        onSave = { created ->
                            viewModel.createBlock(created) { navController.popBackStack() }
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
                                viewModel.updateBlock(edited) { navController.popBackStack() }
                            },
                            onDelete = { id ->
                                viewModel.deleteBlock(id)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }

        if (currentRoute == Routes.TODAY) {
            QuickAddFab(
                onClick = {
                    draft = TimeBlockDraft.startingAt(todayState.now)
                    sheetOpen = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 104.dp),
            )
        }

        if (isTabRoute) {
            BottomTabBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        QuickAddSheet(
            visible = sheetOpen,
            dates = (0..3).map { todayState.today.plusDays(it.toLong()) },
            draft = draft,
            onDraftChange = { draft = it },
            onSubmit = {
                viewModel.createBlock(draft)
                sheetOpen = false
            },
            onExpand = {
                sheetOpen = false
                navController.navigate(Routes.addFor(draft.date))
            },
            onDismiss = { sheetOpen = false },
        )
    }
}

/** The 88dp tab bar from the mockup, rebuilt with the same glass + pill treatment. */
@Composable
private fun BottomTabBar(
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppTokens.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, palette.background.copy(alpha = 0.92f)),
                ),
            )
            .padding(top = 12.dp, bottom = 18.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.hairline))
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Radius.chip))
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                Modifier
                                    .width(46.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(Radius.chip))
                                    .background(BrandColors.brandGradientSoft)
                                    .border(1.dp, Color(0x528C7CFF), RoundedCornerShape(Radius.chip)),
                            )
                        }
                        TimeBlockIcon(
                            icon = tab.icon,
                            size = 21.dp,
                            tint = if (selected) Color(0xFFA99CFF) else palette.muted,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tab.label,
                        color = if (selected) palette.text else palette.muted,
                        style = AppTokens.type.micro.copy(fontSize = 10.5.sp, fontWeight = FontWeight(600)),
                    )
                }
            }
        }
    }
}
