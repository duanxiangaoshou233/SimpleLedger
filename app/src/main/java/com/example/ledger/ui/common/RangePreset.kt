package com.example.ledger.ui.common

import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.domain.model.TimeRange
import java.time.LocalDate

/**
 * 时间范围的快捷选项（记录列表筛选 + 统计页共用）。
 *
 * 只放"能算出来的"预设；[CUSTOM] 需要两个日期，由各自的 ViewModel 单独维护，
 * 这样这个枚举保持无状态、可复制粘贴到任何地方。
 */
enum class RangePreset(val label: String, val shortLabel: String) {
    ALL("全部时间", "全部"),
    TODAY("今天", "今天"),
    THIS_WEEK("本周", "本周"),
    THIS_MONTH("本月", "本月"),
    THIS_YEAR("今年", "今年"),
    CUSTOM("自定义", "自定义"),
    ;

    /** 计算实际区间；每次调用都取"当前时间"，所以跨零点后会自动滚动 */
    fun toTimeRange(today: LocalDate = LocalDate.now(DateTimeUtils.zone)): TimeRange = when (this) {
        ALL -> TimeRange.All
        TODAY -> TimeRange.ofDay(today)
        THIS_WEEK -> TimeRange.ofWeek(today)
        THIS_MONTH -> TimeRange.ofMonth(java.time.YearMonth.of(today.year, today.monthValue))
        THIS_YEAR -> TimeRange.ofYear(today.year)
        // 自定义区间的真实范围由 ViewModel 提供，这里退化成本月兜底，避免返回非法区间
        CUSTOM -> TimeRange.ofMonth(java.time.YearMonth.of(today.year, today.monthValue))
    }

    /** 记录列表页只提供这几个预设（不需要自定义时间块） */
    companion object {
        val listPresets: List<RangePreset> = listOf(ALL, TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR)
        val statsPresets: List<RangePreset> = listOf(TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM)
    }
}
