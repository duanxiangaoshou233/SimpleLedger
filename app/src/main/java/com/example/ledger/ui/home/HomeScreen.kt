package com.example.ledger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.ui.components.DayGroupHeader
import com.example.ledger.ui.components.EmptyState
import com.example.ledger.ui.components.LedgerAddFab
import com.example.ledger.ui.components.LedgerConfirmDialog
import com.example.ledger.ui.components.LocalSnackbarHostState
import com.example.ledger.ui.components.SectionHeader
import com.example.ledger.ui.components.SkeletonList
import com.example.ledger.ui.components.TransactionRow
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * 首页（记账 Tab）。
 *
 * 结构：
 * ```
 *  问候语 + 今天日期
 *  ┌─ Hero 卡（主色渐变）──────────────┐
 *  │ ‹ 2024年6月 ›        本月        │
 *  │ 支出                              │
 *  │ ¥ 3,860.50                        │
 *  │ 收入 8,200.00  |  结余 4,339.50   │
 *  └──────────────────────────────────┘
 *  最近记录                    全部记录 >
 *  今天                              -120.00
 *    [餐] 餐饮 14:30  和朋友吃饭        -120.00
 *  ...
 *                                     [ + 记一笔 ]
 * ```
 *
 * 数据全部来自 ViewModel（含按天分组），这里只负责画。
 */
@Composable
fun HomeScreen(
    onOpenRecords: () -> Unit,
    onAddRecord: (TransactionType) -> Unit,
    onEditRecord: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
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
                // 底部留出悬浮导航 + FAB 的空间，内容可以"滑到导航栏下面"
                bottom = 170.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "greeting") { GreetingHeader() }

            item(key = "hero") {
                MonthHeroCard(
                    state = state,
                    onPreviousMonth = viewModel::onPreviousMonth,
                    onNextMonth = viewModel::onNextMonth,
                )
            }

            item(key = "recentHeader") {
                SectionHeader(
                    title = "最近记录",
                    subtitle = if (state.recentCount > 0) "共 ${state.recentCount} 笔" else null,
                    trailing = {
                        TextButton(onClick = onOpenRecords) {
                            Text(
                                text = "全部记录",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    },
                )
            }

            when {
                state.isLoading -> item(key = "skeleton") { SkeletonList(itemCount = 4) }

                state.isEmpty -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "还没有记账",
                        description = "点右下角「记一笔」，10 秒记下第一笔开销",
                        actionLabel = "立即记账",
                        onAction = { onAddRecord(TransactionType.EXPENSE) },
                    )
                }

                else -> state.recentGroups.forEach { group ->
                    stickyHeader(key = "day-${group.dayStartMillis}") {
                        DayGroupHeader(
                            group = group,
                            modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        )
                    }
                    items(items = group.items, key = { it.id }) { item ->
                        TransactionRow(
                            item = item,
                            onClick = { onEditRecord(item.id) },
                            // 首页长按 = 直接删除（这里不做多选，多选交给记录列表页）
                            onLongClick = { viewModel.onDeleteRequest(item) },
                        )
                    }
                }
            }
        }

        LedgerAddFab(
            onClick = { onAddRecord(TransactionType.EXPENSE) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = Spacing.lg, bottom = 104.dp),
        )
    }

    // ---------------- 删除确认 ----------------
    state.deleting?.let { target ->
        LedgerConfirmDialog(
            title = "删除这笔记录？",
            message = buildString {
                append(target.tagName)
                append("  ")
                append(target.signedAmountText)
                if (target.note.isNotBlank()) {
                    append("（")
                    append(target.note)
                    append("）")
                }
            },
            icon = Icons.Rounded.DeleteOutline,
            destructive = true,
            confirmText = "删除",
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss,
        )
    }
}

/* ============================================================ 局部组件 */

/** 问候语 + 今天日期（时间只在进入页面时取一次，避免每帧都算） */
@Composable
private fun GreetingHeader() {
    val now = remember { DateTimeUtils.now() }
    val hour = remember(now) { Instant.ofEpochMilli(now).atZone(DateTimeUtils.zone).hour }
    val today = remember(now) { LocalDate.now(DateTimeUtils.zone) }

    val greeting = when (hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }

    Column(modifier = Modifier.padding(horizontal = Spacing.xs)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${today.monthValue}月${today.dayOfMonth}日 · " +
                today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.CHINA),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Hero 卡：主色渐变 + 两个装饰圆 + 月份切换 + 大金额 + 收入/结余 */
@Composable
private fun MonthHeroCard(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val ledgerColors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.heroCard)
            .background(
                Brush.linearGradient(
                    colors = listOf(ledgerColors.heroGradientStart, ledgerColors.heroGradientEnd),
                ),
            ),
    ) {
        // 两个半透明装饰圆：让渐变卡片有"光感"，而不是一块纯色
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 52.dp, y = (-58).dp)
                .size(168.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-44).dp, y = 44.dp)
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
        )

        Column(modifier = Modifier.padding(Spacing.xl)) {
            // ---------------- 月份切换 ----------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(
                    icon = Icons.Rounded.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    enabled = true,
                    onClick = onPreviousMonth,
                )
                Text(
                    text = state.monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                MonthArrow(
                    icon = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = "下个月",
                    enabled = state.canGoNextMonth,
                    onClick = onNextMonth,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (state.isCurrentMonth) "本月" else "历史",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "支出",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥",
                    style = AppTheme.amount.Large,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = MoneyFormatter.format(state.totals.expenseInCents),
                    style = AppTheme.amount.Hero,
                    color = Color.White,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroMiniStat(label = "收入", amountText = MoneyFormatter.format(state.totals.incomeInCents))
                Box(
                    Modifier
                        .padding(horizontal = Spacing.lg)
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
                HeroMiniStat(label = "结余", amountText = MoneyFormatter.format(state.totals.balanceInCents))
            }
        }
    }
}

@Composable
private fun RowScope.HeroMiniStat(label: String, amountText: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = amountText,
            style = AppTheme.amount.Small,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
