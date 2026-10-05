package com.example.ledger.data.repository

import com.example.ledger.data.local.entity.TagEntity
import com.example.ledger.data.local.entity.TransactionEntity
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.Transaction

/**
 * Entity <-> Domain 映射。
 *
 * 为什么不让 Entity 直接被 UI 使用（省掉这层）？
 *  - Entity 是"数据库形状"（含外键、索引、自增哨兵值），一旦 UI 依赖它，
 *    以后改表结构就会牵动所有页面；
 *  - Domain 模型可以带业务属性（如 signedAmountInCents、isPersisted），
 *    这些不属于数据库。
 *
 * 标 `internal`：仅模块内可见，防止 UI 层顺手 import 了 Entity。
 */

internal fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    colorHex = colorHex,
    type = type,
)

internal fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    type = type,
)

internal fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    type = type,
    amountInCents = amountInCents,
    dateTime = dateTime,
    tagId = tagId,
    note = note,
    createdAt = createdAt,
)

internal fun Transaction.toEntity(createdAt: Long): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    amountInCents = amountInCents,
    dateTime = dateTime,
    tagId = tagId,
    note = note,
    createdAt = createdAt,
)
