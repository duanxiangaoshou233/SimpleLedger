package com.example.ledger.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledger.domain.model.TagType
import com.example.ledger.ui.components.ColorSwatch
import com.example.ledger.ui.components.EmptyState
import com.example.ledger.ui.components.LedgerAddFab
import com.example.ledger.ui.components.LedgerCard
import com.example.ledger.ui.components.LedgerDivider
import com.example.ledger.ui.components.LedgerIconButton
import com.example.ledger.ui.components.LocalSnackbarHostState
import com.example.ledger.ui.components.SectionHeader
import com.example.ledger.ui.components.SkeletonList
import com.example.ledger.ui.components.TagChip
import com.example.ledger.ui.components.toComposeColor
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Radius
import com.example.ledger.ui.theme.Spacing

/**
 * 标签管理页。
 *
 * 结构：按"支出标签 / 收入标签 / 通用标签"分组的卡片列表，每行显示
 * 色块 + 名称 + 使用次数 + 编辑/删除。
 *
 * 删除是本页唯一有"副作用"的操作，因此删除对话框会明确告诉用户
 * "有 N 笔账单在用"，并让用户选择转移到哪个标签（或设为无标签）。
 */
@Composable
fun TagsScreen(
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TagsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.sm,
                bottom = 170.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = Spacing.xs)) {
                    Text(
                        text = "标签",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.totalCount > 0) {
                            "共 ${state.totalCount} 个标签 · 颜色同时用于统计图表"
                        } else {
                            "给每一类开销起个名字，统计才好看"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                state.isLoading -> item(key = "skeleton") { SkeletonList(itemCount = 5) }

                state.isEmpty -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Label,
                        title = "还没有标签",
                        description = "点右下角「新建标签」，先建一个「餐饮」试试",
                        actionLabel = "新建标签",
                        onAction = viewModel::openEditorForNew,
                    )
                }

                else -> state.groups.forEach { group ->
                    item(key = "group-${group.type.name}") {
                        Column {
                            SectionHeader(
                                title = group.title,
                                subtitle = "${group.items.size} 个",
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            LedgerCard {
                                group.items.forEachIndexed { index, item ->
                                    TagRow(
                                        item = item,
                                        onEdit = { viewModel.openEditorForEdit(item.tag) },
                                        onDelete = { viewModel.onDeleteRequest(item) },
                                    )
                                    if (index != group.items.lastIndex) {
                                        LedgerDivider(
                                            modifier = Modifier.padding(start = 68.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        LedgerAddFab(
            label = "新建标签",
            onClick = viewModel::openEditorForNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = Spacing.lg, bottom = 104.dp),
        )
    }

    state.editor?.let { editor ->
        TagEditorDialog(
            editor = editor,
            colorChoices = state.colorChoices,
            onNameChange = viewModel::onEditorNameChange,
            onColorChange = viewModel::onEditorColorChange,
            onTypeChange = viewModel::onEditorTypeChange,
            onConfirm = viewModel::onEditorSave,
            onDismiss = viewModel::onEditorDismiss,
        )
    }

    state.deleteDialog?.let { dialog ->
        TagDeleteDialog(
            dialog = dialog,
            onReassignChange = viewModel::onReassignTargetChange,
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss,
        )
    }
}

/* ============================================================ 列表行 */

@Composable
private fun TagRow(
    item: TagItemUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = item.tag.colorHex.toComposeColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(Radius.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.tag.name.take(1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.tag.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (item.isUsed) "已用于 ${item.usageCount} 笔账单" else "尚未使用",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LedgerIconButton(
            icon = Icons.Rounded.Edit,
            contentDescription = "编辑",
            onClick = onEdit,
        )
        LedgerIconButton(
            icon = Icons.Rounded.DeleteOutline,
            contentDescription = "删除",
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

/* ============================================================ 编辑对话框 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorDialog(
    editor: TagEditorUiState,
    colorChoices: List<String>,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onTypeChange: (TagType) -> Unit,
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
                    text = editor.title,
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.height(Spacing.lg))

                TextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("标签名称") },
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

                if (editor.error != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = editor.error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                Text(
                    text = "适用范围",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TagType.entries.forEach { type ->
                        TagChip(
                            name = type.label,
                            colorHex = when (type) {
                                TagType.EXPENSE -> "#E2574C"
                                TagType.INCOME -> "#159C77"
                                TagType.COMMON -> "#5B8DEF"
                            },
                            selected = editor.type == type,
                            onClick = { onTypeChange(type) },
                        )
                    }
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
                            selected = hex.equals(editor.colorHex, ignoreCase = true),
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
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    TextButton(
                        onClick = onConfirm,
                        enabled = editor.name.isNotBlank() && !editor.isSaving,
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/* ============================================================ 删除对话框 */

@Composable
private fun TagDeleteDialog(
    dialog: TagDeleteUiState,
    onReassignChange: (Long?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.bottomSheet,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(Spacing.xl)) {
                Text(
                    text = "删除标签「${dialog.tag.name}」？",
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.height(Spacing.sm))

                if (dialog.needsReassign) {
                    Text(
                        text = "有 ${dialog.usageCount} 笔账单在使用这个标签，" +
                            "请选择如何处理它们（账单本身不会被删除）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.md))

                    ReassignOption(
                        label = "设为无标签",
                        selected = dialog.reassignTo == null,
                        onClick = { onReassignChange(null) },
                    )
                    dialog.candidates.forEach { candidate ->
                        ReassignOption(
                            label = "转移到「${candidate.name}」",
                            colorHex = candidate.colorHex,
                            selected = dialog.reassignTo == candidate.id,
                            onClick = { onReassignChange(candidate.id) },
                        )
                    }
                } else {
                    Text(
                        text = "这个标签还没有被任何账单使用，可以放心删除。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    TextButton(
                        onClick = onConfirm,
                        enabled = !dialog.isProcessing,
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReassignOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    colorHex: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (colorHex != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colorHex.toComposeColor(), AppShapes.dot),
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
