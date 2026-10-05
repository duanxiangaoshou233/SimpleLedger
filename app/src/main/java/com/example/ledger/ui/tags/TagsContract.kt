package com.example.ledger.ui.tags

import androidx.compose.runtime.Immutable
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TagType

/**
 * 标签管理页 UI 状态。
 *
 * 列表已按 [TagType] 分好组（支出 → 收入 → 通用），每组带标题与数量；
 * 编辑/删除都是"非 null 即打开"的对话框状态，避免再多一层布尔开关。
 */
@Immutable
data class TagsUiState(
    val isLoading: Boolean = true,
    val groups: List<TagGroupUi> = emptyList(),
    val totalCount: Int = 0,
    val editor: TagEditorUiState? = null,
    val deleteDialog: TagDeleteUiState? = null,
    val colorChoices: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

/** 一个分组（支出标签 / 收入标签 / 通用标签） */
@Immutable
data class TagGroupUi(
    val type: TagType,
    val title: String,
    val items: List<TagItemUi>,
)

/** 标签行：标签本体 + 使用次数 */
@Immutable
data class TagItemUi(
    val tag: Tag,
    val usageCount: Int,
) {
    val isUsed: Boolean get() = usageCount > 0
}

/** 新建 / 编辑标签对话框状态 */
@Immutable
data class TagEditorUiState(
    val id: Long = 0L,
    val name: String = "",
    val colorHex: String = "#2E9E8F",
    val type: TagType = TagType.EXPENSE,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isNew: Boolean get() = id == 0L
    val title: String get() = if (isNew) "新建标签" else "编辑标签"
}

/**
 * 删除标签确认对话框状态。
 *
 * 删除是唯一"会牵动其它数据"的操作，因此对话框必须把后果说清楚：
 *  - [usageCount] == 0：直接确认删除即可；
 *  - [usageCount] > 0：必须让用户选择"转移到某个标签"或"设为无标签"。
 */
@Immutable
data class TagDeleteUiState(
    val tag: Tag,
    val usageCount: Int,
    /** null 表示"设为无标签" */
    val reassignTo: Long? = null,
    /** 可选的转移目标（同类型或通用标签） */
    val candidates: List<Tag> = emptyList(),
    val isProcessing: Boolean = false,
) {
    val needsReassign: Boolean get() = usageCount > 0

    val reassignLabel: String
        get() = if (reassignTo == null) "设为无标签"
        else candidates.firstOrNull { it.id == reassignTo }?.let { "转移到「${it.name}」" } ?: "设为无标签"
}

/** 分组标题文案 */
fun TagType.groupTitle(): String = when (this) {
    TagType.EXPENSE -> "支出标签"
    TagType.INCOME -> "收入标签"
    TagType.COMMON -> "通用标签"
}

/** 一次性事件 */
sealed interface TagsEffect {
    data class ShowMessage(val message: String) : TagsEffect
}
