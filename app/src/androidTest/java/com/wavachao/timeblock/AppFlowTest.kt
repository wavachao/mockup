package com.wavachao.timeblock

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import com.wavachao.timeblock.data.model.TimeBlock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule(order = 0) val notifications = object : ExternalResource() {
        override fun before() {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.grantRuntimePermission(
                    instrumentation.targetContext.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val ids = mutableListOf<Long>()
    private val repo get() = (compose.activity.application as TimeBlockApp).container.repository

    @After fun cleanup() = runBlocking { ids.forEach { repo.delete(it) } }

    @Test fun newScheduleOpensEditorAndBackReturnsHome() {
        compose.onNodeWithText("新建日程").performClick()
        compose.onNodeWithText("保存日程").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("我的日程").assertIsDisplayed()
    }

    @Test fun distantFutureScheduleIsSearchable() {
        val date = LocalDate.now().plusYears(1)
        val id = runBlocking { repo.save(TimeBlock(title = "明年的旅行", start = date.atStartOfDay(), end = date.plusDays(1).atStartOfDay(), allDay = true)) }
        ids += id
        compose.onNode(hasSetTextAction()).performTextInput("明年的旅行")
        compose.waitUntil(10000) { compose.onAllNodesWithText("全天").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithText("全天").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { repo.blockById(id) }!!.allDay)
    }

    @Test fun statisticsAndQuickRescheduleUseStoredSchedules() {
        val day=LocalDate.now()
        val id=runBlocking {repo.save(TimeBlock(title="QA改期",start=day.atTime(9,0),end=day.atTime(10,30),notes="保留备注"))}
        ids+=id
        compose.waitUntil(10000) {compose.onAllNodesWithText("QA改期").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("更多操作：QA改期").performScrollTo().performClick()
        compose.onNodeWithText("移到明天").performClick()
        compose.waitUntil(10000) {runBlocking {repo.blockById(id)?.date==day.plusDays(1)}}
        val moved=runBlocking {repo.blockById(id)}!!
        assertEquals(90,moved.durationMinutes)
        assertEquals("保留备注",moved.notes)
        compose.onNodeWithText("统计").performClick()
        compose.onNodeWithText("日程完成率").assertExists()
        compose.onNodeWithText("按月").performClick()
        compose.onNodeWithText("计划时长").assertExists()
    }

    @Test fun completionCanBeUndoneFromList() {
        val day=LocalDate.now()
        val id=runBlocking {repo.save(TimeBlock(title="QA撤销完成",start=day.atTime(9,0),end=day.atTime(10,0)))}
        ids+=id
        compose.waitUntil(10000) {compose.onAllNodesWithText("QA撤销完成").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("完成：QA撤销完成").performScrollTo().performClick()
        compose.waitUntil(10000) {runBlocking {repo.blockById(id)?.done==true}}
        compose.onNodeWithText("撤销").performClick()
        compose.waitUntil(10000) {runBlocking {repo.blockById(id)?.done==false}}
    }

    @Test fun reviewScreensWithRealSchedules() {
        val day=LocalDate.now()
        val fixtures=listOf(
            TimeBlock(title="项目周会",start=day.atTime(9,30),end=day.atTime(10,30),notes="会议室 A · 确认下周计划"),
            TimeBlock(title="阅读与整理",start=day.atTime(14,0),end=day.atTime(15,30),category=com.wavachao.timeblock.data.model.BlockCategory.STUDY),
            TimeBlock(title="晨间散步",start=day.atTime(7,0),end=day.atTime(7,30),done=true,category=com.wavachao.timeblock.data.model.BlockCategory.SPORT),
            TimeBlock(title="办理证件",start=day.plusDays(1).atStartOfDay(),end=day.plusDays(2).atStartOfDay(),allDay=true,category=com.wavachao.timeblock.data.model.BlockCategory.LIFE),
            TimeBlock(title="公园慢跑",start=day.plusDays(1).atTime(18,0),end=day.plusDays(1).atTime(19,0),category=com.wavachao.timeblock.data.model.BlockCategory.SPORT)
        )
        runBlocking { fixtures.forEach {ids+=repo.save(it)} }
        compose.waitUntil(10000) {compose.onAllNodesWithText("项目周会").fetchSemanticsNodes().isNotEmpty()}
        capture("agenda")
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val icon=ctx.packageManager.getApplicationIcon(ctx.packageName)
        val iconBitmap=android.graphics.Bitmap.createBitmap(256,256,android.graphics.Bitmap.Config.ARGB_8888)
        icon.setBounds(0,0,256,256)
        icon.draw(android.graphics.Canvas(iconBitmap))
        java.io.File(ctx.getExternalFilesDir(null),"qa-1.2.0/app-icon.png").outputStream().use {iconBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        iconBitmap.recycle()
        compose.onNodeWithText("新建日程").performClick()
        compose.onNodeWithText("保存日程").assertIsDisplayed()
        capture("editor")
        compose.onNodeWithContentDescription("开始时间").performScrollTo().performClick()
        capture("time-picker")
        compose.onNodeWithText("取消").performClick()
        Espresso.pressBack()
        compose.onNodeWithText("统计").performClick()
        compose.onNodeWithText("日程完成率").assertIsDisplayed()
        capture("statistics")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("分类分布"))
        capture("statistics-distribution")
        compose.onNodeWithText("日历").performClick()
        capture("calendar")
        compose.onNodeWithText("收起月历").performScrollTo().performClick()
        capture("calendar-week")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val directory=java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"qa-1.2.0").apply {mkdirs()}
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.File(directory,"$name.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }

    @Test fun warmNotificationOpensDetailAndCompletionUpdatesImmediately() {
        val date = LocalDate.now().plusDays(2)
        val id = runBlocking { repo.save(TimeBlock(title = "QA通知详情", start = date.atTime(9, 0), end = date.atTime(10, 0))) }
        ids += id
        compose.runOnUiThread {
            // Keep the launch action/categories so ActivityScenario can track teardown
            // after MainActivity replaces its intent in onNewIntent.
            compose.activity.startActivity(Intent(compose.activity.intent)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_BLOCK_ID, id))
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("QA通知详情").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("标记为已完成").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("标记为未完成").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { repo.blockById(id) }!!.done)
        compose.onNodeWithText("编辑日程").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("QA编辑后完成")
        compose.onNodeWithText("保存日程").performClick()
        compose.waitUntil(10000) { runBlocking { repo.blockById(id)?.title == "QA编辑后完成" } }
        assertTrue(runBlocking { repo.blockById(id) }!!.done)
    }
}
