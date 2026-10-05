package com.example.ledger.data.local.dao

import androidx.room.Embedded
import com.example.ledger.data.local.entity.TagEntity
import com.example.ledger.data.local.entity.TransactionEntity

/**
 * DAO 的查询投影（Projection）类：只承载"读"出来的一行数据，不参与写入。
 */

/** 按标签汇总的一行：标签信息 + 金额 + 笔数 */
data class TagStatRow(
    /** null 表示这些账单没有标签 */
    val tagId: Long?,
    val tagName: String?,
    val colorHex: String?,
    val total: Long,
    val itemCount: Int,
)

/** 趋势图需要的轻量投影：只要"时间 + 金额"，不把整行读进内存 */
data class AmountAtTime(
    val dateTime: Long,
    val amountInCents: Long,
)

/** 标签 + 被引用次数（标签管理页） */
data class TagUsageRow(
    @Embedded val tag: TagEntity,
    val usageCount: Int,
)

/** 账单 + 标签名（CSV 导出、记录详情页需要显示标签名称与颜色） */
data class TransactionExportRow(
    @Embedded val transaction: TransactionEntity,
    val tagName: String?,
    val tagColorHex: String?,
)
