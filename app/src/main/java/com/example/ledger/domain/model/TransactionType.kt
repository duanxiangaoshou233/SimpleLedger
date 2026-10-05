package com.example.ledger.domain.model

/**
 * 交易类型。
 *
 * 设计约定（非常重要，全工程统一遵守）：
 *  - **金额永远存正数**（Long，单位"分"），正负号只由 type 推导，
 *    这样 SQL 聚合不用处理符号，统计逻辑不会因为忘记取绝对值而算错。
 *  - 数据库中持久化为枚举名字符串（"EXPENSE" / "INCOME"），
 *    方便以后加类型时做数据迁移，也比存序号可读。
 */
enum class TransactionType(val label: String, val sign: String) {
    /** 支出 */
    EXPENSE("支出", "-"),

    /** 收入 */
    INCOME("收入", "+"),
    ;

    val isExpense: Boolean get() = this == EXPENSE
    val isIncome: Boolean get() = this == INCOME

    companion object {
        /** 从数据库字符串安全还原（脏数据不崩溃） */
        fun fromNameOrNull(name: String?): TransactionType? =
            entries.firstOrNull { it.name == name }
    }
}
