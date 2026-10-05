package com.example.ledger.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  间距规范（8dp 栅格 + 4dp 半步）
 *  ---------------------------------------------------------------------------
 *  使用规则（写 UI 时照此执行，避免出现 10dp / 14dp 这类"随手值"）：
 *   · 组件内部 padding：sm(8) / md(12)
 *   · 卡片内部 padding：lg(16)
 *   · 卡片之间、模块之间：md(12) / lg(16)
 *   · 页面左右安全边距：xl(24)
 *   · 大模块（不同语义区块）之间：xxl(32)
 * ==========================================================================*/
object Spacing {
    /** 4dp —— 图标与文字之间 */
    val xs: Dp = 4.dp

    /** 8dp —— 相关元素之间 */
    val sm: Dp = 8.dp

    /** 12dp —— 卡片间距 */
    val md: Dp = 12.dp

    /** 16dp —— 卡片内边距 / 模块间距 */
    val lg: Dp = 16.dp

    /** 24dp —— 页面左右边距 / 大间距 */
    val xl: Dp = 24.dp

    /** 32dp —— 语义区块间距 */
    val xxl: Dp = 32.dp

    /** 48dp —— 空状态、首屏留白 */
    val xxxl: Dp = 48.dp
}

/** 常用尺寸常量，避免各处硬编码 */
object Dimens {
    /** 内容区左右统一边距 */
    val screenHorizontal: Dp = Spacing.lg

    /** 列表项最小高度，保证可点面积 >= 48dp */
    val listItemMinHeight: Dp = 64.dp

    /** 底部导航条高度（不含系统栏） */
    val bottomBarHeight: Dp = 64.dp

    /** 悬浮导航条距屏幕底部的浮动距离 */
    val bottomBarFloatMargin: Dp = 12.dp

    /** 数字键盘按键高度 */
    val keypadKeyHeight: Dp = 56.dp

    /** 图表高度 */
    val chartHeight: Dp = 200.dp

    /** 环形图直径 */
    val donutSize: Dp = 180.dp
}
