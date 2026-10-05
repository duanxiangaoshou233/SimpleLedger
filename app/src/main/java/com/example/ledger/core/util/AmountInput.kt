package com.example.ledger.core.util

/**
 * 自定义数字键盘的输入状态机。
 *
 * 为什么把"输入过程"抽成一个纯 Kotlin 状态机，而不是写在 Composable 里？
 *  - 计算器式输入有一堆边界（前导零、多个小数点、超两位小数、退格到空、
 *    载入已有金额后第一次按键应"覆盖"而不是"追加"），这些全是逻辑，不是画界面；
 *  - 纯函数 -> 可以直接单元测试（见 test/AmountInputTest），不用跑模拟器；
 *  - Composable 只负责把 [displayText] 画出来 + 把按键转发进来。
 *
 * 不变式：`raw` 永远是"已经清洗过的合法金额字符串"（可能是空串或 "12." 这种中间态）。
 */
data class AmountInputState(
    /** 原始输入文本，如 ""、"0"、"12."、"1234.56" */
    val raw: String = "",

    /**
     * 下一次数字输入是否覆盖当前值。
     * 场景：编辑已有账单时载入 "38.50"，用户直接按 "5" 应该是 "5" 而不是 "38.505"。
     */
    val replaceOnNextInput: Boolean = false,
) {
    /** 对应的"分"（非法输入 = 0） */
    val cents: Long get() = MoneyFormatter.toCents(raw)

    val isEmpty: Boolean get() = raw.isEmpty()

    val isZero: Boolean get() = cents == 0L

    /** 大字展示用："0" / "12." / "1234.56" */
    val displayText: String get() = raw.ifEmpty { "0" }

    /** 带货币符号展示："¥12.34" */
    val displayWithSymbol: String get() = "¥$displayText"

    /** 按下数字键 */
    fun digit(digit: Char): AmountInputState {
        if (digit !in '0'..'9') return this
        if (replaceOnNextInput) {
            return AmountInputState(raw = MoneyFormatter.sanitizeAmountInput(digit.toString()))
        }
        return AmountInputState(raw = MoneyFormatter.sanitizeAmountInput(raw + digit))
    }

    /** 按下小数点：已有小数点则忽略；为空则自动补 "0." */
    fun decimal(): AmountInputState {
        if (replaceOnNextInput) return AmountInputState(raw = "0.")
        if (raw.contains('.')) return this
        if (raw.isEmpty()) return AmountInputState(raw = "0.")
        return AmountInputState(raw = MoneyFormatter.sanitizeAmountInput("$raw."))
    }

    /** 退格。注意 "0" 退格后是空串（展示上仍然是 "0"） */
    fun backspace(): AmountInputState {
        if (replaceOnNextInput) return AmountInputState()
        if (raw.isEmpty()) return this
        return AmountInputState(raw = raw.dropLast(1))
    }

    /** 清空（长按退格 / C 键） */
    fun clear(): AmountInputState = AmountInputState()

    companion object {
        /** 从"分"构造（编辑已有账单时用），并标记"下次输入覆盖" */
        fun fromCents(cents: Long): AmountInputState = AmountInputState(
            raw = MoneyFormatter.formatPlain(cents),
            replaceOnNextInput = true,
        )
    }
}
