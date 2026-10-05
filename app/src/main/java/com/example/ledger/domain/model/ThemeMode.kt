package com.example.ledger.domain.model

/**
 * 主题模式（业务偏好，不是纯 UI 概念）。
 *
 * 放在 domain 层的原因：
 *  - `SettingsRepository`（数据层）要持久化它，数据层不允许 import `ui.theme`；
 *  - UI 层（`LedgerTheme`）只是它的消费者。
 *
 * 三态：跟随系统 / 强制浅色 / 强制深色。
 */
enum class ThemeMode(val label: String) {
    /** 跟随系统（默认） */
    SYSTEM("跟随系统"),

    /** 始终浅色 */
    LIGHT("浅色"),

    /** 始终深色 */
    DARK("深色"),
    ;

    companion object {
        fun fromNameOrNull(name: String?): ThemeMode? = entries.firstOrNull { it.name == name }
    }
}
