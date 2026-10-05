package com.example.ledger.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.ui.theme.AppTheme

/**
 * 金额展示组件。
 *
 * 为什么用 AnnotatedString 把 "¥" 画小一号，而不是两个 Text 并排？
 *  - 两个 Text 需要 `alignByBaseline()` 才能对齐，一旦外面套了 Row/Column 就容易错位；
 *  - 单 Text 天然共享基线，且只测量一次，滚动更省。
 *
 * @param signed 是否带 +/- 号（列表里支出显示红色 "-"，收入显示绿色 "+"）
 */
@Composable
fun AmountText(
    amountInCents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.amount.Medium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showSymbol: Boolean = true,
    sign: String = "",
) {
    val text = buildAnnotatedString {
        if (showSymbol) {
            withStyle(
                SpanStyle(
                    fontSize = style.fontSize * 0.62f,
                    fontWeight = FontWeight.Medium,
                ),
            ) {
                append("¥")
            }
        }
        if (sign.isNotEmpty()) append(sign)
        append(MoneyFormatter.format(amountInCents))
    }

    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

/**
 * "¥ 12.34" 这种带间隔的横排金额（用于需要符号与数字分开排版的场景，如 Hero 卡）。
 */
@Composable
fun AmountWithSymbolRow(
    amountInCents: Long,
    modifier: Modifier = Modifier,
    amountStyle: TextStyle = AppTheme.amount.Hero,
    symbolStyle: TextStyle = AppTheme.amount.Large,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(text = "¥", style = symbolStyle, color = color)
        Spacer(Modifier.width(4.dp))
        Text(
            text = MoneyFormatter.format(amountInCents),
            style = amountStyle,
            color = color,
            maxLines = 1,
        )
    }
}
