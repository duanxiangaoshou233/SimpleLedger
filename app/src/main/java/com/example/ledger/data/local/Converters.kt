package com.example.ledger.data.local

import androidx.room.TypeConverter
import com.example.ledger.domain.model.TagType
import com.example.ledger.domain.model.TransactionType

/**
 * 枚举 <-> 字符串 的 TypeConverter。
 *
 * 为什么不存序号（ordinal）？
 *  - 枚举顺序一变，历史数据就全错位了；存名字则永远可读、可迁移。
 *
 * 容错：遇到无法识别的字符串不抛异常，退化成默认值，
 * 这样即便用户手动改过数据库文件，App 也不会一打开就崩。
 *
 * 注意：**只定义非空版本的转换器**。
 * 可空字段（如 TransactionEntity.tagId 这种 Long? 无需转换，
 * 而 TransactionType? 的查询参数）由 Room 生成的代码自动判空绑定 null；
 * 若同时定义可空与非空两份转换器，Room 会报 "Ambiguous @TypeConverter"。
 */
class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        TransactionType.fromNameOrNull(value) ?: TransactionType.EXPENSE

    @TypeConverter
    fun fromTagType(value: TagType): String = value.name

    @TypeConverter
    fun toTagType(value: String): TagType =
        TagType.fromNameOrNull(value) ?: TagType.COMMON
}
