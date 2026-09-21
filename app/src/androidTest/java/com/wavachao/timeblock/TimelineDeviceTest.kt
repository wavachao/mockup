package com.wavachao.timeblock

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wavachao.timeblock.data.DayStats
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.TodayUiState
import com.wavachao.timeblock.ui.screens.TodayScreen
import com.wavachao.timeblock.ui.theme.TimeBlockTheme
import com.wavachao.timeblock.ui.util.buildTimeline
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.PathParser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TimelineDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun bellArcUsesValidViewportGeometry() {
        val bounds = PathParser.parse(BlockIcons.Bell.pathData.first()).getBounds()
        assertEquals(5f, bounds.top, 0.1f)
        assertEquals(19f, bounds.bottom, 0.1f)
        assertTrue(bounds.left >= 0f && bounds.right <= 24f)
    }

    @Test fun overlappingCardsOccupySeparateColumnsInsideScreen() {
        val date = LocalDate.now()
        val blocks = listOf(
            TimeBlock(id = 1, title = "重叠安排 A", start = date.atTime(9, 0), end = date.atTime(10, 0)),
            TimeBlock(id = 2, title = "重叠安排 B", start = date.atTime(9, 15), end = date.atTime(10, 0)),
        )
        val state = TodayUiState(today = date, selectedDate = date, now = date.atTime(8, 0),
            blocks = blocks, timeline = buildTimeline(blocks), stats = DayStats.of(date, blocks, date.atTime(8, 0)))
        compose.setContent { TimeBlockTheme { TodayScreen(state, {}, {}, {}, {}) } }
        val first = compose.onNodeWithText("重叠安排 A").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("重叠安排 B").fetchSemanticsNode().boundsInRoot
        assertTrue("overlapping schedules must not cover each other", first.right <= second.left)
        assertTrue("both titles must retain visible width", first.width > 0 && second.width > 0)
        assertTrue("right lane must stay on screen", second.right <= compose.activity.resources.displayMetrics.widthPixels)
    }
}
