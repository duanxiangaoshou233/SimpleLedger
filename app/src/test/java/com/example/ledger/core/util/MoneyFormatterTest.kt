package com.example.ledger.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 金额格式化的单元测试。
 *
 * 重点验证"分 <-> 元"的转换永远不会引入浮点误差：
 * 0.1 + 0.2 != 0.3 这类经典问题在本 App 里必须不可能出现。
 */
class MoneyFormatterTest {

    @Test
    fun `分转元的展示格式`() {
        assertEquals("0.00", MoneyFormatter.format(0L))
        assertEquals("0.05", MoneyFormatter.format(5L))
        assertEquals("12.34", MoneyFormatter.format(1_234L))
        assertEquals("1,234.56", MoneyFormatter.format(123_456L))
        assertEquals("12,345,678.90", MoneyFormatter.format(1_234_567_890L))
    }

    @Test
    fun `CSV 用无千分位格式`() {
        assertEquals("1234.56", MoneyFormatter.formatPlain(123_456L))
    }

    @Test
    fun `整数金额的智能格式`() {
        assertEquals("1,234", MoneyFormatter.formatSmart(123_400L))
        assertEquals("1,234.56", MoneyFormatter.formatSmart(123_456L))
    }

    @Test
    fun `字符串解析为分`() {
        assertEquals(1_234L, MoneyFormatter.toCents("12.34"))
        assertEquals(1_200L, MoneyFormatter.toCents("12"))
        assertEquals(1_230L, MoneyFormatter.toCents("12.3"))
        assertEquals(1_200L, MoneyFormatter.toCents("12."))
        assertEquals(0L, MoneyFormatter.toCents(""))
        assertEquals(0L, MoneyFormatter.toCents("."))
        assertEquals(0L, MoneyFormatter.toCents("abc"))
    }

    @Test
    fun `超出两位小数按四舍五入`() {
        assertEquals(1_235L, MoneyFormatter.toCents("12.345"))
        assertEquals(1_234L, MoneyFormatter.toCents("12.344"))
    }

    @Test
    fun `浮点误差不会出现`() {
        // 0.1 元 + 0.2 元 必须是精确的 0.30
        val sum = MoneyFormatter.toCents("0.1") + MoneyFormatter.toCents("0.2")
        assertEquals(30L, sum)
        assertEquals("0.30", MoneyFormatter.formatPlain(sum))
    }

    @Test
    fun `宽松解析允许货币符号与千分位`() {
        assertEquals(123_456L, MoneyFormatter.parseOrNull("¥1,234.56"))
        assertNull(MoneyFormatter.parseOrNull("不是一个数"))
    }

    @Test
    fun `坐标轴紧凑格式`() {
        assertEquals("0", MoneyFormatter.formatCompact(0L))
        // 1234.56 元 -> 不足 1 万，按整数元显示
        assertEquals("1,235", MoneyFormatter.formatCompact(123_456L))
        // 123,000 元 -> 12.3 万元
        assertEquals("12.3万", MoneyFormatter.formatCompact(12_300_000L))
        // 1.2 亿元
        assertEquals("1.2亿", MoneyFormatter.formatCompact(12_000_000_000L))
    }

    @Test
    fun `键盘输入清洗`() {
        assertEquals("7", MoneyFormatter.sanitizeAmountInput("007"))
        assertEquals("0.5", MoneyFormatter.sanitizeAmountInput("00.5"))
        assertEquals("12.99", MoneyFormatter.sanitizeAmountInput("12.999"))
        assertEquals("12.", MoneyFormatter.sanitizeAmountInput("12."))
        // 完全没有数字与小数点时返回空串，由键盘决定是否显示 0
        assertEquals("", MoneyFormatter.sanitizeAmountInput("abc"))
    }
}
