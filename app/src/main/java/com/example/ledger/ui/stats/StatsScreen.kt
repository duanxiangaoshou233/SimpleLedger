package com.example.ledger.ui.stats

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.ui.common.RangePreset
import com.example.ledger.ui.components.EmptyState
import com.example.ledger.ui.components.LedgerCard
import com.example.ledger.ui.components.LedgerDatePickerDialog
import com.example.ledger.ui.components.LocalSnackbarHostState
import com.example.ledger.ui.components.SectionHeader
import com.example.ledger.ui.components.SkeletonList
import com.example.ledger.ui.components.TagChip
import com.example.ledger.ui.stats.charts.ChartLegend
import com.example.ledger.ui.stats.charts.DonutChart
import com.example.ledger.ui.stats.charts.TagRankingRow
import com.example.ledger.ui.stats.charts.TrendBarChart
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Spacing
import java.time.LocalDate

/**
 * 统计页。
 *
 * 三个页签：**支出 / 收入 / 自定义时间块**。
 * 每个页签下都是同一套结构：总额 → 标签环形图（图例可点选）→ 标签排行 → 时间趋势柱状图。
 *
 * 图表全部是自研 Compose Canvas 组件（DonutChart / TrendBarChart），
 * 数据由 ViewModel 算好（含占比、零值补桶、自动粒度），这里只负责摆放与联动。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current

    // 柱状图的选中柱子是纯展示状态，留在页面里即可
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.trend) { selectedBarIndex = null }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StatsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    val ledgerColors = AppTheme.colors
    val focusColor = if (state.focusType == TransactionType.INCOME) {
        ledgerColors.income
    } else {
        ledgerColors.expense
    }

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
                    text = "统计",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.rangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "tabs") {
            SegmentedTabs(
                options = StatsTab.entries.map { it.label },
                selectedIndex = StatsTab.entries.indexOf(state.tab),
                onSelect = { viewModel.onTabChange(StatsTab.entries[it]) },
            )
        }

        // ---------------- 自定义时间块（只在自定义页签出现）----------------
        if (state.tab == StatsTab.CUSTOM) {
            item(key = "customRange") {
                LedgerCard {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = "时间区间（含首含尾）",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            DateField(
                                label = "开始",
                                date = state.customStart,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.onStartPickerOpenChange(true) },
                            )
                            DateField(
                                label = "结束",
                                date = state.customEnd,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.onEndPickerOpenChange(true) },
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            listOf(
                                RangePreset.THIS_WEEK,
                                RangePreset.THIS_MONTH,
                                RangePreset.THIS_YEAR,
                            ).forEach { preset ->
                                TagChip(
                                    name = preset.shortLabel,
                                    colorHex = "#2E9E8F",
                                    selected = false,
                                    onClick = { viewModel.onQuickRange(preset) },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TagChip(
                                name = "统计支出",
                                colorHex = "#E2574C",
                                selected = state.customType == TransactionType.EXPENSE,
                                onClick = { viewModel.onCustomTypeChange(TransactionType.EXPENSE) },
                            )
                            TagChip(
                                name = "统计收入",
                                colorHex = "#159C77",
                                selected = state.customType == TransactionType.INCOME,
                                onClick = { viewModel.onCustomTypeChange(TransactionType.INCOME) },
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "presets") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    RangePreset.statsPresets.forEach { preset ->
                        TagChip(
                            name = preset.label,
                            colorHex = "#2E9E8F",
                            selected = state.preset == preset,
                            onClick = { viewModel.onPresetChange(preset) },
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            item(key = "skeleton") { SkeletonList(itemCount = 5) }
            return@LazyColumn
        }

        if (state.isEmpty) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.EventNote,
                    title = "这段时间还没有记录",
                    description = "换个时间范围，或者先去记一笔",
                )
            }
            return@LazyColumn
        }

        // ---------------- 总额 ----------------
        item(key = "total") {
            LedgerCard {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = if (state.focusType == TransactionType.INCOME) "总收入" else "总支出",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥",
                            style = AppTheme.amount.Large,
                            color = focusColor.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = MoneyFormatter.format(state.focusTotalInCents),
                            style = AppTheme.amount.Hero,
                            color = focusColor,
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "共 ${state.tagAmounts.sumOf { it.count }} 笔 · " +
                            "${state.tagAmounts.size} 个标签 · " +
                            state.granularity.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---------------- 环形图 + 图例 ----------------
        item(key = "donut") {
            LedgerCard {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    SectionHeader(
                        title = "按标签占比",
                        subtitle = "点击图例筛选",
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DonutChart(
                            data = state.tagAmounts,
                            highlightTagId = state.highlightTagId,
                            onSliceClick = viewModel::onHighlightTag,
                            centerTitle = if (state.focusType == TransactionType.INCOME) "总收入" else "总支出",
                            centerAmountInCents = state.focusTotalInCents,
                            centerAmountColor = focusColor,
                        )
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    ChartLegend(
                        items = state.tagAmounts,
                        highlightTagId = state.highlightTagId,
                        onToggle = viewModel::onHighlightTag,
                    )
                }
            }
        }

        // ---------------- 标签排行 ----------------
        item(key = "ranking") {
            LedgerCard {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    SectionHeader(
                        title = "标签排行",
                        subtitle = "共 ${state.tagAmounts.size} 项",
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                    val maxRatio = state.tagAmounts.maxOfOrNull { it.ratio } ?: 0f
                    state.tagAmounts.forEach { item ->
                        TagRankingRow(
                            item = item,
                            maxRatio = maxRatio,
                            highlighted = state.highlightTagId == item.tagId,
                            onClick = { viewModel.onHighlightTag(item.tagId) },
                        )
                    }
                }
            }
        }

        // ---------------- 时间趋势 ----------------
        item(key = "trend") {
            LedgerCard {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    SectionHeader(
                        title = "支出趋势",
                        subtitle = state.granularity.label,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                    TrendBarChart(
                        points = state.trend,
                        barColor = focusColor,
                        granularity = state.granularity,
                        selectedIndex = selectedBarIndex,
                        onBarClick = { selectedBarIndex = it },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "点击柱子可查看具体金额",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // ---------------- 日期选择 ----------------
    if (state.showStartPicker) {
        LedgerDatePickerDialog(
            initialDate = state.customStart,
            onDismiss = { viewModel.onStartPickerOpenChange(false) },
            onConfirm = {
                viewModel.onCustomStartChange(it)
                viewModel.onStartPickerOpenChange(false)
            },
        )
    }
    if (state.showEndPicker) {
        LedgerDatePickerDialog(
            initialDate = state.customEnd,
            onDismiss = { viewModel.onEndPickerOpenChange(false) },
            onConfirm = {
                viewModel.onCustomEndChange(it)
                viewModel.onEndPickerOpenChange(false)
            },
        )
    }
}

/* ============================================================ 局部组件 */

/** 三选一分段控件（支出 / 收入 / 自定义） */
@Composable
private fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(AppShapes.field)
            .background(scheme.surfaceContainerHigh)
            .padding(Spacing.xs),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val container by animateColorAsState(
                targetValue = if (selected) scheme.primary else Color.Transparent,
                animationSpec = tween(220),
                label = "tabContainer",
            )
            val content by animateColorAsState(
                targetValue = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "tabContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(AppShapes.field)
                    .background(container)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (index) {
                            0 -> Icons.Rounded.PieChart
                            1 -> Icons.Rounded.BarChart
                            else -> Icons.Rounded.CalendarMonth
                        },
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = content,
                    )
                }
            }
        }
    }
}

/** 自定义区间的日期字段（点击弹 M3 DatePicker） */
@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(AppShapes.field)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = "${date.year}-${date.monthValue.toString().padStart(2, '0')}-" +
                    date.dayOfMonth.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
