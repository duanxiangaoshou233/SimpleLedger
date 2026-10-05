package com.example.ledger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.common.TransactionDayGroup
import com.example.ledger.ui.common.TransactionUiModel
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Radius
import com.example.ledger.ui.theme.Spacing

/**
 * 账单列表的一行。
 *
 * 布局：`[标签头像 / 多选圈] [标签名 + 时间 / 备注] [带符号金额]`
 *
 * 细节：
 *  - 头像用"标签首字 + 标签色 14% 淡底"，比纯色点更有信息量，也比图标更贴合中文语境；
 *  - 金额用等宽字体 + 支出红/收入绿，扫一眼就能分辨收支；
 *  - 多选模式下左侧换成动画选中圈（填充 + 对勾淡入），整行底色轻微染成主色。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    item: TransactionUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val ledgerColors = AppTheme.colors
    val amountColor = if (item.isExpense) ledgerColors.expense else ledgerColors.income
    val tagColor = item.tagColorHex.toComposeColor()

    val rowBackground by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer.copy(alpha = 0.40f) else Color.Transparent,
        animationSpec = tween(180),
        label = "rowSelection",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            SelectionIndicator(selected = selected)
        } else {
            TagAvatar(name = item.tagName, color = tagColor)
        }

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = item.timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (item.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(Spacing.md))

        Text(
            text = item.signedAmountText,
            style = AppTheme.amount.Medium,
            color = amountColor,
            maxLines = 1,
        )
    }
}

/** 按天分组的标题行：左边"今天/昨天/6月1日"，右边当天收支小计 */
@Composable
fun DayGroupHeader(
    group: TransactionDayGroup,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = group.dayLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = group.summaryText,
            style = AppTheme.amount.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 左滑删除时露出的背景（右侧一个圆形删除按钮） */
@Composable
fun SwipeDeleteBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TagAvatar(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(Radius.sm)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "selectContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outline,
        animationSpec = tween(180),
        label = "selectBorder",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "selectCheck",
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .padding(10.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = if (selected) "已选中" else "未选中",
            tint = scheme.onPrimary,
            modifier = Modifier.size(13.dp).alpha(checkAlpha),
        )
    }
}
