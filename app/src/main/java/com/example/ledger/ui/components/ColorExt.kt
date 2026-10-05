package com.example.ledger.ui.components

import androidx.compose.ui.graphics.Color

/**
 * "#RRGGBB" / "#AARRGGBB" -> Compose Color。
 *
 * 为什么不用 `androidx.core.graphics.toColorInt()`？
 *  - 那是个 Android 平台扩展，在 @Preview/单元测试里会牵扯 android.graphics；
 *  - 自己解析只有 10 行，且能对脏数据做兜底（用户导入的 CSV 可能带奇怪颜色）。
 */
fun String.toComposeColor(fallback: Color = Color(0xFF9AA5A0)): Color {
    val hex = trim().removePrefix("#")
    return try {
        when (hex.length) {
            6 -> Color(0xFF000000L or hex.toLong(16))
            8 -> Color(hex.toLong(16))
            else -> fallback
        }
    } catch (e: NumberFormatException) {
        fallback
    }
}

/**
 * 在某个底色上应该用什么前景色。
 * 用于"选中态 Chip 用标签色填充"时决定文字是黑还是白。
 */
fun Color.readableOnColor(): Color =
    if (luminance() > 0.55f) Color(0xFF1B1B1B) else Color.White
