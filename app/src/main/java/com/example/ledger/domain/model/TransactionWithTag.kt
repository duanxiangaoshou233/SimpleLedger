package com.example.ledger.domain.model

/**
 * 账单 + 标签展示信息（只读组合体）。
 *
 * 用途：
 *  - CSV 导出需要"标签名"这一列；
 *  - 记录列表/详情页需要标签颜色做色点。
 *
 * 直接用一条 LEFT JOIN 查出，避免"先查账单再逐条查标签"的 N+1 问题。
 */
data class TransactionWithTag(
    val transaction: Transaction,
    val tagName: String?,
    val tagColorHex: String?,
) {
    /** 无标签时给一个统一的灰调，保证 UI 不留空洞 */
    val displayTagName: String get() = tagName ?: TagAmount.NO_TAG_NAME
    val displayTagColorHex: String get() = tagColorHex ?: TagAmount.NO_TAG_COLOR_HEX
}
