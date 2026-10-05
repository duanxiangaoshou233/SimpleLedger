package com.example.ledger.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Dimens
import com.example.ledger.ui.theme.Spacing

/**
 * 悬浮式底部导航栏。
 *
 * 视觉与交互决策：
 *  1) **悬浮**：左右 16dp、上下浮起 12dp，圆角 24dp + 12dp 投影，
 *     内容列表可以从中"穿过去"，比贴底的 M3 NavigationBar 现代得多；
 *  2) **半透明**：容器 alpha 0.94，滚动时能隐约透出下方的账单，
 *     配合 1dp 极细描边保证在任何内容上都清晰可辨（不做真实模糊：API 31 以下没有 RenderEffect，
 *     为一份背毛玻璃引入跨版本分支不划算，0.94 的半透明已经有类似观感）；
 *  3) **药丸指示器**：选中项的胶囊背景用 spring 滑动过去，而不是四个独立背景淡入淡出；
 *  4) 点击时图标 scale 到 0.92 再回弹，选中项图标加粗线（Rounded）未选中用线框（Outlined）。
 */
@Composable
fun LedgerBottomBar(
    currentRoute: String?,
    onSelect: (LedgerDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val destinations = LedgerDestination.bottomBarDestinations
    val selectedIndex = destinations
        .indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        shape = AppShapes.bottomBar,
        color = scheme.surfaceContainer.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = if (isDark) 0.8f else 0.5f)),
        shadowElevation = 12.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.bottomBarHeight)
                .padding(Spacing.xs),
        ) {
            val itemWidth = maxWidth / destinations.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "barIndicator",
            )

            // 滑动药丸：位于所有 item 之下，靠 offset 平移到选中项
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(AppShapes.chip)
                    .background(scheme.primaryContainer.copy(alpha = if (isDark) 0.45f else 0.75f)),
            )

            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                destinations.forEachIndexed { index, destination ->
                    BarItem(
                        destination = destination,
                        selected = index == selectedIndex,
                        onClick = { onSelect(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BarItem(
    destination: LedgerDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 700f),
        label = "barIconScale",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(AppShapes.chip)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.label,
            tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp).scale(iconScale),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}
