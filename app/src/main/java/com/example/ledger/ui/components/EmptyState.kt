package com.example.ledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Spacing

/**
 * 空状态。
 *
 * 设计要点（"不能一片空白"）：
 *  - 112dp 的圆形渐变底 + 48dp 图标，形成视觉锚点；
 *  - 主文案 16sp SemiBold，副文案 12sp 灰色并限制宽度（中文长句居中超过 2 行很难看）；
 *  - 可选的行动按钮，把"空"变成"下一步做什么"。
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.85f),
                            scheme.primaryContainer.copy(alpha = 0.15f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.82f),
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.xl))
            FilledTonalButton(onClick = onAction, shape = AppShapes.field) {
                Text(actionLabel)
            }
        }
    }
}
