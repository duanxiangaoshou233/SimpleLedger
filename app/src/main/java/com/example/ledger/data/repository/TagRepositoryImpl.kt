package com.example.ledger.data.repository

import com.example.ledger.core.util.TagPalette
import com.example.ledger.data.local.dao.TagDao
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TagType
import com.example.ledger.domain.model.TagWithUsage
import com.example.ledger.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 标签仓库实现（数据层）。
 *
 * 删除标签是本 App 里唯一"会牵动其它数据"的操作，因此把转移逻辑收在这里：
 *   1) 先把被引用的账单 UPDATE 到目标标签（或置空）；
 *   2) 再删除标签本身。
 * 两步放在同一个 suspend 方法里，UI 层不可能只做一半。
 *
 * （进阶做法是包一层 `@Transaction`。当前两步都是单条 SQL 且都幂等，
 *   即便中途进程被杀，重进 App 再删一次也不会产生脏数据，因此保持简单。）
 */
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTagsWithUsage(): Flow<List<TagWithUsage>> =
        tagDao.observeWithUsage().map { rows ->
            rows.map { row -> TagWithUsage(tag = row.tag.toDomain(), usageCount = row.usageCount) }
        }

    override suspend fun getTag(id: Long): Tag? = tagDao.getById(id)?.toDomain()

    override suspend fun findByName(name: String, type: TagType): Tag? =
        tagDao.findByNameAndType(name.trim(), type)?.toDomain()

    override suspend fun create(tag: Tag): Long = tagDao.insert(tag.toEntity())

    override suspend fun update(tag: Tag) = tagDao.update(tag.toEntity())

    override suspend fun countUsage(tagId: Long): Int = tagDao.countUsage(tagId)

    override suspend fun deleteTag(tagId: Long, reassignTo: Long?): Int {
        val affected = tagDao.reassignTransactions(sourceTagId = tagId, targetTagId = reassignTo)
        tagDao.deleteById(tagId)
        return affected
    }

    override suspend fun suggestColor(): String {
        val existing = tagDao.getAll()
        val used = existing.map { it.colorHex.lowercase() }.toSet()
        // 优先挑一个没人用过的颜色
        return TagPalette.hexColors.firstOrNull { it.lowercase() !in used }
            ?: TagPalette.selectable[existing.size % TagPalette.selectable.size]
    }
}
