package com.example.ledger.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Spacing

/**
 * 统一样式的确认对话框。
 *
 * 与直接用 AlertDialog 的区别：
 *  - 圆角 28dp，与底部弹窗一致，整套 UI 的"软度"统一；
 *  - 顶部有圆形色底图标，删除类操作自动用 error 色，让用户**在读到文字之前**就知道后果；
 *  - destructive 时额外补一行"此操作不可撤销"，减少误删。
 */
@Composable
fun LedgerConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
    dismissText: String = "取消",
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val accentContainer = if (destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppShapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = icon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(it, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (destructive) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "此操作不可撤销",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 颜色选择器里的一格。
 *
 * 选中表现做了三层，而不是简单画个圈：
 *  1) 外侧 38dp 的同色淡底（alpha 0.18）→ 形成"光晕"；
 *  2) 内圆从 30dp 缩到 24dp → 有"按下"的呼吸感；
 *  3) 内圆中央出现对比色对勾。
 */
@Composable
fun ColorSwatch(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = colorHex.toComposeColor()
    val innerSize by animateDpAsState(
        targetValue = if (selected) 24.dp else 30.dp,
        animationSpec = tween(200),
        label = "swatchSize",
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "swatchHalo",
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f * haloAlpha)),
        )
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = color.readableOnColor(),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
