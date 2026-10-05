package com.example.ledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TimeRange
import com.example.ledger.domain.model.TypeTotals
import com.example.ledger.domain.repository.TagRepository
import com.example.ledger.domain.repository.TransactionRepository
import com.example.ledger.ui.common.TransactionUiModel
import com.example.ledger.ui.common.groupByDay
import com.example.ledger.ui.common.toUiModels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * 首页 ViewModel。
 *
 * 数据流（全部由 Room 的 Flow 驱动，任何写入都会自动回流，无需手工刷新）：
 *
 *   monthOffset ──flatMapLatest──> observeTotals(该月) ──┐
 *   observeRecent(8) ──combine(tagsCache)───────────────┼──combine──> HomeUiState
 *   deleting / monthOffset ─────────────────────────────┘
 *
 * 注意 combine 最多支持 5 个流的重载，这里用到 4 个，留有余量。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 0 = 本月，-1 = 上月，-2 = 上上月（不允许为正，未来没有账可看） */
    private val monthOffset = MutableStateFlow(0)

    private val deleting = MutableStateFlow<TransactionUiModel?>(null)

    /** 标签缓存：把 tagId 翻译成"名称 + 颜色"用，避免列表 N+1 查询 */
    private val tagsCache = MutableStateFlow<List<Tag>>(emptyList())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTags().collect { tagsCache.value = it }
        }
    }

    /** 月份切换 → 该月收支总额 */
    private val monthFlow: Flow<MonthSummary> = monthOffset.flatMapLatest { offset ->
        val yearMonth = YearMonth.now().plusMonths(offset.toLong())
        val range = TimeRange.ofMonth(yearMonth)
        transactionRepository.observeTotals(range).map { totals ->
            MonthSummary(
                label = "${yearMonth.year}年${yearMonth.monthValue}月",
                isCurrentMonth = offset == 0,
                totals = totals,
            )
        }
    }

    /** 最近 N 笔（列表带标签信息） */
    private val recentFlow: Flow<List<TransactionUiModel>> =
        combine(
            transactionRepository.observeRecent(RECENT_LIMIT),
            tagsCache,
        ) { transactions, tags ->
            transactions.toUiModels(tags.associateBy { it.id })
        }

    val uiState: StateFlow<HomeUiState> = combine(
        monthFlow,
        recentFlow,
        deleting,
        monthOffset,
    ) { month, recent, deletingItem, offset ->
        HomeUiState(
            isLoading = false,
            monthLabel = month.label,
            isCurrentMonth = month.isCurrentMonth,
            canGoNextMonth = offset < 0,
            totals = month.totals,
            recentGroups = recent.groupByDay(),
            recentCount = recent.size,
            deleting = deletingItem,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /* ============================================================ 月份切换 */

    fun onPreviousMonth() {
        monthOffset.value = monthOffset.value - 1
    }

    /** 不允许翻到未来月份：到底了就忽略（UI 上按钮同时也会置灰） */
    fun onNextMonth() {
        if (monthOffset.value < 0) monthOffset.value = monthOffset.value + 1
    }

    /* ============================================================ 删除 */

    fun onDeleteRequest(item: TransactionUiModel) {
        deleting.value = item
    }

    fun onDeleteDismiss() {
        deleting.value = null
    }

    fun onDeleteConfirm() {
        val item = deleting.value ?: return
        deleting.value = null
        viewModelScope.launch {
            transactionRepository.deleteById(item.id)
            _effects.send(HomeEffect.ShowMessage("已删除这笔记录"))
        }
    }

    private data class MonthSummary(
        val label: String,
        val isCurrentMonth: Boolean,
        val totals: TypeTotals,
    )

    private companion object {
        const val RECENT_LIMIT = 8
    }
}
