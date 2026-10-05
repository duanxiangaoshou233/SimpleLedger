package com.example.ledger.ui.record

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.ui.components.ColorSwatch
import com.example.ledger.ui.components.LedgerDatePickerDialog
import com.example.ledger.ui.components.LedgerIconButton
import com.example.ledger.ui.components.LedgerTimePickerDialog
import com.example.ledger.ui.components.TagChip
import com.example.ledger.ui.home.AmountKeypad
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate

/**
 * 记账 / 编辑面板（全局唯一，由 LedgerApp 渲染一次）。
 *
 * 结构：
 * ```
 *  拖拽条
 *  ┌ 标题 + 关闭            ┐  固定
 *  │ 支出 / 收入 分段切换    │
 *  │ ¥ 0.00  （大金额 + 光标）│
 *  ├ 日期 / 时间 chip        ┤  可滚动
 *  │ 备注                    │
 *  │ 标签 FlowRow（含新建）   │
 *  ├ 数字键盘                ┤  固定
 *  └ 保存（键盘右侧长条）     ┘
 * ```
 *
 * 退场动画：`state.visible` 变 false 时先 `sheetState.hide()` 再卸载，
 * 不会"啪"地消失。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordEditorSheet(
    state: RecordEditorUiState,
    onDismiss: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onTagSelected: (Long?) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onNewTagClick: () -> Unit,
    onNewTagNameChange: (String) -> Unit,
    onNewTagColorChange: (String) -> Unit,
    onNewTagConfirm: () -> Unit,
    onNewTagDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mounted by remember { mutableStateOf(state.visible) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.visible) {
        if (state.visible) {
            mounted = true
        } else {
            runCatching { sheetState.hide() }
            mounted = false
            showDatePicker = false
            showTimePicker = false
        }
    }

    if (!mounted) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppShapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 上限 780dp：小屏可滚动，大屏不显得空
                .heightIn(max = 780.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
        ) {
            EditorHeader(state = state, onDismiss = onDismiss)

            Spacer(Modifier.height(Spacing.md))

            TypeToggle(type = state.type, onChange = onTypeChange)

            Spacer(Modifier.height(Spacing.lg))

            AmountDisplay(state = state)

            Spacer(Modifier.height(Spacing.lg))

            DateTimeRow(
                state = state,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true },
            )

            Spacer(Modifier.height(Spacing.md))

            NoteField(note = state.note, onNoteChange = onNoteChange)

            Spacer(Modifier.height(Spacing.lg))

            TagSection(
                state = state,
                onTagSelected = onTagSelected,
                onNewTagClick = onNewTagClick,
            )

            Spacer(Modifier.height(Spacing.lg))

            AmountKeypad(
                onDigit = onDigit,
                onDecimal = onDecimal,
                onBackspace = onBackspace,
                onClear = onClear,
                onSave = onSave,
                saveEnabled = state.canSave,
                isEditing = !state.isNew,
            )
        }
    }

    // ---------------- 日期 / 时间 / 新建标签 三个弹窗 ----------------
    if (showDatePicker) {
        LedgerDatePickerDialog(
            initialDate = Instant.ofEpochMilli(state.dateTime).atZone(DateTimeUtils.zone).toLocalDate(),
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        val time = Instant.ofEpochMilli(state.dateTime).atZone(DateTimeUtils.zone).toLocalTime()
        LedgerTimePickerDialog(
            initialHour = time.hour,
            initialMinute = time.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onTimeChange(hour, minute)
                showTimePicker = false
            },
        )
    }

    val draft = state.newTagDraft
    if (draft != null) {
        NewTagDialog(
            draft = draft,
            colorChoices = state.colorChoices,
            onNameChange = onNewTagNameChange,
            onColorChange = onNewTagColorChange,
            onConfirm = onNewTagConfirm,
            onDismiss = onNewTagDismiss,
        )
    }
}

/* ============================================================ 局部组件 */

@Composable
private fun EditorHeader(state: RecordEditorUiState, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.dateText.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${state.dateText} ${state.weekdayText} ${state.timeText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LedgerIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "关闭",
            onClick = onDismiss,
        )
    }
}

/** 支出 / 收入 的分段切换：滑块直接染成语义色（支出红 / 收入绿） */
@Composable
private fun TypeToggle(type: TransactionType, onChange: (TransactionType) -> Unit) {
    val ledgerColors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppShapes.chip)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        TransactionType.entries.forEach { candidate ->
            val selected = candidate == type
            val activeColor = if (candidate.isExpense) ledgerColors.expense else ledgerColors.income
            val containerAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(220),
                label = "typeToggle",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(AppShapes.chip)
                    .background(activeColor.copy(alpha = containerAlpha))
                    .clickable { onChange(candidate) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = candidate.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 大金额展示：右对齐、等宽数字、底部一条语义色细线、末尾一个呼吸光标 */
@Composable
private fun AmountDisplay(state: RecordEditorUiState) {
    val ledgerColors = AppTheme.colors
    val activeColor = if (state.type.isExpense) ledgerColors.expense else ledgerColors.income
    val hasAmount = state.amountInCents > 0L
    val amountColor by animateColorAsState(
        targetValue = if (hasAmount) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        animationSpec = tween(220),
        label = "amountColor",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "¥",
                style = AppTheme.amount.Large,
                color = amountColor.copy(alpha = 0.75f),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = state.amountInput.displayText,
                style = AppTheme.amount.Hero,
                color = amountColor,
                maxLines = 1,
            )
            BlinkingCursor(color = activeColor)
        }

        Spacer(Modifier.height(Spacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    color = activeColor.copy(alpha = if (hasAmount) 0.55f else 0.20f),
                    shape = CircleShape,
                ),
        )

        val error = state.amountError
        if (error != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = error,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** 呼吸光标：让"这是正在输入的金额"这件事一眼可见 */
@Composable
private fun BlinkingCursor(color: Color) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Box(
        modifier = Modifier
            .padding(start = Spacing.xs, bottom = Spacing.sm)
            .size(width = 2.dp, height = 30.dp)
            .background(color.copy(alpha = alpha)),
    )
}

/** 日期 / 时间两个 chip（点开对应的 M3 选择器） */
@Composable
private fun DateTimeRow(
    state: RecordEditorUiState,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        InfoChip(
            icon = { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(16.dp)) },
            text = state.dateText,
            onClick = onPickDate,
        )
        InfoChip(
            icon = { Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp)) },
            text = state.timeText,
            onClick = onPickTime,
        )
    }
}

@Composable
private fun InfoChip(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = AppShapes.chip,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) { icon() }
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 备注输入：用填充式 TextField 并去掉默认下划线，视觉更干净 */
@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit) {
    TextField(
        value = note,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "添加备注…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        },
        singleLine = true,
        shape = AppShapes.field,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

/** 标签区：FlowRow 的 Chip + 末尾"新建标签"虚线 chip */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(
    state: RecordEditorUiState,
    onTagSelected: (Long?) -> Unit,
    onNewTagClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "标签",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.tagId == null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "未选择",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TagChip(
                name = "无标签",
                colorHex = "#9AA5A0",
                selected = state.tagId == null,
                onClick = { onTagSelected(null) },
                neutral = true,
            )
            state.availableTags.forEach { tag ->
                TagChip(
                    name = tag.name,
                    colorHex = tag.colorHex,
                    selected = state.tagId == tag.id,
                    onClick = { onTagSelected(tag.id) },
                )
            }
            AddTagChip(onClick = onNewTagClick)
        }
    }
}

/** 虚线描边的"新建标签" Chip（用 Compose 的虚线绘制，避免引入额外资源） */
@Composable
private fun AddTagChip(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = AppShapes.chip,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = scheme.outline,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = "新建",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
            )
        }
    }
}

/** 现场新建标签弹窗：名称 + 颜色 + 类型跟随当前记账类型 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewTagDialog(
    draft: NewTagDraftUiState,
    colorChoices: List<String>,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.bottomSheet,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(Spacing.xl)) {
                Text(
                    text = "新建标签",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.lg))
                TextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("标签名称，如「咖啡」") },
                    singleLine = true,
                    shape = AppShapes.field,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                if (draft.error != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = draft.error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    colorChoices.forEach { hex ->
                        ColorSwatch(
                            colorHex = hex,
                            selected = hex.equals(draft.colorHex, ignoreCase = true),
                            onClick = { onColorChange(hex) },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "取消",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    TextButton(
                        onClick = onConfirm,
                        enabled = draft.name.isNotBlank() && !draft.isSaving,
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
