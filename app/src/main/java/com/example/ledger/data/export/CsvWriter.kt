package com.example.ledger.data.export

import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.TransactionWithTag

/**
 * CSV 拼装（纯 Kotlin，无 Android 依赖 —— 所以可以直接跑单元测试）。
 *
 * 字段顺序：日期,时间,类型,金额,标签,备注
 *
 * 三个容易踩的坑，这里都处理了：
 *  1) **BOM**：文件开头写 U+FEFF，Excel 才会按 UTF-8 解码，中文才不乱码；
 *  2) **转义**：备注里出现逗号/引号/换行时用双引号包裹，内部引号翻倍（RFC 4180）；
 *  3) **换行符**：用 CRLF，Windows 版 Excel 对 LF 的兼容性不总是可靠。
 */
object CsvWriter {

    /** 文件头，Excel 靠它识别 UTF-8 */
    const val BOM: String = "\uFEFF"

    private const val LINE_SEPARATOR = "\r\n"

    private val HEADER = listOf("日期", "时间", "类型", "金额", "标签", "备注")

    /** 生成完整 CSV 文本（含 BOM，可直接 toByteArray(UTF_8) 落盘） */
    fun buildCsv(rows: List<TransactionWithTag>): String = buildString {
        append(BOM)
        append(HEADER.joinToString(",") { escape(it) })
        append(LINE_SEPARATOR)

        rows.forEach { row ->
            val transaction = row.transaction
            append(
                listOf(
                    DateTimeUtils.formatCsvDate(transaction.dateTime),
                    DateTimeUtils.formatCsvTime(transaction.dateTime),
                    transaction.type.label,
                    // 带符号：支出为负，Excel 里可直接求和
                    MoneyFormatter.formatPlain(transaction.signedAmountInCents),
                    row.displayTagName,
                    transaction.note,
                ).joinToString(",") { escape(it) },
            )
            append(LINE_SEPARATOR)
        }
    }

    /**
     * RFC 4180 转义：只在必要时加引号，避免把整个文件撑成一行行引号。
     * 必须判断 `\r`，否则含换行的备注会破坏表格结构。
     */
    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
