package com.example.ledger.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全 App 共用的 Snackbar 宿主。
 *
 * 为什么放在 CompositionLocal 而不是每个页面各来一个 Scaffold？
 *  - 我们希望 Snackbar 出现在**悬浮底部导航栏之上**的固定位置；
 *  - 如果每个页面自己放 Scaffold，snackbar 会贴到屏幕最底部，被悬浮导航栏盖住；
 *  - 根组件渲染一次 SnackbarHost，任何页面（含全局记账面板）都能直接 showSnackbar。
 *
 * 默认值给一个"空宿主"而不是 error()，这样 @Preview 里不会崩。
 */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }
