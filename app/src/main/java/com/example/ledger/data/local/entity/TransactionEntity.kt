package com.example.ledger.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ledger.domain.model.TransactionType

/**
 * 交易（账单）表。
 *
 * 关键设计：
 *  1) **amountInCents 恒为正数**，单位"分"（Long）。正负号由 type 表达，
 *     聚合时不用 ABS()，也不会因为漏取绝对值把"支出排行"算成负数。
 *  2) dateTime 存 epoch millis（UTC 毫秒），但语义上是"用户在本地时区选择的那个时刻"，
 *     展示与分组都按设备当前时区解释 —— 既避免夏令时/换时区导致的数据错位，
 *     又能直接参与 SQL 比较与索引。
 *  3) 外键指向 tags，onDelete = SET_NULL：即便某天代码里直接删了标签，
 *     账单也不会被级联删除，只会变成"无标签"（数据安全兜底）。
 *     正常流程里用户会先选择"转移账单"或"设为无标签"，走 Repository 的显式逻辑。
 *  4) 三个索引分别服务：列表倒序分页（dateTime）、按标签筛选（tagId）、按收支筛选（type）。
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dateTime"]),
        Index(value = ["tagId"]),
        Index(value = ["type"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    /** 金额，单位"分"，恒为正 */
    @ColumnInfo(name = "amountInCents")
    val amountInCents: Long,

    /** 记账时间，epoch millis */
    @ColumnInfo(name = "dateTime")
    val dateTime: Long,

    /** 关联标签 id；null = 无标签 */
    @ColumnInfo(name = "tagId")
    val tagId: Long?,

    /** 备注 */
    @ColumnInfo(name = "note")
    val note: String = "",

    /** 创建时间，epoch millis（同分钟内的稳定排序兜底） */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
)
