package com.example.ledger.ui.stats

import androidx.compose.runtime.Immutable
import com.example.ledger.domain.model.TagAmount
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.model.TrendGranularity
import com.example.ledger.domain.model.TrendPoint
import com.example.ledger.domain.model.TypeTotals
import com.example.ledger.ui.common.RangePreset
import java.time.LocalDate

/** 统计页的三个分段：支出 / 收入 / 自定义时间块 */
enum class StatsTab(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    CUSTOM("自定义"),
}

/**
 * 统计页 UI 状态。
 *
 * 图表的"原料"全部已经算好：
 *  - [tagAmounts] 已按金额降序、比例算好 → 环形图 + 排行榜直接画；
 *  - [trend] 已补齐零值桶 → 柱状图 X 轴等距；
 *  - [granularity] 是按区间自动选的，UI 只用来显示"按日/按周/按月"标签。
 *
 * [highlightTagId] 是"图例点击筛选"的联动状态：环形图与排行榜共用它，
 * 所以放在 ViewModel 而不是各自的 Composable 里，两处才不会各选各的。
 */
@Immutable
data class StatsUiState(
    val isLoading: Boolean = true,
    val tab: StatsTab = StatsTab.EXPENSE,
    val preset: RangePreset = RangePreset.THIS_MONTH,
    val rangeLabel: String = "",
    val customStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEnd: LocalDate = LocalDate.now(),
    val customType: TransactionType = TransactionType.EXPENSE,
    val totals: TypeTotals = TypeTotals(),
    val tagAmounts: List<TagAmount> = emptyList(),
    val trend: List<TrendPoint> = emptyList(),
    val granularity: TrendGranularity = TrendGranularity.DAY,
    val highlightTagId: Long? = null,
    val showStartPicker: Boolean = false,
    val showEndPicker: Boolean = false,
) {
    /** 当前页签聚焦的收支类型（决定金额颜色与"支出/收入"文案） */
    val focusType: TransactionType
        get() = when (tab) {
            StatsTab.EXPENSE -> TransactionType.EXPENSE
            StatsTab.INCOME -> TransactionType.INCOME
            StatsTab.CUSTOM -> customType
        }

    /** 焦点总额（环形图中心的数字） */
    val focusTotalInCents: Long
        get() = if (focusType == TransactionType.INCOME) totals.incomeInCents else totals.expenseInCents

    val isEmpty: Boolean get() = tagAmounts.isEmpty() && trend.isEmpty()

    /** 排行榜头部占比的分母，避免 UI 再算一遍 */
    val tagTotalInCents: Long get() = tagAmounts.sumOf { it.amount }
}

/** 一次性事件 */
sealed interface StatsEffect {
    data class ShowMessage(val message: String) : StatsEffect
}
