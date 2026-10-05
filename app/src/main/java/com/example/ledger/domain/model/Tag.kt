package com.example.ledger.domain.model

/**
 * 标签（领域模型）。
 *
 * @param id       自增主键；0 表示"尚未入库"
 * @param name     标签名，同一 [TagType] 下不允许重名（由 ViewModel 校验后给出友好提示）
 * @param colorHex 颜色，形如 "#2E9E8F"；用于列表色点、Chip 选中态、图表分类色
 * @param type     适用范围（支出/收入/通用）
 */
data class Tag(
    val id: Long = 0L,
    val name: String,
    val colorHex: String,
    val type: TagType,
) {
    /** 是否已入库 */
    val isPersisted: Boolean get() = id > 0L
}

/** 标签 + 使用次数，用于标签管理页显示"已被 N 笔账单使用" */
data class TagWithUsage(
    val tag: Tag,
    val usageCount: Int,
)
