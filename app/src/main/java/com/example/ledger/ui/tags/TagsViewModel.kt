package com.example.ledger.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledger.core.util.TagPalette
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TagType
import com.example.ledger.domain.model.TagWithUsage
import com.example.ledger.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 标签管理 ViewModel。
 *
 * 两个对话框（编辑 / 删除）的状态都各自收成一个可空对象：
 * `null` = 关闭，非 null = 打开。这样不会出现"开关说开着但数据没了"的错位。
 */
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<TagEditorUiState?>(null)
    private val deleteDialog = MutableStateFlow<TagDeleteUiState?>(null)

    /** 标签缓存：删除时挑"转移目标"、编辑时查重都要用，且不依赖 UI 是否在订阅 */
    private val tagsCache = MutableStateFlow<List<TagWithUsage>>(emptyList())

    private val _effects = Channel<TagsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTagsWithUsage().collect { tagsCache.value = it }
        }
    }

    val uiState: StateFlow<TagsUiState> = combine(
        tagsCache,
        editor,
        deleteDialog,
    ) { tags, editorState, deleteState ->
        TagsUiState(
            isLoading = false,
            groups = tags.toGroups(),
            totalCount = tags.size,
            editor = editorState,
            deleteDialog = deleteState,
            colorChoices = TagPalette.selectable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagsUiState(),
    )

    /* ============================================================ 新建 / 编辑 */

    fun openEditorForNew() {
        editor.value = TagEditorUiState(
            id = 0L,
            name = "",
            colorHex = TagPalette.hexColors.first(),
            type = TagType.EXPENSE,
        )
    }

    fun openEditorForEdit(tag: Tag) {
        editor.value = TagEditorUiState(
            id = tag.id,
            name = tag.name,
            colorHex = tag.colorHex,
            type = tag.type,
        )
    }

    fun onEditorNameChange(name: String) {
        val current = editor.value ?: return
        editor.value = current.copy(name = name, error = null)
    }

    fun onEditorColorChange(colorHex: String) {
        val current = editor.value ?: return
        editor.value = current.copy(colorHex = colorHex)
    }

    fun onEditorTypeChange(type: TagType) {
        val current = editor.value ?: return
        editor.value = current.copy(type = type)
    }

    fun onEditorDismiss() {
        editor.value = null
    }

    /**
     * 保存标签：校验名称非空 + 同类型下不重名（编辑时排除自己）。
     * 校验放在 ViewModel，数据库层面不加唯一索引 —— 用户看到的是友好提示而不是崩溃。
     */
    fun onEditorSave() {
        val current = editor.value ?: return
        if (current.isSaving) return

        val name = current.name.trim()
        if (name.isEmpty()) {
            editor.value = current.copy(error = "请输入标签名称")
            return
        }

        viewModelScope.launch {
            editor.value = current.copy(isSaving = true, error = null)

            val duplicate = tagRepository.findByName(name, current.type)
            if (duplicate != null && duplicate.id != current.id) {
                editor.value = current.copy(isSaving = false, error = "已有同名标签「$name」")
                return@launch
            }

            if (current.isNew) {
                tagRepository.create(Tag(name = name, colorHex = current.colorHex, type = current.type))
            } else {
                tagRepository.update(
                    Tag(id = current.id, name = name, colorHex = current.colorHex, type = current.type),
                )
            }

            editor.value = null
            _effects.send(
                TagsEffect.ShowMessage(if (current.isNew) "已创建标签「$name」" else "已保存修改"),
            )
        }
    }

    /* ============================================================ 删除 */

    fun onDeleteRequest(item: TagItemUi) {
        // 转移候选：同类型或通用标签（收入标签不适合承接支出账单的转移）
        val candidates = tagsCache.value
            .map { it.tag }
            .filter { tag ->
                tag.id != item.tag.id &&
                    (tag.type == item.tag.type ||
                        tag.type == TagType.COMMON ||
                        item.tag.type == TagType.COMMON)
            }
        deleteDialog.value = TagDeleteUiState(
            tag = item.tag,
            usageCount = item.usageCount,
            reassignTo = candidates.firstOrNull()?.id,
            candidates = candidates,
        )
    }

    fun onReassignTargetChange(tagId: Long?) {
        val current = deleteDialog.value ?: return
        deleteDialog.value = current.copy(reassignTo = tagId)
    }

    fun onDeleteDismiss() {
        deleteDialog.value = null
    }

    fun onDeleteConfirm() {
        val current = deleteDialog.value ?: return
        if (current.isProcessing) return

        deleteDialog.value = current.copy(isProcessing = true)
        viewModelScope.launch {
            val affected = tagRepository.deleteTag(current.tag.id, current.reassignTo)
            deleteDialog.value = null
            _effects.send(TagsEffect.ShowMessage(buildDeleteMessage(current, affected)))
        }
    }

    private fun buildDeleteMessage(state: TagDeleteUiState, affected: Int): String = when {
        affected <= 0 -> "已删除标签「${state.tag.name}」"
        state.reassignTo == null -> "已删除标签「${state.tag.name}」，$affected 笔账单已设为无标签"
        else -> "已删除标签「${state.tag.name}」，$affected 笔账单已${state.reassignLabel}"
    }

    /** 按 TagType 分组（顺序由枚举顺序决定：支出 → 收入 → 通用） */
    private fun List<TagWithUsage>.toGroups(): List<TagGroupUi> =
        groupBy { it.tag.type }
            .toSortedMap(compareBy<TagType> { it.ordinal })
            .map { (type, items) ->
                TagGroupUi(
                    type = type,
                    title = type.groupTitle(),
                    items = items.map { TagItemUi(tag = it.tag, usageCount = it.usageCount) },
                )
            }
}
