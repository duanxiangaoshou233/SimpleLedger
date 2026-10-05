package com.example.ledger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Spacing

/**
 * 标签 Chip。
 *
 * 和 Material 的 FilterChip 相比，这里完全自己画，换来两个关键能力：
 *  1) **选中态直接填充标签自身的颜色**（FilterChip 想做这件事要覆写一堆 token）；
 *  2) 按下时整体 scale 到 0.94 的触感反馈（FilterChip 默认只有涟漪）。
 *
 * Chip 左侧始终有小色点，即使未选中也能一眼看出标签颜色。
 *
 * 涟漪用 `LocalIndication.current`（Material3 主题注入的 ripple），
 * 这样不需要依赖 `rememberRipple`（它在后续 Compose 版本里被废弃/移除）。
 */
@Composable
fun TagChip(
    name: String,
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** "无标签"这种特殊 Chip 传 true：不显示色点、用中性灰 */
    neutral: Boolean = false,
) {
    val tagColor = if (neutral) MaterialTheme.colorScheme.onSurfaceVariant else colorHex.toComposeColor()

    val containerColor by animateColorAsState(
        targetValue = if (selected) tagColor else tagColor.copy(alpha = 0.10f),
        animationSpec = tween(220),
        label = "chipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) tagColor.readableOnColor() else tagColor,
        animationSpec = tween(220),
        label = "chipContent",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "chipScale",
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(AppShapes.chip)
            .background(containerColor, AppShapes.chip)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (selected) Color.Transparent else tagColor.copy(alpha = 0.28f),
                ),
                shape = AppShapes.chip,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!neutral) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(contentColor.copy(alpha = 0.85f), AppShapes.dot),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}

/**
 * 只有图标的小圆按钮（列表右侧的编辑/删除、顶栏的返回等）。
 * 统一 40dp 触控区 + 圆形涟漪，避免各处自己拼 IconButton 导致大小不一。
 */
@Composable
fun LedgerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(AppShapes.dot)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** 统一样式的小分隔线（比默认 Divider 更淡） */
@Composable
fun LedgerDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}
