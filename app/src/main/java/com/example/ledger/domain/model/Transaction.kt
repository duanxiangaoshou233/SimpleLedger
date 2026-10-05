package com.example.ledger.domain.model

/**
 * 一笔交易（领域模型）。
 *
 * @param amountInCents 金额，单位"分"，**恒为正数**（Long 避免浮点误差）
 * @param dateTime      记账时间（用户选择的那个时间点），epoch millis，本地时区解释
 * @param tagId         关联标签；null = 无标签（不删除账单，只解除关联）
 * @param note          备注，可为空字符串
 * @param createdAt     记录创建时间，用于同秒排序兜底
 */
data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amountInCents: Long,
    val dateTime: Long,
    val tagId: Long?,
    val note: String = "",
    val createdAt: Long = 0L,
) {
    val isPersisted: Boolean get() = id > 0L

    /** 带符号的金额，仅用于展示（如 "-38.50"） */
    val signedAmountInCents: Long
        get() = if (type.isExpense) -amountInCents else amountInCents
}

/**
 * 列表筛选条件。
 *
 * 四个维度都可以独立为空：null 表示"不限制"。
 * 对应 DAO 里那条 `:param IS NULL OR column = :param` 的统一查询，
 * 避免为每种筛选组合写一条 SQL（8 种组合 → 1 条 SQL）。
 *
 * @param keyword 备注模糊搜索；调用前记得 trim，空串会被归一化成 null
 * @param range   时间区间（默认不限）
 */
data class TransactionFilter(
    val type: TransactionType? = null,
    val tagId: Long? = null,
    val keyword: String? = null,
    val range: TimeRange = TimeRange.All,
) {
    /** 是否处于"有筛选"状态 —— 记录列表页据此显示"已筛选"提示与一键清除按钮 */
    val isActive: Boolean
        get() = type != null || tagId != null || !keyword.isNullOrBlank() || range != TimeRange.All

    companion object {
        val Empty = TransactionFilter()
    }
}
