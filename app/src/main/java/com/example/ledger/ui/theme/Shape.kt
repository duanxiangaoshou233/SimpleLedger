package com.example.ledger.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  圆角规范
 *  ---------------------------------------------------------------------------
 *  设计约束：小圆角 8dp / 中圆角 16dp / 大圆角 24dp / Chip 用胶囊形。
 *  额外补一个 12dp（卡片内的次级容器）与 32dp（底部弹窗顶部圆角），
 *  避免出现"所有容器同一圆角"的廉价感。
 * ==========================================================================*/
object Radius {
    /** 8dp —— 小元素：色块、徽标、迷你按钮 */
    val xs = 8.dp

    /** 12dp —— 次级容器：输入框内嵌块、列表内的缩略图 */
    val sm = 12.dp

    /** 16dp —— 中圆角：按钮、输入框、Chip 组容器 */
    val md = 16.dp

    /** 20dp —— 卡片（比中圆角更"软"，但仍克制） */
    val card = 20.dp

    /** 24dp —— 大圆角：大卡片、底部导航悬浮条 */
    val lg = 24.dp

    /** 28dp —— 底部弹窗、全宽 Sheet */
    val sheet = 28.dp

    /** 32dp —— 超大圆角：Hero 卡片刻意呼应屏幕圆角 */
    val xl = 32.dp
}

/** 供 Material 组件自动继承的 Shapes（Card / Dialog / Menu 等） */
val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

/** 语义化形状：让 UI 代码读起来是"意图"而不是"数字" */
object AppShapes {
    /** 普通内容卡片 */
    val card = RoundedCornerShape(Radius.card)

    /** 首页 Hero / 大金额卡片 */
    val heroCard = RoundedCornerShape(Radius.xl)

    /** 底部弹窗（只圆上面两个角） */
    val bottomSheet = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)

    /** Chip：胶囊形 */
    val chip = CircleShape

    /** 悬浮按钮：比圆角矩形更有"现代工具"感 */
    val fab = RoundedCornerShape(Radius.card)

    /** 悬浮式底部导航条 */
    val bottomBar = RoundedCornerShape(Radius.lg)

    /** 输入框 / 文本域 */
    val field = RoundedCornerShape(Radius.md)

    /** 迷你标签色点 */
    val dot = CircleShape
}
