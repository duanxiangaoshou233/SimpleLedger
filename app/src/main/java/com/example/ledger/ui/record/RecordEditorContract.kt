package com.example.ledger.ui.record

import androidx.compose.runtime.Immutable
import com.example.ledger.core.util.AmountInputState
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TransactionType

/**
 * 记账/编辑面板的 UI 状态。
 *
 * 这是一个**全局共享**的面板：首页、记录列表、统计页都可以唤起它，
 * 因此它由 `RecordEditorViewModel` 统一持有（在 Activity 作用域），
 * 面板本身在 `LedgerApp` 里只渲染一次，避免"每个页面各写一套编辑逻辑"。
 *
 * @param visible       是否展开（false 时 Sheet 会走退场动画）
 * @param isNew         id == 0 即为新增
 * @param availableTags 按当前收支类型过滤后的可选标签（Chip 列表）
 * @param amountInput   自定义键盘的输入状态机
 * @param newTagDraft   非 null 时表示"现场新建标签"弹窗打开
 */
@Immutable
data class RecordEditorUiState(
    val visible: Boolean = false,
    val id: Long = 0L,
    val isNew: Boolean = true,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: AmountInputState = AmountInputState(),
    val dateTime: Long = 0L,
    val tagId: Long? = null,
    val note: String = "",
    val availableTags: List<Tag> = emptyList(),
    val colorChoices: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val amountError: String? = null,
    val newTagDraft: NewTagDraftUiState? = null,
    /** 展示用的日期文案，如 "2024-06-01" */
    val dateText: String = "",
    /** 展示用的时间文案，如 "14:30" */
    val timeText: String = "",
    /** "星期六" */
    val weekdayText: String = "",
) {
    val amountInCents: Long get() = amountInput.cents

    /** 标题：新增/编辑 */
    val title: String get() = if (isNew) "记一笔" else "编辑记录"

    /** 是否允许保存：金额必须 > 0，且当前没有正在进行的保存 */
    val canSave: Boolean get() = amountInCents > 0L && !isSaving
}

/** 现场新建标签的弹窗状态 */
@Immutable
data class NewTagDraftUiState(
    val name: String = "",
    val colorHex: String = "#2E9E8F",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** 一次性事件（Snackbar / 关闭动画等），用 Channel 派发，不做成状态 */
sealed interface RecordEditorEffect {
    data class ShowMessage(val message: String) : RecordEditorEffect
}
