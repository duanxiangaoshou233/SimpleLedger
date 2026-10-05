package com.example.ledger.ui.stats.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TagAmount
import com.example.ledger.ui.components.toComposeColor
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.atan2

/**
 * 环形图（自研 Compose Canvas，不依赖任何图表库）。
 *
 * 为什么自己画？
 *  - Vico 没有饼图/环形图，MPAndroidChart 是 View 体系且默认样式很丑；
 *  - 自己画可以精确控制：**环宽、切片间隙、圆角、入场动画、点击命中、中心文案**。
 *
 * 视觉规格：
 *  - 环宽 18dp，切片之间留 2° 间隙（只有一片时不留），比"无缝拼色"更透气；
 *  - 未选中的切片在"图例高亮"时降到 22% 透明度，形成聚焦；
 *  - 入场动画 900ms FastOutSlowIn，每次数据变化都重播（Animatable.snapTo(0) 再 animateTo）。
 *
 * @param highlightTagId 图例/榜单当前高亮的标签；null 表示不高亮任何一个
 * @param onSliceClick 点击某个环段（回调 tagId，null 表示"无标签"分组）
 */
@Composable
fun DonutChart(
    data: List<TagAmount>,
    highlightTagId: Long?,
    onSliceClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = Dimens.donutSize,
    strokeWidth: Dp = 18.dp,
    centerTitle: String = "总额",
    centerAmountInCents: Long = 0L,
    /** 中心金额的颜色：由调用方传入支出红/收入绿，图表本身不猜语义 */
    centerAmountColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.surfaceContainerHigh

    val sweepProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }
    val progress = sweepProgress.value

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(diameter)
                .pointerInput(data, highlightTagId) {
                    detectTapGestures { position ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val distance = (position - center).getDistance()
                        // 注意：pointerInput 里的 size 是 IntSize（只有 width/height），
                        // minDimension 是 geometry.Size 才有的属性，别混用
                        val outer = minOf(size.width, size.height) / 2f
                        val inner = outer - strokeWidth.toPx() - 6.dp.toPx()
                        if (distance in inner..outer) {
                            // 画布 0° 在 3 点方向，我们从 -90°（12 点）开始，因此补 90°
                            val angle = (
                                Math.toDegrees(
                                    atan2(
                                        (position.y - center.y).toDouble(),
                                        (position.x - center.x).toDouble(),
                                    ),
                                ) + 90.0 + 360.0
                                ) % 360.0
                            var cursor = 0f
                            data.forEach { slice ->
                                val sweep = slice.ratio * 360f
                                if (angle >= cursor && angle < cursor + sweep) {
                                    onSliceClick(slice.tagId)
                                    return@detectTapGestures
                                }
                                cursor += sweep
                            }
                        }
                    }
                },
        ) {
            val stroke = strokeWidth.toPx()
            val arcDiameter = size.minDimension - stroke
            val topLeft = Offset(
                x = (size.width - arcDiameter) / 2f,
                y = (size.height - arcDiameter) / 2f,
            )
            val arcSize = Size(arcDiameter, arcDiameter)

            // 轨道底环
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )

            if (data.isEmpty()) return@Canvas

            // 只有一片时不留间隙，否则会出现一个突兀的缺口
            val gap = if (data.size > 1) 2f else 0f
            var startAngle = -90f

            data.forEach { slice ->
                val fullSweep = slice.ratio * 360f
                val visibleSweep = (fullSweep * progress - gap).coerceAtLeast(0f)
                val dimmed = highlightTagId != null && highlightTagId != slice.tagId
                val sliceColor = slice.colorHex.toComposeColor()

                drawArc(
                    color = if (dimmed) sliceColor.copy(alpha = 0.22f) else sliceColor,
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = visibleSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += fullSweep * progress
            }
        }

        // ---------------- 中心文案 ----------------
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = MoneyFormatter.format(centerAmountInCents),
                style = AppTheme.amount.Large,
                color = if (data.isEmpty()) {
                    scheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    centerAmountColor
                },
            )
        }
    }
}
