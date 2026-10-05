package com.example.ledger.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.ui.common.RangePreset
import com.example.ledger.ui.common.TransactionUiModel
import com.example.ledger.ui.components.DayGroupHeader
import com.example.ledger.ui.components.EmptyState
import com.example.ledger.ui.components.LedgerAddFab
import com.example.ledger.ui.components.LedgerConfirmDialog
import com.example.ledger.ui.components.LedgerIconButton
import com.example.ledger.ui.components.LedgerDivider
import com.example.ledger.ui.components.LocalSnackbarHostState
import com.example.ledger.ui.components.SkeletonList
import com.example.ledger.ui.components.SwipeDeleteBackground
import com.example.ledger.ui.components.TagChip
import com.example.ledger.ui.components.TransactionRow
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Spacing

/**
 * 记录列表页（二级页面，从首页"全部记录"进入）。
 *
 * 功能：搜索备注 / 按收支与标签与时间筛选 / 左滑删除 / 长按多选批量删除。
 *
 * 交互细节：
 *  - **左滑必须经过确认**：`confirmValueChange` 永远返回 false，
 *    行会回弹，同时弹出确认框。这样既保留了滑动手势的爽快，又不会误删。
 *  - **多选模式下禁用滑动**：避免"想选中却把它划走了"的手势冲突。
 *  - 顶部在普通态/多选态之间整体切换（SelectionBar），而不是叠加一个小工具条。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    onAddRecord: (TransactionType) -> Unit,
    onEditRecord: (Long) -> Unit,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val listState = rememberLazyListState()

    var searchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecordsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            if (state.isSelectionMode) {
                SelectionBar(
                    count = state.selectedCount,
                    allSelected = state.allSelected,
                    amountText = MoneyFormatter.format(state.selectedAmountInCents),
                    onSelectAll = viewModel::onSelectAll,
                    onDelete = viewModel::onBatchDeleteRequest,
                    onExit = viewModel::onExitSelection,
                )
            } else {
                RecordsTopBar(
                    searchActive = searchActive,
                    onBack = onBack,
                    onToggleSearch = {
                        searchActive = !searchActive
                        if (!searchActive) viewModel.onKeywordChange("")
                    },
                )
            }

            AnimatedVisibility(
                visible = searchActive && !state.isSelectionMode,
                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
            ) {
                SearchField(
                    keyword = state.keyword,
                    onKeywordChange = viewModel::onKeywordChange,
                )
            }

            FilterChipsRow(
                state = state,
                onOpenFilterSheet = { viewModel.onFilterSheetOpenChange(true) },
                onClearFilters = viewModel::onClearFilters,
            )

            SummaryRow(state = state)

            LedgerDivider(modifier = Modifier.padding(horizontal = Spacing.lg))

            // 列表区用 weight(1f)：Column 里必须显式占用"剩余高度"，
            // 直接 fillMaxSize 会按整屏高度测量，导致列表底部被裁掉
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.isLoading -> SkeletonList(
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                        itemCount = 6,
                    )

                    state.isEmpty -> EmptyState(
                        icon = if (state.hasFilter) Icons.Rounded.SearchOff else Icons.Rounded.Search,
                        title = if (state.hasFilter) "没有符合条件的记录" else "还没有任何记录",
                        description = if (state.hasFilter) {
                            "换个筛选条件，或点「清除筛选」看看全部记录"
                        } else {
                            "点右下角「记一笔」，开始记录你的每一笔开销"
                        },
                        actionLabel = if (state.hasFilter) "清除筛选" else "立即记账",
                        onAction = {
                            if (state.hasFilter) viewModel.onClearFilters()
                            else onAddRecord(TransactionType.EXPENSE)
                        },
                    )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 170.dp),
                    ) {
                        state.groups.forEach { group ->
                            stickyHeader(key = "day-${group.dayStartMillis}") {
                                DayGroupHeader(
                                    group = group,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                                )
                            }
                            items(items = group.items, key = { it.id }) { item ->
                                SwipeableTransactionRow(
                                    item = item,
                                    selectionMode = state.isSelectionMode,
                                    selected = item.id in state.selection,
                                    onClick = {
                                        if (state.isSelectionMode) {
                                            viewModel.onToggleSelection(item.id)
                                        } else {
                                            onEditRecord(item.id)
                                        }
                                    },
                                    onLongClick = { viewModel.onItemLongPress(item) },
                                    onSwipeDelete = { viewModel.onDeleteRequest(item) },
                                )
                            }
                        }
                    }
                }
            }
        }

        LedgerAddFab(
            onClick = { onAddRecord(TransactionType.EXPENSE) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = Spacing.lg, bottom = Spacing.xl),
        )
    }

    // ---------------- 筛选面板 ----------------
    if (state.isFilterSheetOpen) {
        RecordsFilterSheet(
            state = state,
            onTypeChange = viewModel::onTypeFilterChange,
            onTagChange = viewModel::onTagFilterChange,
            onRangeChange = viewModel::onRangePresetChange,
            onClear = viewModel::onClearFilters,
            onDismiss = { viewModel.onFilterSheetOpenChange(false) },
        )
    }

    // ---------------- 删除确认 ----------------
    state.deleting?.let { target ->
        LedgerConfirmDialog(
            title = "删除这笔记录？",
            message = "${target.tagName}  ${target.signedAmountText}",
            icon = Icons.Rounded.DeleteOutline,
            destructive = true,
            confirmText = "删除",
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss,
        )
    }

    if (state.batchDeleteRequested) {
        LedgerConfirmDialog(
            title = "删除选中的 ${state.selectedCount} 笔记录？",
            message = "合计 ${MoneyFormatter.format(state.selectedAmountInCents)}，删除后无法恢复",
            icon = Icons.Rounded.DeleteSweep,
            destructive = true,
            confirmText = "全部删除",
            onConfirm = viewModel::onBatchDeleteConfirm,
            onDismiss = viewModel::onBatchDeleteDismiss,
        )
    }
}

/* ============================================================ 顶部栏 */

@Composable
private fun RecordsTopBar(
    searchActive: Boolean,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            onClick = onBack,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "全部记录",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        LedgerIconButton(
            icon = if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
            contentDescription = if (searchActive) "关闭搜索" else "搜索备注",
            onClick = onToggleSearch,
        )
    }
}

/** 多选态顶栏：数量 + 合计金额 + 全选 / 删除 / 退出 */
@Composable
private fun SelectionBar(
    count: Int,
    allSelected: Boolean,
    amountText: String,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = AppShapes.card,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "退出多选",
                onClick = onExit,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(Spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已选 $count 笔",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "合计 $amountText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            LedgerIconButton(
                icon = Icons.Rounded.SelectAll,
                contentDescription = if (allSelected) "取消全选" else "全选",
                onClick = onSelectAll,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LedgerIconButton(
                icon = Icons.Rounded.DeleteSweep,
                contentDescription = "删除选中",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SearchField(
    keyword: String,
    onKeywordChange: (String) -> Unit,
) {
    TextField(
        value = keyword,
        onValueChange = onKeywordChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        placeholder = {
            Text(
                text = "搜索备注内容…",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        singleLine = true,
        shape = AppShapes.field,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

/** 横向滚动的筛选 Chip 行 */
@Composable
private fun FilterChipsRow(
    state: RecordsUiState,
    onOpenFilterSheet: () -> Unit,
    onClearFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterPill(
            text = when (state.typeFilter) {
                null -> "收支"
                TransactionType.EXPENSE -> "支出"
                TransactionType.INCOME -> "收入"
            },
            active = state.typeFilter != null,
            onClick = onOpenFilterSheet,
        )
        FilterPill(
            text = state.tagFilterName ?: "标签",
            active = state.tagFilterId != null,
            onClick = onOpenFilterSheet,
        )
        FilterPill(
            text = state.rangePreset.shortLabel,
            active = state.rangePreset != RangePreset.ALL,
            onClick = onOpenFilterSheet,
        )

        if (state.hasFilter) {
            Box(
                modifier = Modifier
                    .clip(AppShapes.chip)
                    .clickable(onClick = onClearFilters)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Text(
                    text = "清除筛选",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(AppShapes.chip)
            .background(if (active) scheme.primaryContainer else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 汇总行：笔数 + 支出 + 收入 */
@Composable
private fun SummaryRow(state: RecordsUiState) {
    val ledgerColors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "共 ${state.totalCount} 笔",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (state.expenseInCents > 0L) {
            Text(
                text = "支出 ${MoneyFormatter.format(state.expenseInCents)}",
                style = AppTheme.amount.Micro,
                color = ledgerColors.expense,
            )
            Spacer(Modifier.width(Spacing.md))
        }
        if (state.incomeInCents > 0L) {
            Text(
                text = "收入 ${MoneyFormatter.format(state.incomeInCents)}",
                style = AppTheme.amount.Micro,
                color = ledgerColors.income,
            )
        }
    }
}

/* ============================================================ 列表行（左滑删除） */

@Composable
private fun SwipeableTransactionRow(
    item: TransactionUiModel,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeDelete: () -> Unit,
) {
    // 多选模式下不做滑动，避免手势冲突
    if (selectionMode) {
        TransactionRow(
            item = item,
            onClick = onClick,
            onLongClick = onLongClick,
            selectionMode = true,
            selected = selected,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
            }
            // 永远返回 false：行回弹，删除必须经过确认对话框
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeDeleteBackground() },
        enableDismissFromStartToEnd = false,
    ) {
        TransactionRow(
            item = item,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

/* ============================================================ 筛选面板 */

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RecordsFilterSheet(
    state: RecordsUiState,
    onTypeChange: (TransactionType?) -> Unit,
    onTagChange: (Long?) -> Unit,
    onRangeChange: (RangePreset) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppShapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
        ) {
            Text(
                text = "筛选",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(Spacing.lg))

            FilterSectionTitle("收支类型")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TagChip(
                    name = "全部",
                    colorHex = "#5B8DEF",
                    selected = state.typeFilter == null,
                    onClick = { onTypeChange(null) },
                    neutral = true,
                )
                TagChip(
                    name = "支出",
                    colorHex = "#E2574C",
                    selected = state.typeFilter == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                )
                TagChip(
                    name = "收入",
                    colorHex = "#159C77",
                    selected = state.typeFilter == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            FilterSectionTitle("时间范围")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                RangePreset.listPresets.forEach { preset ->
                    TagChip(
                        name = preset.label,
                        colorHex = "#2E9E8F",
                        selected = state.rangePreset == preset,
                        onClick = { onRangeChange(preset) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            FilterSectionTitle("标签")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TagChip(
                    name = "全部标签",
                    colorHex = "#9AA5A0",
                    selected = state.tagFilterId == null,
                    onClick = { onTagChange(null) },
                    neutral = true,
                )
                state.allTags.forEach { tag ->
                    TagChip(
                        name = tag.name,
                        colorHex = tag.colorHex,
                        selected = state.tagFilterId == tag.id,
                        onClick = { onTagChange(tag.id) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("清除全部筛选", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(Spacing.sm))
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("完成")
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.sm),
    )
}
