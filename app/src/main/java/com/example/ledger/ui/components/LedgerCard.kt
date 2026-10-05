package com.example.ledger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Radius

/**
 * 全 App 统一的内容卡片。
 *
 * 视觉决策：
 *  - 浅色下用 `surfaceContainerLowest`（纯白）+ 1dp 极细边框 + 1dp 柔和阴影，
 *    比"重阴影"更克制、更现代；
 *  - 深色下阴影看不见（黑底黑影），所以边框透明度更高，靠边框分层。
 */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val borderColor = if (isDark) {
        scheme.outlineVariant.copy(alpha = 0.9f)
    } else {
        scheme.outlineVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = AppShapes.card,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 0.dp else 1.dp,
    ) {
        Column(content = content)
    }
}

/**
 * 分组小标题：一根 4dp 圆角竖条 + 标题 +（可选）副标题 + 右侧操作位。
 * 竖条是这套设计系统里最便宜的精致感来源。
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radius.xs / 2)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** 简单的空行占位，避免到处写 Spacer(Modifier.height(x)) */
@Composable
fun VSpace(height: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.height(height))
}

/** 简单的空列占位 */
@Composable
fun HSpace(width: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.width(width))
}

/** 一个圆角小色块（图表图例、标签色点复用） */
@Composable
fun ColorDot(
    colorHex: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 10.dp,
) {
    Box(modifier = modifier.size(size).background(colorHex.toComposeColor(), AppShapes.dot))
}
