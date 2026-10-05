package com.example.ledger.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledger.BuildConfig
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.data.export.CsvExporter
import com.example.ledger.data.export.ExportResult
import com.example.ledger.data.settings.SettingsRepository
import com.example.ledger.domain.model.ThemeMode
import com.example.ledger.domain.model.TransactionFilter
import com.example.ledger.domain.repository.TransactionRepository
import com.example.ledger.ui.common.RangePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel。
 *
 * 主题切换：这里只管把选择写进 SettingsRepository，不持有"当前主题"；
 * 根 Composable 订阅同一个 Flow 决定配色 —— 全链路无回调、无事件总线。
 *
 * 导出：状态（范围/条数/进行中）在这里，**写文件在 `exportTo(uri)`**，
 * 由 UI 层的 SAF launcher 拿到 Uri 后回调。ViewModel 直接接收 Uri
 * （ViewModel 属于 UI 层，依赖 android.net.Uri 是可以接受的；
 * 真正的"写文件"细节被隔离在 data 层的 CsvExporter 里）。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val csvExporter: CsvExporter,
) : ViewModel() {

    private val exportPreset = MutableStateFlow(RangePreset.ALL)
    private val panels = MutableStateFlow(Panels())

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** 导出范围内的记录条数（实时跟随范围变化） */
    private val rowCountFlow: Flow<Int> = exportPreset.flatMapLatest { preset ->
        transactionRepository.observeCount(preset.toTimeRange())
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        exportPreset,
        panels,
        rowCountFlow,
    ) { themeMode, preset, panelState, rowCount ->
        SettingsUiState(
            themeMode = themeMode,
            exportPreset = preset,
            exportRangeLabel = DateTimeUtils.formatRangeLabel(preset.toTimeRange()),
            exportRowCount = rowCount,
            isExportDialogOpen = panelState.exportDialog,
            isExporting = panelState.exporting,
            isAboutDialogOpen = panelState.about,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(appVersion = BuildConfig.VERSION_NAME),
    )

    /* ============================================================ 主题 */

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /* ============================================================ 导出 */

    fun onExportDialogOpenChange(open: Boolean) {
        panels.value = panels.value.copy(exportDialog = open)
    }

    fun onExportPresetChange(preset: RangePreset) {
        // 设置页只提供"全部/今天/本周/本月/今年"，自定义区间留给记录列表与统计页
        exportPreset.value = if (preset == RangePreset.CUSTOM) RangePreset.ALL else preset
    }

    /** 默认文件名（交给系统文件选择器做初值） */
    fun suggestedFileName(): String = csvExporter.suggestedFileName()

    /** 当前导出范围（避免 UI 再把状态拼一遍） */
    fun currentExportRangeLabel(): String =
        DateTimeUtils.formatRangeLabel(exportPreset.value.toTimeRange())

    /** SAF 返回 Uri 后调用；期间会在页面上显示"正在导出…" */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            panels.value = panels.value.copy(exporting = true, exportDialog = false)

            val filter = TransactionFilter(range = exportPreset.value.toTimeRange())
            when (val result = csvExporter.exportTo(uri, filter)) {
                is ExportResult.Success ->
                    _effects.send(SettingsEffect.ShowMessage("已导出 ${result.rowCount} 条记录"))

                is ExportResult.Failure ->
                    _effects.send(SettingsEffect.ShowMessage("导出失败：${result.message}"))
            }

            panels.value = panels.value.copy(exporting = false)
        }
    }

    /** 用户在系统文件选择器里按了返回：复位状态，否则按钮会一直转圈 */
    fun onExportCancelled() {
        panels.value = panels.value.copy(exporting = false, exportDialog = false)
    }

    /* ============================================================ 关于 */

    fun onAboutDialogOpenChange(open: Boolean) {
        panels.value = panels.value.copy(about = open)
    }

    private data class Panels(
        val exportDialog: Boolean = false,
        val exporting: Boolean = false,
        val about: Boolean = false,
    )
}
