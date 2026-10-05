package com.example.ledger.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ledger.ui.theme.AppShapes
import com.example.ledger.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 日期选择对话框（Material 3 DatePicker）。
 *
 * ⚠️ 关键坑：`DatePickerState.selectedDateMillis` 是**UTC 零点**的毫秒，
 * 而 App 内其它地方全部用"本地时区"的 epoch millis。
 * 直接混用会差 8 小时（东八区）导致选 6/1 变成 5/31。
 * 因此这里有两个显式转换函数，所有调用方都必须走它们。
 */
@Composable
fun LedgerDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = state.selectedDateMillis?.toLocalDateFromUtc()
                    if (selected != null) onConfirm(selected) else onDismiss()
                },
            ) {
                Text("确定", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
            title = {
                Text(
                    text = "选择日期",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xl, top = Spacing.lg),
                )
            },
        )
    }
}

/**
 * 时间选择对话框。
 *
 * 为什么不用 AlertDialog 包 TimePicker？
 *  - TimePicker 的表盘直径约 300dp，AlertDialog 的最小/最大宽度约束会把它挤变形；
 *  - 自己用 Dialog + Surface 可以精确控制内边距与圆角，与其它弹窗风格一致。
 */
@Composable
fun LedgerTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.sheet,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.lg))
                TimePicker(state = state)
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "取消",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

/* ============================================================ UTC <-> LocalDate */

/** 本地日期 -> DatePicker 需要的"UTC 零点毫秒" */
internal fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** DatePicker 返回的"UTC 零点毫秒" -> 本地日期 */
internal fun Long.toLocalDateFromUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
