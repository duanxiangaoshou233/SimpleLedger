package com.example.ledger.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ledger.domain.model.TagType

/**
 * 标签表。
 *
 * 说明：
 *  - `id = 0L` 是"未入库"的哨兵值，配合 autoGenerate 使用；
 *  - 这里**故意不加 unique 索引**（name+type）。重名检查放在 ViewModel 里做，
 *    这样用户看到的是"已有同名标签"的友好提示，而不是 SQLiteConstraintException 崩溃；
 *    `name + type` 只建普通索引，用于查重与排序；
 *  - 颜色由用户选择或从设计系统的图表色板里分配，存 "#RRGGBB" 字符串。
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name", "type"]),
        Index(value = ["type"]),
    ],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "colorHex")
    val colorHex: String,

    @ColumnInfo(name = "type")
    val type: TagType,
)
