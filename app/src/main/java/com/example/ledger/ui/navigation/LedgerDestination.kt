package com.example.ledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 全部导航目的地。
 *
 * 用枚举而不是一堆字符串常量：
 *  - 底部导航栏需要"顺序 + 选中/未选中两个图标 + 文案"，枚举天然承载这些元数据；
 *  - 路由名与 UI 定义放在一起，改名字不会漏改。
 *
 * [RECORDS_ROUTE] 是**二级页面**（记录列表），不在底部导航里，
 * 从首页"最近记录 → 全部记录"进入。
 */
enum class LedgerDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(
        route = "home",
        label = "记账",
        selectedIcon = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
    ),
    STATS(
        route = "stats",
        label = "统计",
        selectedIcon = Icons.Rounded.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
    ),
    TAGS(
        route = "tags",
        label = "标签",
        selectedIcon = Icons.Rounded.Sell,
        unselectedIcon = Icons.Outlined.Sell,
    ),
    SETTINGS(
        route = "settings",
        label = "设置",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
    ;

    companion object {
        /** 二级页面：记录列表 */
        const val RECORDS_ROUTE = "records"

        val bottomBarDestinations: List<LedgerDestination> = entries.toList()

        fun fromRoute(route: String?): LedgerDestination? =
            entries.firstOrNull { it.route == route }
    }
}
