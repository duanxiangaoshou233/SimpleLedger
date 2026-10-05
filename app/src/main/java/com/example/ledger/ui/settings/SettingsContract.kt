package com.example.ledger.ui.settings

import androidx.compose.runtime.Immutable
import com.example.ledger.domain.model.ThemeMode
import com.example.ledger.ui.common.RangePreset

/**
 * 设置页 UI 状态。
 *
 * 导出部分：
 *  - [exportPreset] 决定导出范围，[exportRowCount] 实时显示"将导出 N 条"；
 *  - [isExporting] 是写文件期间的状态（UI 禁用按钮并显示进度）；
 *  - 真正写文件由设置了 SAF 的 Activity 拿到 Uri 后回调 `SettingsViewModel.exportTo(uri)`。
 */
@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val exportPreset: RangePreset = RangePreset.ALL,
    val exportRangeLabel: String = "全部时间",
    val exportRowCount: Int = 0,
    val isExportDialogOpen: Boolean = false,
    val isExporting: Boolean = false,
    val isAboutDialogOpen: Boolean = false,
    val appVersion: String = "",
) {
    /** 导出按钮文案："导出 128 条记录" */
    val exportButtonLabel: String
        get() = if (exportRowCount > 0) "导出 $exportRowCount 条记录" else "暂无可导出的记录"

    /** 没有数据或正在导出时禁用按钮（避免生成一个只有表头的空文件） */
    val canExport: Boolean get() = exportRowCount > 0 && !isExporting
}

/** 一次性事件 */
sealed interface SettingsEffect {
    data class ShowMessage(val message: String) : SettingsEffect
}
