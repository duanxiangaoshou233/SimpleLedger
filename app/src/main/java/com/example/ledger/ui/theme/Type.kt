package com.example.ledger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  字体层级
 *  ---------------------------------------------------------------------------
 *  字体族：默认使用系统字体（中文设备上即思源黑体 / MiSans / HarmonyOS Sans），
 *  这样体积最小、中文渲染最稳。若想换成自有字体，把 .ttf 放进 res/font/ 后
 *  改下面的 LedgerFontFamily 即可（例如 FontFamily(Font(R.font.mi_sans)) ）。
 *
 *  中文排版要点：
 *   · 中文字形比拉丁字母"高"，行高给到 1.4~1.5 倍才不挤；
 *   · 标题字重不要超过 SemiBold，中文加粗过重会糊成一块；
 *   · 金额数字开启 tnum（等宽数字），列表里多行数字才能竖向对齐。
 * ==========================================================================*/
val LedgerFontFamily: FontFamily = FontFamily.Default

/** 金额专用字族：等宽，保证滚动时不跳动 */
val AmountFontFamily: FontFamily = FontFamily.Monospace

val LedgerTypography = Typography(
    // -------- 大标题（首屏问候、页面大标题）--------
    displayLarge = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),

    // -------- 标题 --------
    headlineLarge = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    // -------- 正文 --------
    bodyLarge = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    ),

    // -------- 说明文字 / 标签 --------
    labelLarge = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = LedgerFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/* ============================================================================
 *  金额字级（设计系统的"第六级"）
 *  ---------------------------------------------------------------------------
 *  金额是本 App 的视觉主角，因此单独定义 5 档：
 *   Hero   44sp  首页总资产 / 统计页总额
 *   Large  28sp  卡片内的大数字、图表中心
 *   Medium 20sp  列表项金额
 *   Small  16sp  次级金额（如"共 12 笔"里的金额）
 *   Micro  12sp  图表坐标轴数值
 *  数字开启等宽（tnum）+ 收紧字距，中文环境下"钱感"更专业。
 * ==========================================================================*/
object AmountTextStyles {
    val Hero = TextStyle(
        fontFamily = AmountFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum",
    )
    val Large = TextStyle(
        fontFamily = AmountFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )
    val Medium = TextStyle(
        fontFamily = AmountFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum",
    )
    val Small = TextStyle(
        fontFamily = AmountFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
    )
    val Micro = TextStyle(
        fontFamily = AmountFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum",
    )
}
