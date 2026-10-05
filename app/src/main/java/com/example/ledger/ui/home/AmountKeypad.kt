package com.example.ledger.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.theme.AppTheme
import com.example.ledger.ui.theme.Radius
import com.example.ledger.ui.theme.Spacing

private val KeyHeight = 52.dp
private val KeyGap = 8.dp

/**
 * 自定义数字键盘（记账面板的核心交互）。
 *
 * 布局（经典记账 App 的"3 列数字 + 右侧长条保存"）：
 * ```
 *  1   2   3   ⌫
 *  4   5   6  ┌──┐
 *  7   8   9  │保存│
 *  .   0   C  └──┘
 * ```
 *
 * 为什么不用系统键盘？
 *  - 数字键盘不需要候选词栏，占地小一半；
 *  - 每个键都能做 56dp 大按钮 + 按压缩放反馈，手感远好于系统 IME；
 *  - 不会因为用户切了第三方输入法就出现"金额输成中文"的尴尬。
 *
 * 渲染性能说明：整块键盘是静态布局（无 LazyColumn），
 * 按键只回调事件、不持有状态，所以每次都走最快路径。
 */
@Composable
fun AmountKeypad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        // ---------------- 第一行：1 2 3 ⌫ ----------------
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            DigitKey("1", Modifier.weight(1f), onDigit)
            DigitKey("2", Modifier.weight(1f), onDigit)
            DigitKey("3", Modifier.weight(1f), onDigit)
            ActionKey(
                icon = Icons.Rounded.Backspace,
                label = "退格",
                onClick = onBackspace,
                modifier = Modifier.weight(1f),
            )
        }

        // ---------------- 后三行 + 右侧保存 ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(KeyHeight * 3 + KeyGap * 2),
            horizontalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(KeyGap),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    DigitKey("4", Modifier.weight(1f), onDigit)
                    DigitKey("5", Modifier.weight(1f), onDigit)
                    DigitKey("6", Modifier.weight(1f), onDigit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    DigitKey("7", Modifier.weight(1f), onDigit)
                    DigitKey("8", Modifier.weight(1f), onDigit)
                    DigitKey("9", Modifier.weight(1f), onDigit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    TextKey(
                        label = ".",
                        onClick = onDecimal,
                        modifier = Modifier.weight(1f),
                        contentDescription = "小数点",
                    )
                    DigitKey("0", Modifier.weight(1f), onDigit)
                    TextKey(
                        label = "C",
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        contentDescription = "清空",
                    )
                }
            }

            SaveKey(
                enabled = saveEnabled,
                isEditing = isEditing,
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/* ============================================================ 按键 */

@Composable
private fun DigitKey(
    digit: String,
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
) {
    KeySurface(
        onClick = { onDigit(digit.first()) },
        modifier = modifier,
        contentDescription = digit,
    ) {
        Text(
            text = digit,
            style = AppTheme.amount.Large,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TextKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    KeySurface(onClick = onClick, modifier = modifier, contentDescription = contentDescription) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionKey(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeySurface(onClick = onClick, modifier = modifier, contentDescription = label) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 按键底座：统一圆角、按下缩放、涟漪。
 * 把"手感"集中在一个地方定义，避免 12 个键各写一遍。
 */
@Composable
private fun KeySurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 1100f),
        label = "keyScale",
    )

    Surface(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale),
        shape = RoundedCornerShape(Radius.md),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClickLabel = contentDescription,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * 保存键：主色填充 + 竖向文字，占据三行高度。
 * 编辑态时文案变成"保存修改"，让用户确认自己不是在新增。
 */
@Composable
private fun SaveKey(
    enabled: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 1100f),
        label = "saveKeyScale",
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(Radius.md),
        color = if (enabled) scheme.primary else scheme.surfaceContainerHighest,
        contentColor = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isEditing) {
                    Text(
                        text = "保\n存\n修\n改",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    Text(
                        text = "保\n存",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}
