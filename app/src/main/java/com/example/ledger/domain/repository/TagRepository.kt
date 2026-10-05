package com.example.ledger.domain.repository

import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TagType
import com.example.ledger.domain.model.TagWithUsage
import kotlinx.coroutines.flow.Flow

/**
 * 标签仓库接口（领域层）。
 *
 * 关于"删除标签时如何处理关联账单"：
 *  这里不做任何隐式级联删除。调用方必须显式选择：
 *  - `deleteTag(id, reassignTo = 其它标签id)` → 账单转移
 *  - `deleteTag(id, reassignTo = null)`      → 账单保留但变成"无标签"
 *  这样用户在对话框里点了什么，数据库里就发生什么，不会静默丢数据。
 */
interface TagRepository {

    /** 观察全部标签（支出 → 收入 → 通用，同组内按创建顺序，保证 Chip 顺序稳定） */
    fun observeTags(): Flow<List<Tag>>

    /** 观察标签及其被引用次数（标签管理页） */
    fun observeTagsWithUsage(): Flow<List<TagWithUsage>>

    suspend fun getTag(id: Long): Tag?

    /** 查重：同一类型下是否已有同名标签（调用前会 trim） */
    suspend fun findByName(name: String, type: TagType): Tag?

    /** 新建标签，返回新 id */
    suspend fun create(tag: Tag): Long

    /** 更新标签（改名/改色/改适用范围） */
    suspend fun update(tag: Tag)

    /** 该标签被多少笔账单引用 */
    suspend fun countUsage(tagId: Long): Int

    /**
     * 删除标签，并把它的账单转移到 [reassignTo]（null = 设为无标签）。
     * @return 受影响的账单数
     */
    suspend fun deleteTag(tagId: Long, reassignTo: Long? = null): Int

    /** 新建标签时推荐一个"还没被用过"的图表色，避免颜色撞车 */
    suspend fun suggestColor(): String
}
