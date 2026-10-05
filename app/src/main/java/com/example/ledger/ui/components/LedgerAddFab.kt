package com.example.ledger.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Spacing

/**
 * 首页主行动按钮（FAB）。
 *
 * 为什么不用 Material 的 FloatingActionButton？
 *  - 我们希望它是"渐变胶囊 + 图标 + 文案"（记账场景里"记一笔"三个字比一个加号更明确）；
 *  - 按下时整体缩到 0.93 并带 spring 回弹，比默认涟漪更有"按下去"的实感。
 */
@Composable
fun LedgerAddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "记一笔",
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 700f),
        label = "fabScale",
    )

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 10.dp, shape = AppShapes.fab, clip = false)
            .clip(AppShapes.fab)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppTheme.colors.heroGradientStart,
                        AppTheme.colors.heroGradientEnd,
                    ),
                ),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}
