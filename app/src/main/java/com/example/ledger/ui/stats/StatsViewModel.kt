package com.example.ledger.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.domain.model.TagAmount
import com.example.ledger.domain.model.TimeRange
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.model.TrendGranularity
import com.example.ledger.domain.model.TrendPoint
import com.example.ledger.domain.model.TypeTotals
import com.example.ledger.domain.repository.TransactionRepository
import com.example.ledger.ui.common.RangePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/**
 * 统计页 ViewModel。
 *
 * 每次"区间 / 页签"变化，都会重新拉三份数据（总额、标签汇总、趋势），
 * 三者放在**同一个 flow 里顺序取**，保证它们一定来自同一个区间快照
 * （如果拆成三个流再 combine，用户快速连点时会看到"总额是本月、环形图是上月"的错位）。
 *
 * 这里的重算频率极低（用户主动切换），所以不做缓存，简单直接。
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val config = MutableStateFlow(StatsConfig())
    private val highlight = MutableStateFlow<Long?>(null)
    private val pickers = MutableStateFlow(PickerState())
    private val loading = MutableStateFlow(true)

    private val _effects = Channel<StatsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val dataFlow: Flow<StatsData> = config.flatMapLatest { cfg ->
        flow {
            loading.value = true
            val range = cfg.resolveRange()
            val type = cfg.resolveType()
            val granularity = TrendGranularity.autoFor(range)
            emit(
                StatsData(
                    totals = transactionRepository.totals(range),
                    tagAmounts = transactionRepository.sumByTag(type, range),
                    trend = transactionRepository.trend(type, range, granularity),
                    range = range,
                    granularity = granularity,
                ),
            )
            loading.value = false
        }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        dataFlow,
        config,
        highlight,
        pickers,
        loading,
    ) { data, cfg, highlightedTag, pickerState, isLoading ->
        StatsUiState(
            isLoading = isLoading,
            tab = cfg.tab,
            preset = cfg.preset,
            rangeLabel = DateTimeUtils.formatRangeLabel(data.range),
            customStart = cfg.customStart,
            customEnd = cfg.customEnd,
            customType = cfg.customType,
            totals = data.totals,
            tagAmounts = data.tagAmounts,
            trend = data.trend,
            granularity = data.granularity,
            highlightTagId = highlightedTag,
            showStartPicker = pickerState.startPicker,
            showEndPicker = pickerState.endPicker,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    /* ============================================================ 页签与区间 */

    fun onTabChange(tab: StatsTab) {
        val current = config.value
        config.value = when (tab) {
            // 进入"自定义"页签时自动切到自定义区间；离开时回到本月，避免下次进来看到空图
            StatsTab.CUSTOM -> current.copy(tab = tab, preset = RangePreset.CUSTOM)
            else -> current.copy(
                tab = tab,
                preset = if (current.preset == RangePreset.CUSTOM) RangePreset.THIS_MONTH else current.preset,
            )
        }
        highlight.value = null
    }

    fun onPresetChange(preset: RangePreset) {
        config.value = config.value.copy(
            tab = if (preset == RangePreset.CUSTOM) StatsTab.CUSTOM else config.value.tab,
            preset = preset,
        )
        highlight.value = null
    }

    fun onCustomTypeChange(type: TransactionType) {
        config.value = config.value.copy(customType = type)
        highlight.value = null
    }

    /** 自定义时间块的起止日期（含首含尾，内部自动转成左闭右开区间） */
    fun onCustomStartChange(date: LocalDate) {
        val current = config.value
        // 起点晚于终点时自动把终点拉过来，避免出现空区间让用户困惑
        val end = if (date > current.customEnd) date else current.customEnd
        config.value = current.copy(tab = StatsTab.CUSTOM, preset = RangePreset.CUSTOM, customStart = date, customEnd = end)
        highlight.value = null
    }

    fun onCustomEndChange(date: LocalDate) {
        val current = config.value
        val start = if (date < current.customStart) date else current.customStart
        config.value = current.copy(tab = StatsTab.CUSTOM, preset = RangePreset.CUSTOM, customStart = start, customEnd = date)
        highlight.value = null
    }

    /** 自定义页签里的"快捷选项"：本周 / 本月 / 今年（终点固定为今天） */
    fun onQuickRange(preset: RangePreset) {
        val today = LocalDate.now(DateTimeUtils.zone)
        val start = when (preset) {
            RangePreset.THIS_WEEK -> today.with(DayOfWeek.MONDAY)
            RangePreset.THIS_MONTH -> today.withDayOfMonth(1)
            RangePreset.THIS_YEAR -> today.withDayOfYear(1)
            else -> today
        }
        config.value = config.value.copy(
            tab = StatsTab.CUSTOM,
            preset = RangePreset.CUSTOM,
            customStart = start,
            customEnd = today,
        )
        highlight.value = null
    }

    /* ============================================================ 图表联动与日期弹窗 */

    /** 点击图例/排行榜：再次点击同一个则取消高亮 */
    fun onHighlightTag(tagId: Long?) {
        highlight.value = if (tagId != null && highlight.value == tagId) null else tagId
    }

    fun onStartPickerOpenChange(open: Boolean) {
        pickers.value = pickers.value.copy(startPicker = open)
    }

    fun onEndPickerOpenChange(open: Boolean) {
        pickers.value = pickers.value.copy(endPicker = open)
    }

    /* ============================================================ 内部数据类 */

    private data class StatsConfig(
        val tab: StatsTab = StatsTab.EXPENSE,
        val preset: RangePreset = RangePreset.THIS_MONTH,
        val customStart: LocalDate = LocalDate.now(DateTimeUtils.zone).withDayOfMonth(1),
        val customEnd: LocalDate = LocalDate.now(DateTimeUtils.zone),
        val customType: TransactionType = TransactionType.EXPENSE,
    ) {
        fun resolveRange(): TimeRange =
            if (preset == RangePreset.CUSTOM) TimeRange.custom(customStart, customEnd)
            else preset.toTimeRange()

        fun resolveType(): TransactionType = when (tab) {
            StatsTab.EXPENSE -> TransactionType.EXPENSE
            StatsTab.INCOME -> TransactionType.INCOME
            StatsTab.CUSTOM -> customType
        }
    }

    private data class PickerState(
        val startPicker: Boolean = false,
        val endPicker: Boolean = false,
    )

    private data class StatsData(
        val totals: TypeTotals,
        val tagAmounts: List<TagAmount>,
        val trend: List<TrendPoint>,
        val range: TimeRange,
        val granularity: TrendGranularity,
    )
}
