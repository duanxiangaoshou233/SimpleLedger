package com.example.ledger.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.ledger.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * SharedPreferences 实现。
 *
 * 关键点：
 *  1) **构造时就同步读出初值**并塞进 StateFlow——这样 App 冷启动第一帧拿到的
 *     就是用户上次选的主题，不会先按系统主题渲染再"闪"一下切成深色；
 *  2) 之后通过 StateFlow 广播，写入方（设置页）与读取方（根 Composable）之间
 *     不需要任何回调，天然解耦；
 *  3) 用 `apply()`（异步落盘）而不是 `commit()`（阻塞主线程做磁盘 IO）。
 *
 * 作用域由 `RepositoryModule` 的 @Binds 决定（这里不重复标 @Singleton，
 * 避免同一个绑定出现两个 scope 声明）。
 */
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val themeModeState = MutableStateFlow(readThemeMode())

    override val themeMode: Flow<ThemeMode> = themeModeState.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModeState.value = mode
        // 注意：这里的 apply() 是 SharedPreferences.Editor.apply()（异步写盘），
        // 不是 Kotlin 的 scope function。
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private fun readThemeMode(): ThemeMode =
        ThemeMode.fromNameOrNull(prefs.getString(KEY_THEME_MODE, null)) ?: ThemeMode.SYSTEM

    private companion object {
        const val PREFS_NAME = "ledger_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
