package com.example.ledger.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Radius
import com.example.ledger.ui.theme.Spacing

/**
 * 骨架屏。
 *
 * 比转圈优雅的地方：**布局和真实内容一致**，数据到达时不会"跳一下"。
 * 微光扫过用 `rememberInfiniteTransition` + 来回移动的线性渐变实现，
 * 成本极低（不额外起协程、不做模糊、不申请额外内存）。
 */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
) {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerHigh
    val highlight = scheme.surfaceContainerLowest
    val progress = rememberShimmerProgress()

    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) { index ->
            val contentAlpha = (1f - index * 0.12f).coerceAtLeast(0.4f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerLowest.copy(alpha = 0.6f), AppShapes.card)
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .shimmer(base, highlight, progress, RoundedCornerShape(Radius.sm)),
                )
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.45f * contentAlpha)
                            .shimmer(base, highlight, progress, CircleShape),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Box(
                        Modifier
                            .height(12.dp)
                            .fillMaxWidth(0.70f * contentAlpha)
                            .shimmer(base, highlight, progress, CircleShape),
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Box(
                    Modifier
                        .height(18.dp)
                        .width(72.dp)
                        .shimmer(base, highlight, progress, CircleShape),
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

/** 卡片内部的骨架块（例如 Hero 卡的金额占位） */
@Composable
fun SkeletonBlock(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    val scheme = MaterialTheme.colorScheme
    val progress = rememberShimmerProgress()
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shimmer(scheme.surfaceContainerHigh, scheme.surfaceContainerLowest, progress, shape),
    )
}

@Composable
private fun rememberShimmerProgress(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return progress
}

/** 把"移动的高光渐变"做成一个 Modifier 扩展，避免在 @Composable 里写局部函数 */
private fun Modifier.shimmer(
    base: androidx.compose.ui.graphics.Color,
    highlight: androidx.compose.ui.graphics.Color,
    progress: Float,
    shape: Shape,
): Modifier = background(
    brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x = progress * 900f - 300f, y = 0f),
        end = Offset(x = progress * 900f, y = 220f),
    ),
    shape = shape,
)
