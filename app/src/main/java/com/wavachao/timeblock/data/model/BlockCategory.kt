package com.wavachao.timeblock.data.model

import androidx.compose.ui.graphics.Color
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.IconSpec
import com.wavachao.timeblock.ui.theme.BrandColors
import java.time.DayOfWeek

/** The five colour-coded buckets from the mockup, plus icons for the calendar legend. */
enum class BlockCategory(
    val displayName: String,
    val color: Color,
    val icon: IconSpec,
) {
    WORK("工作", BrandColors.Work, BlockIcons.Code),
    STUDY("学习", BrandColors.Study, BlockIcons.Book),
    LIFE("生活", BrandColors.Life, BlockIcons.Meal),
    SPORT("运动", BrandColors.Sport, BlockIcons.Dumbbell),
    REST("休息", BrandColors.Rest, BlockIcons.Moon),
    ;

    companion object {
        val palette: List<BlockCategory> get() = entries

        fun fromStorage(value: String?): BlockCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WORK
    }
}

/** How a block repeats when it is saved. */
enum class RecurrenceRule(val displayName: String) {
    NONE("不重复"),
    DAILY("每天"),
    WEEKDAYS("工作日"),
    WEEKLY("每周"),
    ;

    fun matches(day: DayOfWeek): Boolean = when (this) {
        NONE -> false
        DAILY -> true
        WEEKDAYS -> day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
        WEEKLY -> true
    }

    companion object {
        fun fromStorage(value: String?): RecurrenceRule =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
    }
}

/** Reminder lead times offered by the editor, in minutes. */
enum class ReminderLead(val minutes: Int, val displayName: String) {
    NONE(0, "不提醒"),
    AT_START(0, "开始时"),
    BEFORE_5(5, "开始前 5 分钟"),
    BEFORE_10(10, "开始前 10 分钟"),
    BEFORE_30(30, "开始前 30 分钟"),
    BEFORE_60(60, "开始前 1 小时"),
    ;

    companion object {
        fun fromMinutes(minutes: Int): ReminderLead =
            entries.firstOrNull { it.minutes == minutes && it != NONE } ?: NONE

        fun label(minutes: Int): String = when {
            minutes <= 0 -> "开始时"
            minutes < 60 -> "开始前 $minutes 分钟"
            minutes % 60 == 0 -> "开始前 ${minutes / 60} 小时"
            else -> "开始前 $minutes 分钟"
        }
    }
}
