package com.wavachao.timeblock

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.data.model.TimeBlockDraft
import com.wavachao.timeblock.ui.screens.BlockEditorScreen
import com.wavachao.timeblock.ui.theme.TimeBlockTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class EditorFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val date = LocalDate.of(2026, 9, 19)

    @Test fun initialTimeDoesNotMoveAndExpandedDraftIsPreserved() {
        val draft = TimeBlockDraft.startingAt(date.atTime(9, 0), 90).copy(title = "保留草稿", notes = "备注内容")
        var saved: TimeBlockDraft? = null
        compose.setContent {
            TimeBlockTheme { BlockEditorScreen(null, date, { saved = it }, {}, {}, initialDraft = draft) }
        }
        compose.waitForIdle()
        compose.onNodeWithText("保存日程").performClick()
        compose.runOnIdle { assertEquals(draft.copy(endDate = draft.end.toLocalDate()), saved) }
    }

    @Test fun blankTitleCannotBeSaved() {
        var saved = false
        compose.setContent { TimeBlockTheme { BlockEditorScreen(null, date, { saved = true }, {}, {}) } }
        compose.onNodeWithText("保存日程").performClick()
        compose.onNodeWithText("请填写日程名称").assertExists()
        compose.runOnIdle { assertFalse(saved) }
    }

    @Test fun allDayCanBeSavedWithoutTime() {
        var saved: TimeBlockDraft? = null
        compose.setContent { TimeBlockTheme { BlockEditorScreen(null, date, { saved = it }, {}, {}, initialDraft = TimeBlockDraft(date = date, title = "全天事项测试")) } }
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("保存日程").performClick()
        compose.runOnIdle { assertTrue(saved!!.allDay); assertEquals(date.atStartOfDay(), saved!!.start); assertEquals(date.plusDays(1).atStartOfDay(), saved!!.end) }
    }

    @Test fun directTimeEntrySavesExactMinutes() {
        var saved: TimeBlockDraft? = null
        compose.setContent { TimeBlockTheme { BlockEditorScreen(null, date, {saved=it}, {}, {}, initialDraft=TimeBlockDraft(title="精确时间",date=date)) } }
        compose.onNodeWithContentDescription("开始时间").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog()))[0].performTextReplacement("14")
        compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog()))[1].performTextReplacement("37")
        compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("保存日程").performClick()
        compose.runOnIdle { assertEquals(java.time.LocalTime.of(14,37),saved!!.startTime); assertEquals(java.time.LocalTime.of(15,37),saved!!.endTime) }
    }

    @Test fun tomorrowShortcutPreservesTimeAndDuration() {
        var saved: TimeBlockDraft? = null
        val moment=date.atTime(8,0)
        compose.setContent { TimeBlockTheme { BlockEditorScreen(null,date,{saved=it},{},{},now=moment,initialDraft=TimeBlockDraft(title="快捷日期",date=date,startTime=java.time.LocalTime.of(10,15),endTime=java.time.LocalTime.of(11,45))) } }
        compose.onNodeWithText("明天").performScrollTo().performClick()
        compose.onNodeWithText("保存日程").performClick()
        compose.runOnIdle { assertEquals(date.plusDays(1),saved!!.date);assertEquals(90,saved!!.durationMinutes) }
    }

    @Test fun commonTimeNeedsNoConfirmTap() {
        var saved: TimeBlockDraft? = null
        compose.setContent { TimeBlockTheme { BlockEditorScreen(null,date,{saved=it},{},{},initialDraft=TimeBlockDraft(title="快捷时间",date=date)) } }
        compose.onNodeWithContentDescription("开始时间").performScrollTo().performClick()
        compose.onNodeWithText("14:00").performClick()
        compose.onNodeWithText("选择开始时间").assertDoesNotExist()
        compose.onNodeWithText("保存日程").performClick()
        compose.runOnIdle { assertEquals(java.time.LocalTime.of(14,0),saved!!.startTime) }
    }

    @Test fun titleAndNotesSurviveActivityRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TimeBlockTheme { BlockEditorScreen(null, date, {}, {}, {}, initialDraft = TimeBlockDraft(date = date)) }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("旋转后保留")
        compose.onNodeWithText("备注 / 地点").performScrollTo().performTextInput("重要备注")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("旋转后保留").assertExists()
        compose.onNodeWithText("重要备注").assertExists()
    }

    @Test fun deleteRequiresExplicitConfirmationAndCanBeCancelled() {
        val original = TimeBlock(id = 7, title = "保留安排", start = date.atTime(9, 0), end = date.atTime(10, 0))
        var deleted = false
        compose.setContent {
            TimeBlockTheme { BlockEditorScreen(original, date, {}, { deleted = true }, {}) }
        }
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("删除这条日程？").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertFalse(deleted) }
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("确认删除", substring = false).performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }
}
