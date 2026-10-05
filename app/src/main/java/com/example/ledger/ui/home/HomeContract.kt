package com.example.ledger.ui.home

import androidx.compose.runtime.Immutable
import com.example.ledger.domain.model.TypeTotals
import com.example.ledger.ui.common.TransactionDayGroup

/**
 * 首页（记账 Tab）UI 状态。
 *
 * 只包含"页面渲染需要的一切"，不含任何可变逻辑：Hero 卡数据 + 最近记录 + 对话框状态。
 * 真正的编辑动作由全局的 `RecordEditorViewModel` 负责，首页只负责"唤起"。
 *
 * 最近记录同样是**按天分组**的（和记录列表一致的视觉语言），
 * 因此这里直接持有分好组的数据，Composable 不再做分组计算。
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    /** "2024年6月" */
    val monthLabel: String = "",
    /** 是否就是当前自然月（决定标题显示"本月支出"还是具体月份） */
    val isCurrentMonth: Boolean = true,
    /** 是否允许往未来翻月（不允许超过当前月） */
    val canGoNextMonth: Boolean = false,
    val totals: TypeTotals = TypeTotals(),
    /** 最近记录（按天分组，时间倒序） */
    val recentGroups: List<TransactionDayGroup> = emptyList(),
    /** 最近记录的笔数（首页标题右侧"共 N 笔"） */
    val recentCount: Int = 0,
    /** 非 null 表示"确认删除"对话框打开 */
    val deleting: TransactionUiModel? = null,
) {
    /** 首次使用（一笔账都没有）→ 显示空状态插画 */
    val isEmpty: Boolean get() = recentGroups.isEmpty()
}

/** 一次性事件 */
sealed interface HomeEffect {
    data class ShowMessage(val message: String) : HomeEffect
}
