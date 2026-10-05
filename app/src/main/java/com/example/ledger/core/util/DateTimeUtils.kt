package com.example.ledger.core.util

import com.example.ledger.domain.model.TimeRange
import com.example.ledger.domain.model.TrendGranularity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 日期时间工具。
 *
 * 为什么可以放心用 `java.time`？
 *  - minSdk = 26，`java.time` 从 API 26 开始就是系统自带的，不需要 desugaring；
 *  - ZoneId.systemDefault() 会跟随用户"改时区/夏令时"，比手工算 offset 可靠。
 *
 * 约定：数据库里存的一律是 epoch millis；本文件负责"人话"和"区间"的相互转换。
 */
object DateTimeUtils {

    val zone: ZoneId get() = ZoneId.systemDefault()

    // 线程安全的 DateTimeFormatter 可以安全地做成常量
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeCsvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val timeCsvFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val monthDayShortFormatter = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)
    private val monthFormatter = DateTimeFormatter.ofPattern("M月", Locale.CHINA)
    private val yearFormatter = DateTimeFormatter.ofPattern("yyyy年", Locale.CHINA)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)

    // ------------------------------------------------------------------ 转换

    fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    fun Long.toLocalDateTime(): LocalDateTime = Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

    fun LocalDate.startOfDayMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

    fun LocalDateTime.toEpochMillis(): Long = atZone(zone).toInstant().toEpochMilli()

    /** 当前时刻（毫秒），统一入口方便以后注入假时钟做测试 */
    fun now(): Long = System.currentTimeMillis()

    // ------------------------------------------------------------------ 展示

    /** "2024年6月1日 14:30" */
    fun formatDateTime(millis: Long): String = millis.toLocalDateTime().format(dateTimeFormatter)

    /** "2024-06-01" */
    fun formatDate(millis: Long): String = millis.toLocalDate().format(dateFormatter)

    /** CSV 的日期列 */
    fun formatCsvDate(millis: Long): String = millis.toLocalDate().format(dateTimeCsvFormatter)

    /** CSV 的时间列 */
    fun formatCsvTime(millis: Long): String = millis.toLocalDateTime().format(timeCsvFormatter)

    /** "6月1日" */
    fun formatMonthDay(millis: Long): String = millis.toLocalDate().format(monthDayFormatter)

    /** "6/1"（图表轴用，短） */
    fun formatMonthDayShort(millis: Long): String = millis.toLocalDate().format(monthDayShortFormatter)

    /** "6月" */
    fun formatMonth(millis: Long): String = millis.toLocalDate().format(monthFormatter)

    /** "2024年" */
    fun formatYear(millis: Long): String = millis.toLocalDate().format(yearFormatter)

    /** "14:30" */
    fun formatTime(millis: Long): String = millis.toLocalDateTime().format(timeFormatter)

    /** "星期六" */
    fun formatWeekday(millis: Long): String = millis.toLocalDate().format(weekdayFormatter)

    /**
     * 列表分组标题：今天 / 昨天 / 6月1日 / 2023年6月1日（跨年时补年份）
     */
    fun friendlyDayLabel(millis: Long, today: LocalDate = LocalDate.now(zone)): String {
        val date = millis.toLocalDate()
        return when {
            date == today -> "今天"
            date == today.minusDays(1) -> "昨天"
            date == today.minusDays(2) -> "前天"
            date.year == today.year -> date.format(monthDayFormatter)
            else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        }
    }

    /** 时间区间的人话描述，统计页标题用 */
    fun formatRangeLabel(range: TimeRange): String {
        if (range.isAll) return "全部时间"
        val start = range.startMillis.toLocalDate()
        // 右开区间 -> 最后一个"被包含的日期"
        val lastDay = range.endMillisExclusive.toLocalDate().minusDays(1)
        return when {
            start == lastDay ->
                "${start.year}年${start.monthValue}月${start.dayOfMonth}日"

            start.year == lastDay.year ->
                "${start.monthValue}月${start.dayOfMonth}日 - " +
                    "${lastDay.monthValue}月${lastDay.dayOfMonth}日"

            else ->
                "${start.year}年${start.monthValue}月${start.dayOfMonth}日 - " +
                    "${lastDay.year}年${lastDay.monthValue}月${lastDay.dayOfMonth}日"
        }
    }

    /** 趋势柱子的标签，按粒度给不同精度 */
    fun trendLabel(bucketStartMillis: Long, granularity: TrendGranularity): String {
        val date = bucketStartMillis.toLocalDate()
        return when (granularity) {
            TrendGranularity.DAY -> date.format(monthDayShortFormatter)
            TrendGranularity.WEEK -> date.format(monthDayShortFormatter)
            TrendGranularity.MONTH -> date.format(monthFormatter)
            TrendGranularity.YEAR -> "${date.year}"
        }
    }

    // ------------------------------------------------------------------ 快捷区间

    fun todayRange(): TimeRange = TimeRange.ofDay(LocalDate.now(zone), zone)

    fun thisWeekRange(): TimeRange = TimeRange.ofWeek(LocalDate.now(zone), zone)

    fun thisMonthRange(): TimeRange = TimeRange.ofMonth(java.time.YearMonth.now(zone), zone)

    fun thisYearRange(): TimeRange = TimeRange.ofYear(LocalDate.now(zone).year, zone)

    /** 近 N 天（含今天） */
    fun lastDaysRange(days: Int): TimeRange {
        val today = LocalDate.now(zone)
        val start = today.minusDays((days - 1).coerceAtLeast(0).toLong())
        return TimeRange(start.startOfDayMillis(), today.plusDays(1).startOfDayMillis())
    }

    // ------------------------------------------------------------------ 趋势分桶
    // 说明：这两个函数刻意写成"普通成员函数 + 数值参数"而不是扩展函数，
    // 这样数据层可以直接 DateTimeUtils.bucketStart(...) 调用，不依赖导入成员扩展，
    // 可读性和可测试性都更好。

    /** 把任意时刻归一到它所属桶的起点（周一 / 1 号 / 1 月 1 日 / 当天 00:00） */
    fun bucketStart(millis: Long, granularity: TrendGranularity): Long {
        val date = millis.toLocalDate()
        val firstDate = when (granularity) {
            TrendGranularity.DAY -> date
            TrendGranularity.WEEK -> date.with(DayOfWeek.MONDAY)
            TrendGranularity.MONTH -> date.withDayOfMonth(1)
            TrendGranularity.YEAR -> date.withDayOfYear(1)
        }
        return firstDate.startOfDayMillis()
    }

    /** 桶起点的下一个桶起点 */
    fun nextBucketStart(bucketStartMillis: Long, granularity: TrendGranularity): Long {
        val date = bucketStartMillis.toLocalDate()
        val nextDate = when (granularity) {
            TrendGranularity.DAY -> date.plusDays(1)
            TrendGranularity.WEEK -> date.plusWeeks(1)
            TrendGranularity.MONTH -> date.plusMonths(1)
            TrendGranularity.YEAR -> date.plusYears(1)
        }
        return nextDate.startOfDayMillis()
    }
}
