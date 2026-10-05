package com.example.ledger.ui.records

import androidx.compose.runtime.Immutable
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.ui.common.RangePreset
import com.example.ledger.ui.common.TransactionDayGroup
import com.example.ledger.ui.common.TransactionUiModel

/**
 * 记录列表页 UI 状态（按天分组的完整账单列表 + 筛选 + 多选）。
 *
 * 关于多选：`selection` 为空 = 普通浏览模式；非空 = 多选模式。
 * 这样"是否处于多选"不需要额外的布尔字段，永远不会和选中集合不同步。
 */
@Immutable
data class RecordsUiState(
    val isLoading: Boolean = true,

    // ---------------- 筛选条件 ----------------
    val typeFilter: TransactionType? = null,
    val tagFilterId: Long? = null,
    /** 已选标签名（用于顶部筛选 Chip 的文案） */
    val tagFilterName: String? = null,
    val keyword: String = "",
    val rangePreset: RangePreset = RangePreset.ALL,
    val allTags: List<Tag> = emptyList(),
    val isFilterSheetOpen: Boolean = false,

    // ---------------- 列表数据 ----------------
    val groups: List<TransactionDayGroup> = emptyList(),
    val totalCount: Int = 0,
    val expenseInCents: Long = 0L,
    val incomeInCents: Long = 0L,

    // ---------------- 多选与删除 ----------------
    val selection: Set<Long> = emptySet(),
    val selectedAmountInCents: Long = 0L,
    val deleting: TransactionUiModel? = null,
    val batchDeleteRequested: Boolean = false,
) {
    val isSelectionMode: Boolean get() = selection.isNotEmpty()
    val selectedCount: Int get() = selection.size
    val isEmpty: Boolean get() = groups.isEmpty()

    /** 是否处于"已筛选"状态（决定是否显示"清除筛选"入口） */
    val hasFilter: Boolean
        get() = typeFilter != null ||
            tagFilterId != null ||
            keyword.isNotBlank() ||
            rangePreset != RangePreset.ALL

    /** 当前列表里是否全部被选中（决定全选按钮的语义） */
    val allSelected: Boolean
        get() = totalCount > 0 && selection.size >= totalCount
}

/** 一次性事件 */
sealed interface RecordsEffect {
    data class ShowMessage(val message: String) : RecordsEffect
}
