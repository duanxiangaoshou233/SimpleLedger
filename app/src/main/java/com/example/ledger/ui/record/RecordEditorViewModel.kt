package com.example.ledger.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledger.core.util.AmountInputState
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.core.util.TagPalette
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TagType
import com.example.ledger.domain.model.Transaction
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.repository.TagRepository
import com.example.ledger.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * 记账 / 编辑面板的 ViewModel（**Activity 作用域，全 App 共用一份**）。
 *
 * 状态设计：
 *  - 内部真源是 [draft]（可变的"草稿"）+ [tagsCache]（标签缓存），
 *    对外暴露的 [uiState] 是二者 combine 出来的**只读派生状态**；
 *  - 这样"切换收支类型后自动过滤可选标签""标签被别处删掉后不再显示"这类联动，
 *    不需要写任何手工同步代码——标签流一变，派生状态自然重算。
 *
 * 保存校验（都在这里做，UI 只负责显示 error）：
 *  1) 金额必须 > 0；
 *  2) 标签必须仍然存在（可能在面板打开期间被标签页删掉了，不校验会触发外键约束异常）。
 */
@HiltViewModel
class RecordEditorViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val draft = MutableStateFlow(EditorDraft())

    /** 标签缓存：不依赖 UI 订阅，随时能读到最新标签（保存校验要用） */
    private val tagsCache = MutableStateFlow<List<Tag>>(emptyList())

    private val _effects = Channel<RecordEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTags().collect { tagsCache.value = it }
        }
    }

    val uiState: StateFlow<RecordEditorUiState> =
        combine(draft, tagsCache) { d, tags -> d.toUiState(tags) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RecordEditorUiState(),
            )

    /* ============================================================ 打开 / 关闭 */

    /** 打开"记一笔"（默认记支出，时间取当前时刻） */
    fun openForCreate(type: TransactionType = TransactionType.EXPENSE) {
        draft.value = EditorDraft(
            visible = true,
            type = type,
            dateTime = DateTimeUtils.now(),
        )
    }

    /** 打开"编辑"（把已有账单灌进草稿） */
    fun openForEdit(transaction: Transaction) {
        // 标签可能在别处被删了：找不到就是"无标签"，别把脏 id 带进来
        val tag = tagsCache.value.firstOrNull { it.id == transaction.tagId }
        draft.value = EditorDraft(
            visible = true,
            id = transaction.id,
            type = transaction.type,
            amountInput = AmountInputState.fromCents(transaction.amountInCents),
            dateTime = transaction.dateTime,
            tagId = tag?.id,
            note = transaction.note,
        )
    }

    fun dismiss() {
        draft.value = EditorDraft()
    }

    /**
     * 按 id 打开编辑。
     * 列表里只有 UI 模型（不带完整领域对象），所以这里自己去仓库取一次，
     * 让 UI 层只需要传一个 id，不必把 Transaction 一路传上来。
     */
    fun openForEditById(id: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getById(id) ?: return@launch
            openForEdit(transaction)
        }
    }

    /* ============================================================ 收支与金额 */

    fun setType(type: TransactionType) {
        val current = draft.value
        // 切换到收入/支出后，原本选中的标签可能不再适用（如"餐饮"不能用于收入）→ 自动清空
        val stillValid = current.tagId
            ?.let { id -> tagsCache.value.any { it.id == id && it.type.matches(type) } }
            ?: false
        draft.value = current.copy(
            type = type,
            tagId = if (stillValid) current.tagId else null,
            newTagDraft = null,
            amountError = null,
        )
    }

    fun onDigit(digit: Char) = updateAmount { it.digit(digit) }

    fun onDecimal() = updateAmount { it.decimal() }

    fun onBackspace() = updateAmount { it.backspace() }

    fun onClearAmount() = updateAmount { it.clear() }

    private fun updateAmount(transform: (AmountInputState) -> AmountInputState) {
        val current = draft.value
        draft.value = current.copy(
            amountInput = transform(current.amountInput),
            amountError = null,
        )
    }

    /* ============================================================ 其它字段 */

    fun onTagSelected(tagId: Long?) {
        draft.value = draft.value.copy(tagId = tagId)
    }

    fun onNoteChange(note: String) {
        draft.value = draft.value.copy(note = note)
    }

    /** 改日期：**保留原来的时分秒**（用户只想换一天，不想时间被重置成 00:00） */
    fun onDateChange(date: LocalDate) {
        val current = draft.value
        val zone = DateTimeUtils.zone
        val time = Instant.ofEpochMilli(current.dateTime).atZone(zone).toLocalTime()
        draft.value = current.copy(
            dateTime = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli(),
        )
    }

    /** 改时间：保留原来的日期 */
    fun onTimeChange(hour: Int, minute: Int) {
        val current = draft.value
        val zone = DateTimeUtils.zone
        val date = Instant.ofEpochMilli(current.dateTime).atZone(zone).toLocalDate()
        draft.value = current.copy(
            dateTime = LocalDateTime.of(date, LocalTime.of(hour, minute))
                .atZone(zone)
                .toInstant()
                .toEpochMilli(),
        )
    }

    /* ============================================================ 现场新建标签 */

    fun openNewTagDialog() {
        // 先给一个默认色，让弹窗"立刻有颜色"，再去数据层问一个更合适的色
        val fallback = TagPalette.hexColors.first()
        draft.value = draft.value.copy(newTagDraft = NewTagDraftUiState(colorHex = fallback))

        viewModelScope.launch {
            val suggested = tagRepository.suggestColor()
            // 只更新颜色，不覆盖用户已经输入的标签名（suggestColor 是挂起查询，期间用户可能在打字）
            val pending = draft.value.newTagDraft ?: return@launch
            draft.value = draft.value.copy(newTagDraft = pending.copy(colorHex = suggested))
        }
    }

    fun onNewTagNameChange(name: String) {
        val nt = draft.value.newTagDraft ?: return
        draft.value = draft.value.copy(newTagDraft = nt.copy(name = name, error = null))
    }

    fun onNewTagColorChange(colorHex: String) {
        val nt = draft.value.newTagDraft ?: return
        draft.value = draft.value.copy(newTagDraft = nt.copy(colorHex = colorHex))
    }

    fun dismissNewTagDialog() {
        draft.value = draft.value.copy(newTagDraft = null)
    }

    /**
     * 确认新建标签：校验非空 + 同类型下不重名，成功后**自动选中**这个新标签。
     * 现场新建的标签类型跟随当前记账类型（记支出时建的标签就是支出标签）。
     */
    fun confirmNewTag() {
        val current = draft.value
        val pending = current.newTagDraft ?: return
        val name = pending.name.trim()

        if (name.isEmpty()) {
            draft.value = current.copy(newTagDraft = pending.copy(error = "请输入标签名称"))
            return
        }

        val tagType = when (current.type) {
            TransactionType.EXPENSE -> TagType.EXPENSE
            TransactionType.INCOME -> TagType.INCOME
        }

        viewModelScope.launch {
            draft.value = draft.value.copy(newTagDraft = pending.copy(isSaving = true, error = null))

            if (tagRepository.findByName(name, tagType) != null) {
                draft.value = draft.value.copy(
                    newTagDraft = pending.copy(isSaving = false, error = "已有同名标签「$name」"),
                )
                return@launch
            }

            val newId = tagRepository.create(
                Tag(name = name, colorHex = pending.colorHex, type = tagType),
            )
            draft.value = draft.value.copy(tagId = newId, newTagDraft = null)
            _effects.send(RecordEditorEffect.ShowMessage("已创建标签「$name」"))
        }
    }

    /* ============================================================ 保存 */

    fun save() {
        val current = draft.value
        if (current.isSaving) return

        if (current.amountInput.cents <= 0L) {
            draft.value = current.copy(amountError = "请输入金额")
            return
        }

        // 标签二次校验：面板打开期间标签可能已被删除，直接写库会触发外键约束异常
        val validTagId = current.tagId?.takeIf { id -> tagsCache.value.any { it.id == id } }

        viewModelScope.launch {
            draft.value = draft.value.copy(isSaving = true, amountError = null)

            val isNew = current.id == 0L
            transactionRepository.save(
                Transaction(
                    id = current.id,
                    type = current.type,
                    amountInCents = current.amountInput.cents,
                    dateTime = current.dateTime,
                    tagId = validTagId,
                    note = current.note.trim(),
                ),
            )

            // 关闭面板（重置草稿），并给一次性反馈
            draft.value = EditorDraft()
            _effects.send(
                RecordEditorEffect.ShowMessage(
                    if (isNew) "已记一笔 ${current.type.label}" else "已更新这笔记录",
                ),
            )
        }
    }
}

/**
 * 编辑器内部草稿（不对外暴露）。
 *
 * 与 [RecordEditorUiState] 的区别：这里没有 `availableTags`、`colorChoices` 这些
 * "由外部数据派生"的字段，只保存用户真正编辑的内容。
 */
private data class EditorDraft(
    val visible: Boolean = false,
    val id: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: AmountInputState = AmountInputState(),
    val dateTime: Long = 0L,
    val tagId: Long? = null,
    val note: String = "",
    val isSaving: Boolean = false,
    val amountError: String? = null,
    val newTagDraft: NewTagDraftUiState? = null,
)

/** 草稿 + 标签列表 -> 对外 UI 状态 */
private fun EditorDraft.toUiState(tags: List<Tag>): RecordEditorUiState = RecordEditorUiState(
    visible = visible,
    id = id,
    isNew = id == 0L,
    type = type,
    amountInput = amountInput,
    dateTime = dateTime,
    tagId = tagId,
    note = note,
    // 只展示"适用于当前收支类型"的标签：支出时看到 支出+通用，收入时看到 收入+通用
    availableTags = tags.filter { it.type.matches(type) },
    colorChoices = TagPalette.selectable,
    isSaving = isSaving,
    amountError = amountError,
    newTagDraft = newTagDraft,
    dateText = DateTimeUtils.formatDate(dateTime),
    timeText = DateTimeUtils.formatTime(dateTime),
    weekdayText = DateTimeUtils.formatWeekday(dateTime),
)
