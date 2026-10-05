package com.example.ledger.domain.model

/**
 * 标签类型。
 *
 * [COMMON] 是"通用标签"，在记账时无论选收入还是支出都能看到（例如"报销"、"其他"）。
 * 支出标签只在记支出时出现，收入标签只在记收入时出现 —— 用 [matches] 统一判断。
 */
enum class TagType(val label: String) {
    /** 仅可用于支出 */
    EXPENSE("支出"),

    /** 仅可用于收入 */
    INCOME("收入"),

    /** 收支通用 */
    COMMON("通用"),
    ;

    /** 该标签是否适用于某笔交易类型 */
    fun matches(transactionType: TransactionType): Boolean = when (this) {
        COMMON -> true
        EXPENSE -> transactionType == TransactionType.EXPENSE
        INCOME -> transactionType == TransactionType.INCOME
    }

    companion object {
        fun fromNameOrNull(name: String?): TagType? = entries.firstOrNull { it.name == name }

        /** 记账页按收支筛选可用标签 */
        fun availableFor(transactionType: TransactionType): List<TagType> = when (transactionType) {
            TransactionType.EXPENSE -> listOf(EXPENSE, COMMON)
            TransactionType.INCOME -> listOf(INCOME, COMMON)
        }
    }
}
