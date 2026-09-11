package com.wavachao.timeblock.ui.util

import com.wavachao.timeblock.data.model.TimeBlock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** All the copy formatting the mockup depends on (`09:00 – 10:30 · 1h30m`, `10月24日 · 星期五`). */
object TimeFormat {

    private val HhMm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val ChineseLocale: Locale = Locale.SIMPLIFIED_CHINESE

    fun time(value: LocalTime): String = value.format(HhMm)

    fun time(value: LocalDateTime): String = value.format(HhMm)

    /** `1h30m` / `45m` / `2h` — the compact duration badge. */
    fun compactDuration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val hours = safe / 60
        val rest = safe % 60
        return when {
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h${rest}m"
        }
    }

    fun duration(minutes: Int): String = compactDuration(minutes)

    /** `1 小时 30 分` — used by the quick-add sheet's duration chip. */
    fun longDuration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val hours = safe / 60
        val rest = safe % 60
        return when {
            hours == 0 -> "$rest 分钟"
            rest == 0 -> "$hours 小时"
            else -> "$hours 小时 $rest 分"
        }
    }

    /** `6 小时 20 分`, and `—` for a day with nothing planned. */
    fun longDurationOrDash(minutes: Int): String =
        if (minutes <= 0) "—" else longDuration(minutes)

    /** `09:00 – 10:30` (the mockup uses an en dash surrounded by spaces). */
    fun range(block: TimeBlock): String = "${time(block.start)} – ${time(block.end)}"

    fun range(start: LocalDateTime, end: LocalDateTime): String = "${time(start)} – ${time(end)}"

    /** `10月24日 · 星期五` */
    fun dateWithWeekday(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日 · ${weekday(date)}"

    /** `星期五` */
    fun weekday(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, ChineseLocale)

    /** `周五` */
    fun shortWeekday(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, ChineseLocale)

    /** `10月` */
    fun monthLabel(date: LocalDate): String = "${date.monthValue}月"

    /** `今天` / `明天` / `昨天` / `周六` */
    fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.minusDays(1) -> "昨天"
        else -> shortWeekday(date)
    }

    fun dayNumber(date: LocalDate): String = date.dayOfMonth.toString()

    /** `28h 40m` split for the insights hero. */
    fun metricParts(minutes: Int): Pair<String, String> {
        val hours = minutes / 60
        val rest = minutes % 60
        return hours.toString() to rest.toString().padStart(2, '0')
    }

    fun signedDuration(minutes: Int): String {
        val sign = if (minutes < 0) "-" else ""
        return sign + longDuration(kotlin.math.abs(minutes))
    }

    fun minutesBetween(start: LocalDateTime, end: LocalDateTime): Int =
        Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)

    /** `62%` */
    fun percent(value: Float): String = "${(value * 100f).toInt()}%"

    /** `13:52` — the live pill on the now-line. */
    fun clock(now: LocalDateTime): String = time(now)

    /** `1 分钟` / `34 分钟` / `1 小时 20 分` — countdown copy. */
    fun untilLabel(minutes: Int): String = when {
        minutes <= 0 -> "即将开始"
        minutes < 60 -> "还有 $minutes 分钟"
        else -> "还有 ${longDuration(minutes)}"
    }

    /** `剩余 34 分钟` / `已超时 5 分钟`. */
    fun remainingLabel(minutes: Int): String =
        if (minutes >= 0) "剩余 ${longDuration(minutes)}" else "已超时 ${longDuration(-minutes)}"

    /** `进行中` / `已结束` / `计划中` / `已完成` */
    fun statusLabel(block: TimeBlock, now: LocalDateTime): String = when {
        block.done -> "已完成"
        now.isBefore(block.start) -> "计划中"
        now.isAfter(block.end) -> "未完成"
        else -> "进行中"
    }
}
