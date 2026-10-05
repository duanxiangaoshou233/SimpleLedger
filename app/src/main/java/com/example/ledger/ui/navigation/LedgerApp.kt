package com.example.ledger.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.ui.components.LocalSnackbarHostState
import com.example.ledger.ui.home.HomeScreen
import com.example.ledger.ui.record.RecordEditorEffect
import com.example.ledger.ui.record.RecordEditorSheet
import com.example.ledger.ui.record.RecordEditorViewModel
import com.example.ledger.ui.records.RecordsScreen
import com.example.ledger.ui.settings.SettingsScreen
import com.example.ledger.ui.settings.SettingsViewModel
import com.example.ledger.ui.stats.StatsScreen
import com.example.ledger.ui.tags.TagsScreen
import com.example.ledger.ui.theme.LedgerTheme

/**
 * App 根组件。
 *
 * 职责（也是整个 App 唯一"接线"的地方）：
 *  1) **主题**：订阅 `SettingsRepository.themeMode`，设置页改一下 → 这里立刻换配色；
 *  2) **导航**：4 个底部页签 + 1 个二级页面（记录列表），带淡入淡出/滑动过渡；
 *  3) **全局记账面板**：`RecordEditorViewModel` 是 Activity 作用域的，
 *     面板在这里只渲染一份，任何页面都能唤起同一个编辑器；
 *  4) **Snackbar**：通过 CompositionLocal 下发，位置固定在悬浮导航栏之上。
 *
 * 关键设计：`hiltViewModel()` 在这里取到的是 **Activity 作用域**的实例
 * （因为 LedgerApp 不在 NavHost 的目的地里），所以记账面板跨页面共享，
 * 而各页面自己的 ViewModel 仍然是页面内的独立实例。
 */
@Composable
fun LedgerApp() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val editorViewModel: RecordEditorViewModel = hiltViewModel()
    val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 记账面板的提示（保存成功、创建标签…）
    LaunchedEffect(Unit) {
        editorViewModel.effects.collect { effect ->
            when (effect) {
                is RecordEditorEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LedgerTheme(themeMode = settingsState.themeMode) {
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = LedgerDestination.HOME.route,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 14 }
                    },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(200)) },
                    popExitTransition = {
                        fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { it / 14 }
                    },
                ) {
                    composable(route = LedgerDestination.HOME.route) {
                        HomeScreen(
                            onOpenRecords = { navController.navigate(LedgerDestination.RECORDS_ROUTE) },
                            onAddRecord = { type -> editorViewModel.openForCreate(type) },
                            onEditRecord = { id -> editorViewModel.openForEditById(id) },
                        )
                    }

                    composable(route = LedgerDestination.STATS.route) {
                        StatsScreen()
                    }

                    composable(route = LedgerDestination.TAGS.route) {
                        TagsScreen()
                    }

                    composable(route = LedgerDestination.SETTINGS.route) {
                        SettingsScreen()
                    }

                    composable(
                        route = LedgerDestination.RECORDS_ROUTE,
                        // 二级页面用整屏横推，和页签之间的淡入区分开
                        enterTransition = { slideInHorizontally(tween(260)) { it } },
                        popExitTransition = { slideOutHorizontally(tween(240)) { it } },
                    ) {
                        RecordsScreen(
                            onBack = { navController.popBackStack() },
                            onAddRecord = { type -> editorViewModel.openForCreate(type) },
                            onEditRecord = { id -> editorViewModel.openForEditById(id) },
                        )
                    }
                }

                // 底部导航：只在四个主页面显示（二级页面隐藏）
                if (LedgerDestination.fromRoute(currentRoute) != null) {
                    LedgerBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                // Snackbar 固定在悬浮导航栏之上，不会被挡住
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 108.dp, start = 16.dp, end = 16.dp),
                )
            }

            // ---------------- 全局记账 / 编辑面板 ----------------
            RecordEditorSheet(
                state = editorState,
                onDismiss = editorViewModel::dismiss,
                onTypeChange = editorViewModel::setType,
                onDigit = editorViewModel::onDigit,
                onDecimal = editorViewModel::onDecimal,
                onBackspace = editorViewModel::onBackspace,
                onClear = editorViewModel::onClearAmount,
                onSave = editorViewModel::save,
                onTagSelected = editorViewModel::onTagSelected,
                onNoteChange = editorViewModel::onNoteChange,
                onDateChange = editorViewModel::onDateChange,
                onTimeChange = editorViewModel::onTimeChange,
                onNewTagClick = editorViewModel::openNewTagDialog,
                onNewTagNameChange = editorViewModel::onNewTagNameChange,
                onNewTagColorChange = editorViewModel::onNewTagColorChange,
                onNewTagConfirm = editorViewModel::confirmNewTag,
                onNewTagDismiss = editorViewModel::dismissNewTagDialog,
            )
        }
    }
}

/** 便捷方法：默认回到首页（预留给后续"记完账回到首页"之类的跳转） */
internal fun androidx.navigation.NavHostController.navigateToHome() {
    navigate(LedgerDestination.HOME.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 供未来的快捷方式/通知入口使用 */
@Suppress("unused")
internal val defaultAddType: TransactionType = TransactionType.EXPENSE
