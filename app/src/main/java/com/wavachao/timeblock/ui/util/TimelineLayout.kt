package com.wavachao.timeblock.ui.util

import com.wavachao.timeblock.data.model.TimeBlock
import kotlin.math.max
import kotlin.math.min

/**
 * Where a block sits on the day timeline.
 *
 * The timeline is an absolute-positioned canvas with a fixed 46dp hour row (same maths
 * the mockup uses: `09:00 -> top:46px`), so this is the only place that geometry lives.
 */
data class LaidOutBlock(
    val block: TimeBlock,
    /** Minutes from the timeline's first hour. */
    val offsetMinutes: Int,
    val durationMinutes: Int,
    /** Column index and count for overlapping blocks. */
    val lane: Int = 0,
    val laneCount: Int = 1,
) {
    val endOffsetMinutes: Int get() = offsetMinutes + durationMinutes
}

data class TimelineLayout(
    /** First hour rendered, inclusive (08 = 08:00). */
    val startHour: Int,
    /** Last hour rendered, exclusive (20 = up to 20:00). */
    val endHour: Int,
    val blocks: List<LaidOutBlock>,
    /** Idle stretches of at least [minGapMinutes] between consecutive blocks. */
    val gaps: List<IdleGap>,
) {
    val totalHours: Int get() = endHour - startHour
    val totalMinutes: Int get() = totalHours * 60

    /** Vertical fraction 0..1 of a given minute-of-day, for the live "now" line. */
    fun fractionFor(minuteOfDay: Int): Float {
        val fromStart = minuteOfDay - startHour * 60
        return (fromStart.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
    }

    fun containsMinute(minuteOfDay: Int): Boolean =
        minuteOfDay in (startHour * 60)..(endHour * 60)
}

data class IdleGap(
    val startMinutes: Int,
    val endMinutes: Int,
) {
    val minutes: Int get() = endMinutes - startMinutes
}

/**
 * Builds the timeline for [blocks] on one day.
 *
 * Default window is 08:00-20:00 exactly like the mockup, but it stretches to contain
 * anything planned outside that window so no block is ever invisible.
 */
fun buildTimeline(
    blocks: List<TimeBlock>,
    defaultStartHour: Int = 8,
    defaultEndHour: Int = 20,
    minGapMinutes: Int = 25,
): TimelineLayout {
    val ordered = blocks.sortedBy { it.start }
    if (ordered.isEmpty()) {
        return TimelineLayout(defaultStartHour, defaultEndHour, emptyList(), emptyList())
    }

    val earliest = ordered.minOf { it.start.hour }
    val latestEnd = ordered.maxOf { it.end.hour + if (it.end.minute > 0) 1 else 0 }
    val startHour = min(defaultStartHour, earliest).coerceIn(0, 23)
    val endHour = max(defaultEndHour, latestEnd).coerceIn(startHour + 1, 24)

    val laid = ordered.map { block ->
        val offset = minutesFromWindowStart(block, startHour)
        val duration = TimeFormat.minutesBetween(block.start, block.end).coerceAtLeast(1)
        LaidOutBlock(block = block, offsetMinutes = offset, durationMinutes = duration)
    }

    return TimelineLayout(
        startHour = startHour,
        endHour = endHour,
        blocks = assignLanes(laid),
        gaps = findGaps(laid, minGapMinutes),
    )
}

private fun minutesFromWindowStart(block: TimeBlock, startHour: Int): Int =
    block.start.hour * 60 + block.start.minute - startHour * 60

/**
 * Column assignment for overlapping blocks. The mockup has none, but a real planner
 * will, and stacking them on top of each other silently loses information.
 */
internal fun assignLanes(blocks: List<LaidOutBlock>): List<LaidOutBlock> {
    val result = mutableListOf<LaidOutBlock>()
    var cluster = mutableListOf<LaidOutBlock>()
    var clusterEnd = Int.MIN_VALUE

    fun flush() {
        if (cluster.isEmpty()) return
        val laneCount = cluster.maxOf { it.lane } + 1
        cluster.forEach { result += it.copy(laneCount = laneCount) }
        cluster = mutableListOf()
    }

    blocks.forEach { item ->
        if (cluster.isNotEmpty() && item.offsetMinutes >= clusterEnd) flush()
        val taken = cluster.filter { item.offsetMinutes < it.endOffsetMinutes }
            .map { it.lane }
            .toSet()
        var lane = 0
        while (lane in taken) lane++
        val placed = item.copy(lane = lane)
        cluster += placed
        clusterEnd = max(clusterEnd, placed.endOffsetMinutes)
    }
    flush()
    return result
}

private fun findGaps(blocks: List<LaidOutBlock>, minGapMinutes: Int): List<IdleGap> {
    if (blocks.size < 2) return emptyList()
    val gaps = mutableListOf<IdleGap>()
    for (index in 0 until blocks.size - 1) {
        val current = blocks[index]
        val next = blocks[index + 1]
        val gap = next.offsetMinutes - current.endOffsetMinutes
        if (gap >= minGapMinutes) {
            gaps += IdleGap(current.endOffsetMinutes, next.offsetMinutes)
        }
    }
    return gaps
}
