package com.example.ledger.ui.stats.charts

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TagAmount
import com.example.ledger.ui.components.toComposeColor
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 环形图图例：颜色点 + 标签名 + 占比，**可点击筛选**。
 *
 * 选中（高亮）逻辑：
 *  - 点击某个图例 → 该标签高亮，环形图其它切片变淡、下方排行榜只强调这一行；
 *  - 再次点击同一个 → 取消高亮。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(
    items: List<TagAmount>,
    highlightTagId: Long?,
    onToggle: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items.forEach { item ->
            val color = item.colorHex.toComposeColor()
            val selected = highlightTagId == item.tagId
            val container by animateColorAsState(
                targetValue = if (selected) color.copy(alpha = 0.18f) else Color.Transparent,
                animationSpec = tween(180),
                label = "legendContainer",
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) color.copy(alpha = 0.55f) else scheme.outlineVariant,
                animationSpec = tween(180),
                label = "legendBorder",
            )

            Row(
                modifier = Modifier
                    .clip(AppShapes.chip)
                    .background(container)
                    .border(1.dp, borderColor, AppShapes.chip)
                    .clickable { onToggle(if (selected) null else item.tagId) }
                    .padding(horizontal = Spacing.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, AppShapes.dot),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.tagName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "${(item.ratio * 100).roundToInt()}%",
                    style = AppTheme.amount.Micro,
                    color = color,
                )
            }
        }
    }
}

/**
 * 标签排行榜的一行：色点 + 名称 + 笔数 + 进度条 + 金额。
 *
 * 进度条是"占比"的可视化，比自己再算一遍百分比更直观；
 * 高亮时整行底色与进度条都会加强。
 */
@Composable
fun TagRankingRow(
    item: TagAmount,
    maxRatio: Float,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val color = item.colorHex.toComposeColor()
    val barFraction = if (maxRatio > 0f) (item.ratio / maxRatio).coerceIn(0f, 1f) else 0f

    val container by animateColorAsState(
        targetValue = if (highlighted) color.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(180),
        label = "rankContainer",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.field)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(color, AppShapes.dot),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = item.tagName,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "${item.count} 笔",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = MoneyFormatter.format(item.amount),
                style = AppTheme.amount.Small,
                color = if (highlighted) color else scheme.onSurface,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "${(item.ratio * 100).roundToInt()}%",
                style = AppTheme.amount.Micro,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.width(34.dp),
            )
        }

        Spacer(Modifier.height(6.dp))

        // 占比条：轨道 + 填充（宽度用 weight 比例，不需要动画状态）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(AppShapes.chip)
                .background(scheme.surfaceContainerHigh),
        ) {
            if (barFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .height(5.dp)
                        .clip(AppShapes.chip)
                        .background(color.copy(alpha = if (highlighted) 1f else 0.75f)),
                )
            }
        }
    }
}
