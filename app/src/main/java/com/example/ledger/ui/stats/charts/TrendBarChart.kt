package com.example.ledger.ui.stats.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TrendGranularity
import com.example.ledger.domain.model.TrendPoint
import com.example.ledger.ui.theme.Dimens
import kotlin.math.max
import kotlin.math.min

/**
 * 支出/收入趋势柱状图（自研 Compose Canvas）。
 *
 * 视觉规格（针对"默认图表很丑"逐条优化）：
 *  - **圆角胶囊柱**：柱宽的一半作为圆角半径，柱顶柱底都是圆的，比直角柱现代得多；
 *  - **竖向渐变**：柱体从 100% 不透明度渐变到 45%，同一个颜色也有体积感；
 *  - **淡化坐标轴**：只保留 4 条极淡的水平网格线（outlineVariant 的 40%），不画竖线与外框；
 *  - **数值在场**：点击某根柱子，柱顶直接显示金额；Y 轴刻度用 `1.2万 / 3,456` 紧凑格式；
 *  - **入场动画**：900ms FastOutSlowIn + 每根柱 1/40 的错峰，从左到右"长出来"；
 *  - 金额为 0 的桶画一个 3dp 的小横条，保证时间轴连续、不会看着像缺数据。
 *
 * @param selectedIndex 当前选中的柱子（点击切换），null 表示没有选中
 */
@Composable
fun TrendBarChart(
    points: List<TrendPoint>,
    barColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = Dimens.chartHeight,
    granularity: TrendGranularity = TrendGranularity.DAY,
    selectedIndex: Int? = null,
    onBarClick: (Int?) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val gridColor = scheme.outlineVariant.copy(alpha = 0.40f)
    val labelColor = scheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                detectTapGestures { position ->
                    if (points.isEmpty()) return@detectTapGestures
                    val gutter = 46.dp.toPx()
                    val chartWidth = size.width - gutter
                    val step = chartWidth / points.size
                    val index = ((position.x - gutter) / step).toInt().coerceIn(0, points.lastIndex)
                    onBarClick(if (index == selectedIndex) null else index)
                }
            },
    ) {
        if (points.isEmpty()) return@Canvas

        val gutter = 46.dp.toPx()
        val labelArea = 24.dp.toPx()
        val chartHeight = size.height - labelArea
        val chartWidth = size.width - gutter

        val maxAmount = (points.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)

        // ---------------- 网格 + Y 轴刻度 ----------------
        val gridLines = 3
        repeat(gridLines + 1) { i ->
            val y = chartHeight * i / gridLines
            drawLine(
                color = gridColor,
                start = Offset(gutter, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            val value = maxAmount * (gridLines - i) / gridLines
            val layout = textMeasurer.measure(
                text = MoneyFormatter.formatCompact(value),
                style = labelStyle,
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (gutter - 8.dp.toPx() - layout.size.width).coerceAtLeast(0f),
                    y = y - layout.size.height / 2f,
                ),
            )
        }

        // ---------------- 柱子 ----------------
        val step = chartWidth / points.size
        val barWidth = min(step * 0.58f, 22.dp.toPx())
        val stagger = 0.5f / max(points.size, 1)
        val span = 0.62f

        points.forEachIndexed { index, point ->
            val local = ((progress.value - index * stagger) / span).coerceIn(0f, 1f)
            val ratio = point.amount.toFloat() / maxAmount.toFloat()
            val baseY = chartHeight - 4.dp.toPx()
            val left = gutter + step * index + (step - barWidth) / 2f

            if (point.amount <= 0L) {
                // 零值：画一根小横杠，表示"这一天有记录但是 0"或"没有记录"，保持时间轴连续
                drawRoundRect(
                    color = barColor.copy(alpha = 0.28f),
                    topLeft = Offset(left, baseY - 3.dp.toPx()),
                    size = Size(barWidth, 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            } else {
                val barHeight = ((baseY - 6.dp.toPx()) * ratio * local)
                    .coerceAtLeast(min(barWidth, 4.dp.toPx()))
                val top = baseY - barHeight
                val isSelected = index == selectedIndex

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            barColor,
                            barColor.copy(alpha = if (isSelected) 0.75f else 0.45f),
                        ),
                        startY = top,
                        endY = top + barHeight,
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )

                if (isSelected) {
                    val layout = textMeasurer.measure(
                        text = MoneyFormatter.format(point.amount),
                        style = TextStyle(fontSize = 10.sp, color = barColor),
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = (left + barWidth / 2f - layout.size.width / 2f)
                                .coerceIn(gutter, size.width - layout.size.width),
                            y = (top - layout.size.height - 4.dp.toPx()).coerceAtLeast(0f),
                        ),
                    )
                }
            }
        }

        // ---------------- X 轴标签（最多 6 个，避免重叠） ----------------
        val labelEvery = max(1, points.size / 6)
        points.forEachIndexed { index, point ->
            val isLast = index == points.lastIndex
            if (index % labelEvery != 0 && !isLast) return@forEachIndexed
            if (isLast && points.size > 1 && index % labelEvery == 0) return@forEachIndexed

            val layout = textMeasurer.measure(text = point.label, style = labelStyle)
            val centerX = gutter + step * index + step / 2f
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (centerX - layout.size.width / 2f)
                        .coerceIn(0f, size.width - layout.size.width),
                    y = chartHeight + 6.dp.toPx(),
                ),
            )
        }
    }
}
