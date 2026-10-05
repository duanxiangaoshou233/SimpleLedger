package com.example.ledger.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * 左闭右开的时间区间：`[startMillis, endMillisExclusive)`。
 *
 * 为什么用"左闭右开"而不是"闭区间"？
 *  - 相邻区间天然无缝拼接（今天 24:00 == 明天 00:00），不会重复也不会漏掉边界那一毫秒；
 *  - SQL 里只需 `dateTime >= :start AND dateTime < :end`，比 BETWEEN + 23:59:59.999 干净。
 */
data class TimeRange(
    val startMillis: Long,
    val endMillisExclusive: Long,
) {
    /** 区间长度（毫秒） */
    val durationMillis: Long get() = endMillisExclusive - startMillis

    /** 大致天数（向上取整），用于自动选择趋势图的粒度 */
    val dayCount: Long
        get() = if (endMillisExclusive == Long.MAX_VALUE) Long.MAX_VALUE
        else ((durationMillis - 1).coerceAtLeast(0) / 86_400_000L) + 1

    fun contains(millis: Long): Boolean = millis >= startMillis && millis < endMillisExclusive

    /** 用于 UI 判断"是否处于不限时间"的状态 */
    val isAll: Boolean get() = this == All

    companion object {
        /** 不限时间。start 取 0（1970），end 取 Long.MAX_VALUE 以覆盖所有真实数据 */
        val All = TimeRange(0L, Long.MAX_VALUE)

        /** 某一天 00:00:00 ~ 次日 00:00:00 */
        fun ofDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TimeRange =
            TimeRange(date.startOfDayMillis(zone), date.plusDays(1).startOfDayMillis(zone))

        /** 某一周（**周一为一周之始**，符合国内习惯） */
        fun ofWeek(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TimeRange {
            val monday = date.with(DayOfWeek.MONDAY)
            return TimeRange(monday.startOfDayMillis(zone), monday.plusWeeks(1).startOfDayMillis(zone))
        }

        /** 某个月 */
        fun ofMonth(year: Int, month: Int, zone: ZoneId = ZoneId.systemDefault()): TimeRange {
            val first = LocalDate.of(year, month, 1)
            return TimeRange(first.startOfDayMillis(zone), first.plusMonths(1).startOfDayMillis(zone))
        }

        fun ofMonth(yearMonth: java.time.YearMonth, zone: ZoneId = ZoneId.systemDefault()): TimeRange =
            ofMonth(yearMonth.year, yearMonth.monthValue, zone)

        /** 某一年 */
        fun ofYear(year: Int, zone: ZoneId = ZoneId.systemDefault()): TimeRange {
            val first = LocalDate.of(year, 1, 1)
            return TimeRange(first.startOfDayMillis(zone), first.plusYears(1).startOfDayMillis(zone))
        }

        /**
         * 自定义时间块：**含首含尾**（用户选 6/1 ~ 6/30，就应该包含 6/30 全天）。
         * 内部自动把结束日期 +1 天，转换成左闭右开区间。
         */
        fun custom(
            startDate: LocalDate,
            endDateInclusive: LocalDate,
            zone: ZoneId = ZoneId.systemDefault(),
        ): TimeRange {
            val from = minOf(startDate, endDateInclusive)
            val to = maxOf(startDate, endDateInclusive)
            return TimeRange(from.startOfDayMillis(zone), to.plusDays(1).startOfDayMillis(zone))
        }
    }
}

/** 趋势图的统计粒度 */
enum class TrendGranularity(val label: String) {
    DAY("按日"),
    WEEK("按周"),
    MONTH("按月"),
    YEAR("按年"),
    ;

    companion object {
        /**
         * 根据区间自动选择粒度，避免出现"365 根柱子"这种不可读的图：
         *  ≤ 31 天   → 按日
         *  ≤ 180 天  → 按周
         *  ≤ 3 年    → 按月
         *  更长       → 按年
         */
        fun autoFor(range: TimeRange): TrendGranularity = when {
            range.dayCount <= 31L -> DAY
            range.dayCount <= 180L -> WEEK
            range.dayCount <= 1095L -> MONTH
            else -> YEAR
        }
    }
}

/** 某个标签在统计区间内的汇总 */
data class TagAmount(
    /** null 表示"无标签"这一虚拟分组 */
    val tagId: Long?,
    val tagName: String,
    val colorHex: String,
    /** 汇总金额（分，正数） */
    val amount: Long,
    /** 笔数 */
    val count: Int,
    /** 占总额比例 0f~1f，由 Repository 统一算好，UI 直接用 */
    val ratio: Float,
) {
    companion object {
        /** "无标签"分组的展示色（灰调，不占用图表色板） */
        const val NO_TAG_COLOR_HEX = "#9AA5A0"
        const val NO_TAG_NAME = "无标签"
    }
}

/** 趋势图的一个数据点（一根柱子） */
data class TrendPoint(
    /** 该柱子所属桶的起点（epoch millis），可用于点击柱子查看明细 */
    val bucketStartMillis: Long,
    /** 展示标签：按日 "6/1"，按周 "6/1周"，按月 "6月"，按年 "2024" */
    val label: String,
    /** 金额（分，正数） */
    val amount: Long,
)

/** 某区间内收入 / 支出的总额（首页 Hero 卡与统计页头部用） */
data class TypeTotals(
    val expenseInCents: Long = 0L,
    val incomeInCents: Long = 0L,
) {
    /** 结余 = 收入 - 支出 */
    val balanceInCents: Long get() = incomeInCents - expenseInCents

    val isEmpty: Boolean get() = expenseInCents == 0L && incomeInCents == 0L
}

/** 把 LocalDate 转成当天 00:00 的 epoch millis（放在这里避免依赖 util 层） */
private fun LocalDate.startOfDayMillis(zone: ZoneId): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()
