package com.example.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ledger.ui.navigation.LedgerApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一的 Activity（单 Activity + Compose Navigation 架构）。
 *
 * 这里刻意保持"极薄"：
 *  - `enableEdgeToEdge()`：内容延伸到状态栏/导航栏下方，由各页面用
 *    `statusBarsPadding()` / `navigationBarsPadding()` 消费，做出悬浮导航栏的效果；
 *  - 主题、导航、记账面板、Snackbar 全部由 [LedgerApp] 负责，
 *    Activity 不做任何业务决策（以后要改 UI 框架也不需要动这里）。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            LedgerApp()
        }
    }
}
