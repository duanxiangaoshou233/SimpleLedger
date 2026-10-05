package com.example.ledger.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ledger.data.local.entity.TransactionEntity
import com.example.ledger.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * 交易表 DAO。
 *
 * 约定：
 *  - 所有返回 Flow 的方法都是"可观察查询"，Room 会在相关表变化时自动重新发射；
 *  - 单条读取 / 写入用 suspend；
 *  - 统计查询尽量在 SQL 里完成聚合（SUM / COUNT / GROUP BY），不把明细拉进内存。
 */
@Dao
interface TransactionDao {

    /* ============================================================ 查询 */

    /**
     * 统一筛选查询：四个条件任意为 null 表示"不限制"。
     *
     * `:param IS NULL OR column = :param` 这个写法让 2×2×2×2 = 16 种筛选组合
     * 共用一条 SQL，Room 编译期就能校验，也避免拼接字符串导致注入风险。
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:tagId IS NULL OR tagId = :tagId)
          AND (:keyword IS NULL OR note LIKE '%' || :keyword || '%')
          AND dateTime >= :startMillis
          AND dateTime < :endMillis
        ORDER BY dateTime DESC, id DESC
        """,
    )
    fun observeFiltered(
        type: TransactionType?,
        tagId: Long?,
        keyword: String?,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<TransactionEntity>>

    /** 首页"最近记录"：只取最新 N 条 */
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    /* ============================================================ 写入 */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /* ============================================================ 统计 */

    /** 某类型在某区间的总额（分）；无数据返回 null，由 Repository 归一化成 0 */
    @Query(
        """
        SELECT SUM(amountInCents) FROM transactions
        WHERE type = :type AND dateTime >= :startMillis AND dateTime < :endMillis
        """,
    )
    suspend fun sumAmount(type: TransactionType, startMillis: Long, endMillis: Long): Long?

    /** 同上，但可观察（首页 Hero 卡要跟着新记账实时变） */
    @Query(
        """
        SELECT SUM(amountInCents) FROM transactions
        WHERE type = :type AND dateTime >= :startMillis AND dateTime < :endMillis
        """,
    )
    fun observeSumAmount(type: TransactionType, startMillis: Long, endMillis: Long): Flow<Long?>

    /** 某区间的记账笔数（用于列表页头部"共 N 笔"） */
    @Query("SELECT COUNT(*) FROM transactions WHERE dateTime >= :startMillis AND dateTime < :endMillis")
    fun observeCountInRange(startMillis: Long, endMillis: Long): Flow<Int>

    /**
     * 按标签汇总。
     * LEFT JOIN 保证"无标签"的账单也出现在结果里（tagId 为 NULL 的那一组）。
     */
    @Query(
        """
        SELECT t.tagId                AS tagId,
               g.name                 AS tagName,
               g.colorHex             AS colorHex,
               SUM(t.amountInCents)   AS total,
               COUNT(t.id)            AS itemCount
        FROM transactions t
        LEFT JOIN tags g ON g.id = t.tagId
        WHERE t.type = :type
          AND t.dateTime >= :startMillis
          AND t.dateTime < :endMillis
        GROUP BY t.tagId
        ORDER BY total DESC
        """,
    )
    suspend fun sumGroupedByTag(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long,
    ): List<TagStatRow>

    /** 趋势图原料：只要 时间 + 金额，按时间升序，分组计算放到 Repository（时区语义更明确） */
    @Query(
        """
        SELECT dateTime, amountInCents FROM transactions
        WHERE type = :type AND dateTime >= :startMillis AND dateTime < :endMillis
        ORDER BY dateTime ASC
        """,
    )
    suspend fun amountsInRange(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long,
    ): List<AmountAtTime>

    /**
     * 导出 CSV / 全量备份用：一次性取全部（含筛选），并 LEFT JOIN 出标签名与颜色。
     * 按时间升序 —— 导出的表格从上往下读更符合直觉。
     */
    @Query(
        """
        SELECT t.*, g.name AS tagName, g.colorHex AS tagColorHex
        FROM transactions t
        LEFT JOIN tags g ON g.id = t.tagId
        WHERE (:type IS NULL OR t.type = :type)
          AND (:tagId IS NULL OR t.tagId = :tagId)
          AND (:keyword IS NULL OR t.note LIKE '%' || :keyword || '%')
          AND t.dateTime >= :startMillis
          AND t.dateTime < :endMillis
        ORDER BY t.dateTime ASC, t.id ASC
        """,
    )
    suspend fun queryForExport(
        type: TransactionType?,
        tagId: Long?,
        keyword: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<TransactionExportRow>
}
