package com.example.ledger.data.export

import com.example.ledger.domain.model.Transaction
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.model.TransactionWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * CSV 拼装的单元测试（纯 JVM，不需要 Android）。
 *
 * 重点验证三件"最容易在真机上才暴露"的事：
 *  1) BOM 存在（否则 Excel 中文乱码）；
 *  2) 含逗号/引号/换行的备注被正确转义；
 *  3) 金额带符号（支出为负）。
 */
class CsvWriterTest {

    private fun row(
        type: TransactionType,
        cents: Long,
        tag: String?,
        note: String,
    ): TransactionWithTag {
        val dateTime = LocalDateTime.of(2024, 6, 1, 14, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return TransactionWithTag(
            transaction = Transaction(
                id = 1L,
                type = type,
                amountInCents = cents,
                dateTime = dateTime,
                tagId = 1L,
                note = note,
                createdAt = dateTime,
            ),
            tagName = tag,
            tagColorHex = "#2E9E8F",
        )
    }

    @Test
    fun `文件以 BOM 开头且包含表头`() {
        val csv = CsvWriter.buildCsv(listOf(row(TransactionType.EXPENSE, 3850L, "餐饮", "午饭")))
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("日期,时间,类型,金额,标签,备注"))
    }

    @Test
    fun `支出输出负数收入输出正数`() {
        val csv = CsvWriter.buildCsv(
            listOf(
                row(TransactionType.EXPENSE, 3850L, "餐饮", ""),
                row(TransactionType.INCOME, 820_000L, "工资", ""),
            ),
        )
        assertTrue(csv.contains("-38.50"))
        assertTrue(csv.contains("8200.00"))
    }

    @Test
    fun `含逗号引号换行的备注被转义`() {
        assertEquals("\"a,b\"", CsvWriter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvWriter.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvWriter.escape("line1\nline2"))
        assertEquals("普通备注", CsvWriter.escape("普通备注"))
    }

    @Test
    fun `无标签时输出无标签占位`() {
        val csv = CsvWriter.buildCsv(listOf(row(TransactionType.EXPENSE, 100L, null, "")))
        assertTrue(csv.contains("无标签"))
    }

    @Test
    fun `空列表也能生成合法文件`() {
        val csv = CsvWriter.buildCsv(emptyList())
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("日期,时间,类型,金额,标签,备注"))
    }
}
