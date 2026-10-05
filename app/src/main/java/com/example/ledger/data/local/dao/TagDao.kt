package com.example.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ledger.data.local.entity.TagEntity
import com.example.ledger.domain.model.TagType
import kotlinx.coroutines.flow.Flow

/**
 * 标签表 DAO。
 *
 * 排序规则统一为 `type ASC, id ASC`：
 * 支出标签 → 收入标签 → 通用标签，同组内按创建顺序，保证 Chip 顺序稳定不跳动。
 */
@Dao
interface TagDao {

    /* ============================================================ 查询 */

    @Query("SELECT * FROM tags ORDER BY type ASC, id ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY type ASC, id ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findByNameAndType(name: String, type: TagType): TagEntity?

    /** 标签 + 使用次数：标签管理页要显示"已用于 N 笔" */
    @Query(
        """
        SELECT g.*, COUNT(t.id) AS usageCount
        FROM tags g
        LEFT JOIN transactions t ON t.tagId = g.id
        GROUP BY g.id
        ORDER BY g.type ASC, g.id ASC
        """,
    )
    fun observeWithUsage(): Flow<List<TagUsageRow>>

    /* ============================================================ 写入 */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TagEntity): Long

    @Update
    suspend fun update(entity: TagEntity)

    @Delete
    suspend fun delete(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    /* ============================================================ 标签删除时的账单处理 */

    /** 该标签被多少笔账单引用（删除前用来提示 + 决定是否弹确认框） */
    @Query("SELECT COUNT(*) FROM transactions WHERE tagId = :tagId")
    suspend fun countUsage(tagId: Long): Int

    /**
     * 把被 sourceTagId 引用的账单批量转移到 targetTagId。
     * targetTagId 传 null 即"设为无标签"（账单保留，只解除关联）。
     * @return 受影响的账单数
     */
    @Query("UPDATE transactions SET tagId = :targetTagId WHERE tagId = :sourceTagId")
    suspend fun reassignTransactions(sourceTagId: Long, targetTagId: Long?): Int
}
