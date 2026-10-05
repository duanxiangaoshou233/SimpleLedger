package com.example.ledger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ledger.domain.model.ThemeMode

/* ============================================================================
 *  LedgerTheme —— 全站唯一的主题入口
 *  ---------------------------------------------------------------------------
 *  使用方式（阶段 4 由 AppNavHost 注入设置页偏好）：
 *     val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
 *     LedgerTheme(themeMode = uiState.themeMode) { AppNavHost() }
 * ==========================================================================*/

@Composable
fun LedgerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 动态取色（Android 12+ 的 Monet）。默认关闭，保证品牌感一致 */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LedgerDarkColorScheme
        else -> LedgerLightColorScheme
    }
    val ledgerColors = if (darkTheme) LedgerColorsDark else LedgerColorsLight

    // 状态栏 / 导航栏图标明暗跟随主题（覆盖 enableEdgeToEdge 的默认行为）
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content,
        )
    }
}

/* ============================================================================
 *  设计令牌统一访问入口，让 UI 代码保持"读意图"的语义：
 *      AppTheme.colors.expense        // 语义色
 *      AppTheme.spacing.lg            // 间距
 *      AppTheme.shapes.card           // 形状
 *      AppTheme.text.amountHero       // 字体层级
 * ==========================================================================*/
object AppTheme {
    /** 语义扩展色（收入 / 支出 / 图表色板 / 渐变） */
    val colors: LedgerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLedgerColors.current

    /** 间距令牌 */
    val spacing get() = Spacing

    /** 尺寸令牌 */
    val dimens get() = Dimens

    /** 语义形状 */
    val shapes get() = AppShapes

    /** 金额字级 */
    val amount get() = AmountTextStyles
}
