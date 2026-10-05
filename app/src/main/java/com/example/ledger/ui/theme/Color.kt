package com.example.ledger.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/* ============================================================================
 *  简易记账 · 设计系统 —— 颜色
 *  ---------------------------------------------------------------------------
 *  设计思路：
 *  1) 主色取"翡翠青"（Jade），冷静、克制，长时间看不疲劳，契合"记账"场景；
 *  2) 辅助色用"石板青灰"承担次要操作，避免全屏都是同一个绿；
 *  3) 强调色用"琥珀金"，只用于关键数字、图表高亮、成就类提示；
 *  4) 语义色：支出=珊瑚红、收入=青翠绿（中文记账 App 的通用心理预期）；
 *  5) 图表色板 8 色，已做同屏可辨识度与明度均衡校验，避免"荧光乱撞"。
 * ==========================================================================*/

// ---------------------------------------------------------------- 品牌主色 翡翠青
private val JadeLightPrimary = Color(0xFF1E7A66)
private val JadeLightOnPrimary = Color(0xFFFFFFFF)
private val JadeLightPrimaryContainer = Color(0xFFC2EBDD)
private val JadeLightOnPrimaryContainer = Color(0xFF00251C)
private val JadeLightInversePrimary = Color(0xFF6FD8BC)

private val JadeDarkPrimary = Color(0xFF6FD8BC)
private val JadeDarkOnPrimary = Color(0xFF00382A)
private val JadeDarkPrimaryContainer = Color(0xFF00513F)
private val JadeDarkOnPrimaryContainer = Color(0xFF8CF5D8)
private val JadeDarkInversePrimary = Color(0xFF1E7A66)

// ---------------------------------------------------------------- 辅助色 石板青灰
private val StoneLightSecondary = Color(0xFF4A635C)
private val StoneLightSecondaryContainer = Color(0xFFCCE8DF)
private val StoneLightOnSecondaryContainer = Color(0xFF06201A)

private val StoneDarkSecondary = Color(0xFFAECDC4)
private val StoneDarkOnSecondary = Color(0xFF1B3530)
private val StoneDarkSecondaryContainer = Color(0xFF334B45)
private val StoneDarkOnSecondaryContainer = Color(0xFFCAE9E0)

// ---------------------------------------------------------------- 强调色 琥珀金
private val AmberLightTertiary = Color(0xFF8A6100)
private val AmberLightTertiaryContainer = Color(0xFFFFDF9B)
private val AmberLightOnTertiaryContainer = Color(0xFF2B1D00)

private val AmberDarkTertiary = Color(0xFFF5C24B)
private val AmberDarkOnTertiary = Color(0xFF473100)
private val AmberDarkTertiaryContainer = Color(0xFF664700)
private val AmberDarkOnTertiaryContainer = Color(0xFFFFDF9B)

// ---------------------------------------------------------------- 浅色主题 ColorScheme
val LedgerLightColorScheme = lightColorScheme(
    primary = JadeLightPrimary,
    onPrimary = JadeLightOnPrimary,
    primaryContainer = JadeLightPrimaryContainer,
    onPrimaryContainer = JadeLightOnPrimaryContainer,
    inversePrimary = JadeLightInversePrimary,

    secondary = StoneLightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = StoneLightSecondaryContainer,
    onSecondaryContainer = StoneLightOnSecondaryContainer,

    tertiary = AmberLightTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = AmberLightTertiaryContainer,
    onTertiaryContainer = AmberLightOnTertiaryContainer,

    background = Color(0xFFF4F8F6),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFAFDFB),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDDE5E1),
    onSurfaceVariant = Color(0xFF404944),
    surfaceTint = JadeLightPrimary,

    inverseSurface = Color(0xFF2B3230),
    inverseOnSurface = Color(0xFFECF1EE),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFC0C9C4),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFFAFDFB),
    surfaceDim = Color(0xFFD5DBD8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F8F6),
    surfaceContainer = Color(0xFFEEF3F0),
    surfaceContainerHigh = Color(0xFFE8EEEB),
    surfaceContainerHighest = Color(0xFFE2E8E5),
)

// ---------------------------------------------------------------- 深色主题 ColorScheme
val LedgerDarkColorScheme = darkColorScheme(
    primary = JadeDarkPrimary,
    onPrimary = JadeDarkOnPrimary,
    primaryContainer = JadeDarkPrimaryContainer,
    onPrimaryContainer = JadeDarkOnPrimaryContainer,
    inversePrimary = JadeDarkInversePrimary,

    secondary = StoneDarkSecondary,
    onSecondary = StoneDarkOnSecondary,
    secondaryContainer = StoneDarkSecondaryContainer,
    onSecondaryContainer = StoneDarkOnSecondaryContainer,

    tertiary = AmberDarkTertiary,
    onTertiary = AmberDarkOnTertiary,
    tertiaryContainer = AmberDarkTertiaryContainer,
    onTertiaryContainer = AmberDarkOnTertiaryContainer,

    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E0),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceTint = JadeDarkPrimary,

    inverseSurface = Color(0xFFDDE4E0),
    inverseOnSurface = Color(0xFF2B3230),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF3F4945),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF343B38),
    surfaceDim = Color(0xFF0E1513),
    surfaceContainerLowest = Color(0xFF090F0D),
    surfaceContainerLow = Color(0xFF171E1C),
    surfaceContainer = Color(0xFF1B2220),
    surfaceContainerHigh = Color(0xFF252C2A),
    surfaceContainerHighest = Color(0xFF303735),
)

/* ============================================================================
 *  语义扩展色：Material 的 ColorScheme 没有"收入/支出/图表"角色，
 *  用自定义 LedgerColors + CompositionLocal 注入，保证浅深色都成对定义。
 * ==========================================================================*/
data class LedgerColors(
    /** 支出主色（珊瑚红） */
    val expense: Color,
    /** 支出浅底（用于 Chip / 徽标底色） */
    val expenseContainer: Color,
    /** 支出浅底上的文字色 */
    val onExpenseContainer: Color,
    /** 收入主色（青翠绿） */
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    /** 结余为正 / 为负时的强调色 */
    val balancePositive: Color,
    val balanceNegative: Color,
    /** 首页头图渐变（顶部 -> 底部） */
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    /** 图表 8 色板，顺序即默认分配顺序 */
    val chartColors: List<Color>,
)

val LedgerColorsLight = LedgerColors(
    expense = Color(0xFFE2574C),
    expenseContainer = Color(0xFFFFE4DF),
    onExpenseContainer = Color(0xFF93261A),
    income = Color(0xFF159C77),
    incomeContainer = Color(0xFFD6F5E4),
    onIncomeContainer = Color(0xFF06483A),
    balancePositive = Color(0xFF159C77),
    balanceNegative = Color(0xFFE2574C),
    heroGradientStart = Color(0xFF1F7D69),
    heroGradientEnd = Color(0xFF2AA98C),
    chartColors = listOf(
        Color(0xFF2E9E8F), // 青绿
        Color(0xFF4F86E0), // 湖蓝
        Color(0xFFF0A93B), // 琥珀
        Color(0xFF9C7BF0), // 藤紫
        Color(0xFFE86BA0), // 樱粉
        Color(0xFF3FAE6A), // 苔绿
        Color(0xFFF08A4B), // 陶橙
        Color(0xFF5BB4D4), // 天青
    ),
)

val LedgerColorsDark = LedgerColors(
    expense = Color(0xFFFF8F80),
    expenseContainer = Color(0xFF5C1B12),
    onExpenseContainer = Color(0xFFFFDAD4),
    income = Color(0xFF4FD3A5),
    incomeContainer = Color(0xFF0B3F30),
    onIncomeContainer = Color(0xFFB9F2DF),
    balancePositive = Color(0xFF4FD3A5),
    balanceNegative = Color(0xFFFF8F80),
    heroGradientStart = Color(0xFF13584A),
    heroGradientEnd = Color(0xFF1E8A73),
    chartColors = listOf(
        Color(0xFF4FD1C5),
        Color(0xFF7AA8FF),
        Color(0xFFFFC061),
        Color(0xFFBFA5FF),
        Color(0xFFFF8FC7),
        Color(0xFF52E0AE),
        Color(0xFFFFA95C),
        Color(0xFF86C5FF),
    ),
)

/** 供 UI 层读取语义色：AppTheme.colors.expense */
val LocalLedgerColors = staticCompositionLocalOf { LedgerColorsLight }
