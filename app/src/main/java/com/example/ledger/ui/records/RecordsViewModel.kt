package com.example.ledger.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TransactionFilter
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.repository.TagRepository
import com.example.ledger.domain.repository.TransactionRepository
import com.example.ledger.ui.common.RangePreset
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记录列表 ViewModel。
 *
 * 设计要点：
 *  1) 筛选条件收成**一个** [RecordsFilter] 数据类，而不是 4 个独立 StateFlow ——
 *     这样"改筛选"永远只有一次状态更新、一次数据库重查，不会出现中间态（改了类型还没改标签就查了一次）；
 *  2) 列表 → 按天分组 → 组内小计，全部在这里算完，Composable 只负责画；
 *  3) 多选直接就是 `Set<Long>`，空集合即"非多选模式"。
 */
@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(RecordsFilter())
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private val dialog = MutableStateFlow(RecordsDialog())
    private val tagsCache = MutableStateFlow<List<Tag>>(emptyList())

    private val _effects = Channel<RecordsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTags().collect { tagsCache.value = it }
        }
    }

    /** 筛选变化 → 重新查询 → 转 UI 模型（标签信息由 tagsCache 补齐） */
    private val itemsFlow: Flow<List<TransactionUiModel>> = filterState.flatMapLatest { filter ->
        combine(
            transactionRepository.observeTransactions(filter.toFilter()),
            tagsCache,
        ) { transactions, tags ->
            transactions.toUiModels(tags.associateBy { it.id })
        }
    }

    val uiState: StateFlow<RecordsUiState> = combine(
        itemsFlow,
        filterState,
        selection,
        dialog,
        tagsCache,
    ) { items, filter, selectedIds, dialogState, tags ->
        RecordsUiState(
            isLoading = false,
            typeFilter = filter.type,
            tagFilterId = filter.tagId,
            tagFilterName = filter.tagId?.let { id -> tags.firstOrNull { it.id == id }?.name },
            keyword = filter.keyword,
            rangePreset = filter.preset,
            allTags = tags,
            isFilterSheetOpen = filter.isSheetOpen,
            groups = items.groupByDay(),
            totalCount = items.size,
            expenseInCents = items.filter { it.isExpense }.sumOf { it.amountInCents },
            incomeInCents = items.filterNot { it.isExpense }.sumOf { it.amountInCents },
            selection = selectedIds,
            selectedAmountInCents = items.filter { it.id in selectedIds }.sumOf { it.amountInCents },
            deleting = dialogState.deleting,
            batchDeleteRequested = dialogState.batchDelete,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordsUiState(),
    )

    /* ============================================================ 筛选 */

    fun onTypeFilterChange(type: TransactionType?) {
        filterState.value = filterState.value.copy(type = type)
        clearSelection()
    }

    fun onTagFilterChange(tagId: Long?) {
        filterState.value = filterState.value.copy(tagId = tagId)
        clearSelection()
    }

    fun onKeywordChange(keyword: String) {
        filterState.value = filterState.value.copy(keyword = keyword)
        clearSelection()
    }

    fun onRangePresetChange(preset: RangePreset) {
        // 记录列表不提供自定义区间，CUSTOM 落到"全部时间"更符合预期
        val safe = if (preset == RangePreset.CUSTOM) RangePreset.ALL else preset
        filterState.value = filterState.value.copy(preset = safe)
        clearSelection()
    }

    fun onFilterSheetOpenChange(open: Boolean) {
        filterState.value = filterState.value.copy(isSheetOpen = open)
    }

    fun onClearFilters() {
        filterState.value = RecordsFilter()
        clearSelection()
    }

    /* ============================================================ 多选 */

    /** 长按进入多选并选中该项 */
    fun onItemLongPress(item: TransactionUiModel) {
        selection.value = selection.value + item.id
    }

    fun onToggleSelection(id: Long) {
        val current = selection.value
        selection.value = if (id in current) current - id else current + id
    }

    fun onSelectAll() {
        val allIds = uiState.value.groups.flatMap { group -> group.items.map { it.id } }.toSet()
        selection.value = if (selection.value.size >= allIds.size) emptySet() else allIds
    }

    fun onExitSelection() {
        clearSelection()
    }

    private fun clearSelection() {
        if (selection.value.isNotEmpty()) selection.value = emptySet()
    }

    /* ============================================================ 删除 */

    fun onDeleteRequest(item: TransactionUiModel) {
        dialog.value = dialog.value.copy(deleting = item)
    }

    fun onDeleteDismiss() {
        dialog.value = dialog.value.copy(deleting = null)
    }

    fun onDeleteConfirm() {
        val item = dialog.value.deleting ?: return
        dialog.value = dialog.value.copy(deleting = null)
        viewModelScope.launch {
            transactionRepository.deleteById(item.id)
            selection.value = selection.value - item.id
            _effects.send(RecordsEffect.ShowMessage("已删除这笔记录"))
        }
    }

    fun onBatchDeleteRequest() {
        if (selection.value.isEmpty()) return
        dialog.value = dialog.value.copy(batchDelete = true)
    }

    fun onBatchDeleteDismiss() {
        dialog.value = dialog.value.copy(batchDelete = false)
    }

    fun onBatchDeleteConfirm() {
        val ids = selection.value.toList()
        dialog.value = dialog.value.copy(batchDelete = false)
        if (ids.isEmpty()) return

        viewModelScope.launch {
            // 逐条删除：账单量级是"个人记账"（几千条封顶），
            // 逐条走 suspend 比额外开一条 batch DAO 更简单，也不会长事务卡顿
            ids.forEach { id -> transactionRepository.deleteById(id) }
            selection.value = emptySet()
            _effects.send(RecordsEffect.ShowMessage("已删除 ${ids.size} 笔记录"))
        }
    }
}

/** 页面内部筛选条件（真源） */
private data class RecordsFilter(
    val type: TransactionType? = null,
    val tagId: Long? = null,
    val keyword: String = "",
    val preset: RangePreset = RangePreset.ALL,
    val isSheetOpen: Boolean = false,
) {
    fun toFilter(): TransactionFilter = TransactionFilter(
        type = type,
        tagId = tagId,
        keyword = keyword.trim().takeIf { it.isNotEmpty() },
        range = preset.toTimeRange(),
    )
}

/** 页面内部对话框状态 */
private data class RecordsDialog(
    val deleting: TransactionUiModel? = null,
    val batchDelete: Boolean = false,
)
