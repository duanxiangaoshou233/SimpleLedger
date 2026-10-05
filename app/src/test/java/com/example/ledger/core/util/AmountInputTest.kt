package com.example.ledger.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 自定义数字键盘状态机的单元测试（纯 JVM，无需模拟器）。
 *
 * 这些用例覆盖了"计算器式输入"最容易出错的边界：
 * 前导零、重复小数点、超过两位小数、退格到空、载入已有金额后的首次输入。
 */
class AmountInputTest {

    private fun AmountInputState.type(vararg keys: String): AmountInputState {
        var state = this
        keys.forEach { key ->
            state = when (key) {
                "." -> state.decimal()
                "⌫" -> state.backspace()
                else -> key.forEach { digit -> state = state.digit(digit) }
            }
        }
        return state
    }

    @Test
    fun `空状态显示为 0 且金额为 0`() {
        val state = AmountInputState()
        assertTrue(state.isEmpty)
        assertEquals("0", state.displayText)
        assertEquals(0L, state.cents)
    }

    @Test
    fun `连续输入数字`() {
        val state = AmountInputState().type("1", "2", "3")
        assertEquals("123", state.raw)
        assertEquals(12_300L, state.cents)
    }

    @Test
    fun `前导零会被清理`() {
        assertEquals("5", AmountInputState().type("0", "0", "5").raw)
        assertEquals("0", AmountInputState().type("0").raw)
    }

    @Test
    fun `只能有一个小数点且自动补前导零`() {
        val state = AmountInputState().type(".", "5")
        assertEquals("0.5", state.raw)
        assertEquals(50L, state.cents)

        val twoDots = AmountInputState().type("1", ".", ".", "5")
        assertEquals("1.5", twoDots.raw)
    }

    @Test
    fun `小数点后最多两位`() {
        val state = AmountInputState().type("1", ".", "2", "3", "9")
        assertEquals("1.23", state.raw)
        assertEquals(123L, state.cents)
    }

    @Test
    fun `以小数点结尾也是合法中间态`() {
        val state = AmountInputState().type("1", "2", ".")
        assertEquals("12.", state.raw)
        assertEquals(1_200L, state.cents)
    }

    @Test
    fun `退格与清空`() {
        val state = AmountInputState().type("1", "2", "3", "⌫")
        assertEquals("12", state.raw)

        val emptied = AmountInputState().type("1", "⌫")
        assertEquals("", emptied.raw)
        assertTrue(emptied.isEmpty)

        assertEquals("", AmountInputState().type("9", "9").clear().raw)
    }

    @Test
    fun `载入已有金额后的第一次输入是覆盖而不是追加`() {
        val loaded = AmountInputState.fromCents(3_850L)
        assertEquals("38.50", loaded.raw)
        assertTrue(loaded.replaceOnNextInput)

        val replaced = loaded.digit('5')
        assertEquals("5", replaced.raw)
        assertFalse(replaced.replaceOnNextInput)
    }

    @Test
    fun `载入后按小数点等于从零开始`() {
        val loaded = AmountInputState.fromCents(3_850L).decimal()
        assertEquals("0.", loaded.raw)
    }

    @Test
    fun `整数位长度受限不会溢出`() {
        val state = AmountInputState().type("9".repeat(20))
        assertEquals(12, state.raw.length)
        assertTrue(state.cents > 0L)
    }
}
