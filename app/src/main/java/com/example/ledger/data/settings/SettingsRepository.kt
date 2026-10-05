package com.example.ledger.data.settings

import com.example.ledger.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * 应用偏好设置仓库。
 *
 * 目前只有主题模式（跟随系统/浅色/深色）。
 *
 * 为什么用 SharedPreferences 而不是 DataStore？
 *  - 这里只有 1 个枚举值，SharedPreferences 的内存缓存 + 异步落盘完全够用，
 *    且**不需要新增任何依赖**（DataStore 要加 androidx.datastore:datastore-preferences）；
 *  - 用 [Flow] 暴露，未来换成 DataStore 只需替换实现类，UI 层零改动。
 *  - 若以后偏好项变多（>10 个、或需要类型安全的复杂结构），再迁移到 DataStore 即可。
 */
interface SettingsRepository {

    /** 当前主题模式（冷启动即有值，不会闪一下再切） */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
