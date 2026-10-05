package com.example.ledger.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledger.domain.model.ThemeMode
import com.example.ledger.ui.common.RangePreset
import com.example.ledger.ui.components.LedgerCard
import com.example.ledger.ui.components.LedgerDivider
import com.example.ledger.ui.components.LocalSnackbarHostState
import com.example.ledger.ui.components.SectionHeader
import com.example.ledger.ui.components.TagChip
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Spacing

/**
 * 设置页：外观（主题三态）/ 数据（CSV 导出）/ 关于。
 *
 * 主题切换是"即时生效"的：这里点一下 → 写入 SettingsRepository →
 * 根 Composable 订阅的同一个 Flow 立刻发射 → 整个 App 换配色，页面不会重建、不会闪。
 *
 * CSV 导出走 **SAF（系统文件选择器）**：
 *  - 零权限（不需要 WRITE_EXTERNAL_STORAGE / MANAGE_EXTERNAL_STORAGE）；
 *  - 用户在系统界面自己选保存位置，我们不接触任何路径字符串；
 *  - 取消（返回 null）时要把"导出中"复位，否则按钮会一直转圈。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportTo(uri)
        } else {
            viewModel.onExportCancelled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.sm,
                bottom = 170.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = Spacing.xs)) {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "完全离线 · 不联网 · 数据只在本机",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---------------- 外观 ----------------
            item(key = "appearance") {
                Column {
                    SectionHeader(title = "外观")
                    Spacer(Modifier.height(Spacing.sm))
                    LedgerCard {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = "主题",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "深色模式在夜间记账时不刺眼",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.md))
                            ThemeSelector(
                                selected = state.themeMode,
                                onSelect = viewModel::onThemeModeChange,
                            )
                        }
                    }
                }
            }

            // ---------------- 数据导出 ----------------
            item(key = "data") {
                Column {
                    SectionHeader(title = "数据")
                    Spacer(Modifier.height(Spacing.sm))
                    LedgerCard {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "导出 CSV 账单",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "UTF-8 BOM 编码，Excel 直接打开不乱码；金额带符号，可直接求和",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = AppShapes.dot,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(Spacing.md))

                            // 导出范围：全部 / 今天 / 本周 / 本月 / 今年
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                RangePreset.listPresets.forEach { preset ->
                                    TagChip(
                                        name = preset.label,
                                        colorHex = "#2E9E8F",
                                        selected = state.exportPreset == preset,
                                        onClick = { viewModel.onExportPresetChange(preset) },
                                    )
                                }
                            }

                            Spacer(Modifier.height(Spacing.md))

                            Text(
                                text = "范围：${state.exportRangeLabel} · 共 ${state.exportRowCount} 条",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(Spacing.md))

                            Button(
                                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                                enabled = state.canExport,
                                shape = AppShapes.field,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (state.isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text("正在导出…")
                                } else {
                                    Text(state.exportButtonLabel)
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- 关于 ----------------
            item(key = "about") {
                Column {
                    SectionHeader(title = "关于")
                    Spacer(Modifier.height(Spacing.sm))
                    LedgerCard {
                        Column {
                            SettingRow(
                                icon = Icons.Rounded.Info,
                                title = "关于简易记账",
                                subtitle = "版本 ${state.appVersion}",
                                onClick = { viewModel.onAboutDialogOpenChange(true) },
                            )
                        }
                    }
                }
            }

            item(key = "footer") {
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "简易记账 v${state.appVersion}\n" +
                        "所有账本数据保存在本机 SQLite 数据库，无网络请求、无账号、无广告。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = Spacing.xs),
                )
            }
        }
    }

    if (state.isAboutDialogOpen) {
        AboutDialog(
            version = state.appVersion,
            onDismiss = { viewModel.onAboutDialogOpenChange(false) },
        )
    }
}

/* ============================================================ 局部组件 */

/** 三选一的分段控件（跟随系统 / 浅色 / 深色），选中块带颜色动画与图标 */
@Composable
private fun ThemeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppShapes.field)
            .background(scheme.surfaceContainerHigh)
            .padding(Spacing.xs),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) scheme.primary else scheme.surfaceContainerHigh,
                animationSpec = tween(220),
                label = "themeSegment",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "themeSegmentContent",
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(AppShapes.field)
                    .background(container)
                    .clickable { onSelect(mode) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = mode.icon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor,
                )
            }
        }
    }
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Rounded.Contrast
    ThemeMode.LIGHT -> Icons.Rounded.LightMode
    ThemeMode.DARK -> Icons.Rounded.DarkMode
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, AppShapes.dot),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 关于对话框 */
@Composable
private fun AboutDialog(version: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.bottomSheet,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, AppShapes.dot),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "记",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                Text(text = "简易记账", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "版本 $version",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                LedgerDivider(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "一个纯粹的本地记账工具：\n" +
                        "无登录、无联网、无广告、无内购；\n" +
                        "金额以「分」为单位存储，不会出现浮点误差；\n" +
                        "支持 CSV 导出，数据永远属于你自己。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))
                TextButton(onClick = onDismiss) { Text("好的") }
            }
        }
    }
}
