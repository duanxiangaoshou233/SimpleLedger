package com.example.ledger.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * 金额格式化工具。
 *
 * 核心原则：
 *  1) 存储与计算全程用 `Long`（单位"分"），**唯一允许出现 Double 的地方是本文件的展示格式化**；
 *  2) 解析字符串一律走 [BigDecimal]，绝不 `"1.1".toDouble() * 100`（那会得到 110.00000000000001）；
 *  3) `DecimalFormat` 不是线程安全的，这里用 ThreadLocal 持有实例，避免在列表滑动时反复 new。
 */
object MoneyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.CHINA).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    /** 带千分位："1,234.56" */
    private val groupedFormat = ThreadLocal.withInitial { DecimalFormat("#,##0.00", symbols) }

    /** 不带千分位："1234.56" */
    private val plainFormat = ThreadLocal.withInitial { DecimalFormat("0.00", symbols) }

    /** 整数金额不带小数："1,234"（当分为 00 时更清爽） */
    private val groupedIntFormat = ThreadLocal.withInitial { DecimalFormat("#,##0", symbols) }

    // ------------------------------------------------------------------ 格式化

    /** 1234 分 -> "12.34" */
    fun format(cents: Long): String =
        groupedFormat.get()!!.format(BigDecimal.valueOf(cents, 2))

    /** 1234 分 -> "12.34"（无千分位，CSV 导出用，避免 Excel 把逗号当分隔符） */
    fun formatPlain(cents: Long): String =
        plainFormat.get()!!.format(BigDecimal.valueOf(cents, 2))

    /** 带货币符号：¥12.34 */
    fun formatWithSymbol(cents: Long, symbol: String = "¥"): String = "$symbol${format(cents)}"

    /** 整数金额优先，小数不为 0 时保留两位：123400 分 -> "1,234"；123456 -> "1,234.56" */
    fun formatSmart(cents: Long): String =
        if (cents % 100L == 0L) groupedIntFormat.get()!!.format(BigDecimal.valueOf(cents, 2))
        else format(cents)

    /**
     * 图表坐标轴用的紧凑格式（**纯整数运算**，不用浮点）：
     *   0        -> "0"
     *   123456   -> "1,234"
     *   1234567890 分（1234.5 万元） -> "1234.5万"
     *   >= 1 亿元 -> "1.2亿"
     */
    fun formatCompact(cents: Long): String {
        val negative = cents < 0
        val absCents = abs(cents)
        val text = when {
            absCents >= 100_000_000_00L -> { // >= 1 亿元（1 亿元 = 100_000_000_00 分）
                val times10 = (absCents + 500_000_000L) / 1_000_000_000L // 亿 * 10，四舍五入
                val body = trimTrailingZero(times10 / 10L, times10 % 10L) + "亿"
                body
            }
            absCents >= 1_000_000L -> { // >= 1 万元
                val times10 = (absCents + 50_000L) / 100_000L // 万 * 10，四舍五入
                trimTrailingZero(times10 / 10L, times10 % 10L) + "万"
            }
            else -> groupedIntFormat.get()!!.format(BigDecimal.valueOf(absCents, 2))
        }
        return if (negative) "-$text" else text
    }

    private fun trimTrailingZero(intPart: Long, decimalPart: Long): String =
        if (decimalPart == 0L) "$intPart" else "$intPart.$decimalPart"

    // ------------------------------------------------------------------ 解析

    /**
     * 字符串 -> 分。允许 "12"、"12.3"、"12.34"，超出两位小数按四舍五入。
     * 非法输入返回 0，绝不抛异常（自定义键盘的中间态可能是不完整的字符串）。
     *
     * 注意：会先把结尾的小数点去掉（"12." -> "12"），
     * 因为键盘上用户可能正好停在"刚按下小数点"的那一帧。
     */
    fun toCents(input: String): Long {
        var cleaned = input.trim()
        if (cleaned.endsWith(".")) cleaned = cleaned.dropLast(1)
        if (cleaned.isEmpty()) return 0L
        return try {
            BigDecimal(cleaned)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /**
     * 自定义数字键盘的输入清洗：
     *  - 只保留数字与小数点，且只允许一个小数点；
     *  - 小数最多两位（金额的最小单位就是分）；
     *  - 去掉整数字段多余的前导零（"007" -> "7"，但保留 "0.5" 的 0）；
     *  - 限制整数位长度，避免 Long 溢出（最多 12 位整数 ≈ 9999 亿元，够用了）。
     */
    fun sanitizeAmountInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' }
        if (filtered.isEmpty()) return ""

        val firstDot = filtered.indexOf('.')
        val intPartRaw = if (firstDot >= 0) filtered.substring(0, firstDot) else filtered
        val decimalPartRaw = if (firstDot >= 0) {
            filtered.substring(firstDot + 1).filter { it.isDigit() }.take(2)
        } else {
            null
        }

        val intPart = intPartRaw.trimStart('0').take(12).ifEmpty { "0" }

        return when {
            decimalPartRaw == null -> intPart
            decimalPartRaw.isEmpty() -> "$intPart."
            else -> "$intPart.$decimalPartRaw"
        }
    }

    /** 自由文本输入（备注里的金额、导入数据）宽松解析，失败返回 null */
    fun parseOrNull(input: String): Long? {
        val cleaned = input.trim().removePrefix("¥").removePrefix("￥").replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
