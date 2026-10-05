package com.example.ledger.data.repository

import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.data.local.dao.TransactionDao
import com.example.ledger.domain.model.TagAmount
import com.example.ledger.domain.model.TimeRange
import com.example.ledger.domain.model.Transaction
import com.example.ledger.domain.model.TransactionFilter
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.model.TransactionWithTag
import com.example.ledger.domain.model.TrendGranularity
import com.example.ledger.domain.model.TrendPoint
import com.example.ledger.domain.model.TypeTotals
import com.example.ledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 账单仓库实现（数据层）。
 *
 * 这一层是"业务规则的落地点"：
 *  - 把 UI 的筛选条件翻译成 DAO 参数（null 语义统一在这里处理）；
 *  - 把 SQL 的聚合结果加工成领域对象（占比、无标签分组、零值补桶）；
 *  - 维护 createdAt 这类"数据自身属性"，UI 不需要关心。
 *
 * 注意：这里**不写 withContext(Dispatchers.IO)** —— Room 的 suspend / Flow 查询
 * 内部已经跑在 Room 自己的 IO 线程池上，再包一层只是多余开销。
 */
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    /* ============================================================ 读取 */

    override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.observeFiltered(
            type = filter.type,
            tagId = filter.tagId,
            keyword = filter.keyword?.trim()?.takeIf { it.isNotEmpty() },
            startMillis = filter.range.startMillis,
            endMillis = filter.range.endMillisExclusive,
        ).map { entities -> entities.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    /* ============================================================ 写入 */

    override suspend fun save(transaction: Transaction): Long {
        val now = DateTimeUtils.now()
        return if (transaction.isPersisted) {
            // 更新时保留原始 createdAt（它是"这笔账什么时候被记下来的"，不该被编辑动作刷新）
            val originalCreatedAt = transactionDao.getById(transaction.id)?.createdAt ?: now
            transactionDao.update(transaction.toEntity(createdAt = originalCreatedAt))
            transaction.id
        } else {
            transactionDao.insert(transaction.toEntity(createdAt = now))
        }
    }

    override suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity(createdAt = transaction.createdAt))
    }

    override suspend fun deleteById(id: Long) {
        transactionDao.deleteById(id)
    }

    /* ============================================================ 统计 */

    override fun observeTotals(range: TimeRange): Flow<TypeTotals> = combine(
        transactionDao.observeSumAmount(
            type = TransactionType.EXPENSE,
            startMillis = range.startMillis,
            endMillis = range.endMillisExclusive,
        ),
        transactionDao.observeSumAmount(
            type = TransactionType.INCOME,
            startMillis = range.startMillis,
            endMillis = range.endMillisExclusive,
        ),
    ) { expense, income ->
        TypeTotals(
            expenseInCents = expense ?: 0L,
            incomeInCents = income ?: 0L,
        )
    }

    override fun observeCount(range: TimeRange): Flow<Int> =
        transactionDao.observeCountInRange(range.startMillis, range.endMillisExclusive)

    override suspend fun totals(range: TimeRange): TypeTotals = TypeTotals(
        expenseInCents = sumAmount(TransactionType.EXPENSE, range),
        incomeInCents = sumAmount(TransactionType.INCOME, range),
    )

    override suspend fun sumAmount(type: TransactionType, range: TimeRange): Long =
        transactionDao.sumAmount(type, range.startMillis, range.endMillisExclusive) ?: 0L

    override suspend fun sumByTag(type: TransactionType, range: TimeRange): List<TagAmount> {
        val rows = transactionDao.sumGroupedByTag(type, range.startMillis, range.endMillisExclusive)
        if (rows.isEmpty()) return emptyList()

        // 占比在数据层算好：UI 只负责画环/画条，避免"每个图表各算一遍，算出不同结果"
        val total = rows.sumOf { it.total }
        return rows.map { row ->
            TagAmount(
                tagId = row.tagId,
                tagName = row.tagName ?: TagAmount.NO_TAG_NAME,
                colorHex = row.colorHex ?: TagAmount.NO_TAG_COLOR_HEX,
                amount = row.total,
                count = row.itemCount,
                ratio = if (total > 0L) row.total.toFloat() / total.toFloat() else 0f,
            )
        }
    }

    override suspend fun trend(
        type: TransactionType,
        range: TimeRange,
        granularity: TrendGranularity,
    ): List<TrendPoint> {
        val rows = transactionDao.amountsInRange(type, range.startMillis, range.endMillisExclusive)
        if (rows.isEmpty()) return emptyList()

        // "全部时间"没有明确边界，就用数据自身的跨度；否则严格按用户选的区间补零值桶，
        // 这样柱状图的 X 轴才会等距（没有支出的日子也要占一格）。
        val startMillis = if (range.isAll) rows.first().dateTime else range.startMillis
        val endMillisExclusive =
            if (range.isAll) rows.last().dateTime + 1L else range.endMillisExclusive

        val buckets = buildBuckets(startMillis, endMillisExclusive, granularity)
        if (buckets.isEmpty()) return emptyList()

        // 双指针聚合：rows 与 buckets 都已按时间升序，整体 O(n + m)，不做嵌套遍历
        val sums = LongArray(buckets.size)
        var bucketIndex = 0
        for (row in rows) {
            while (bucketIndex < buckets.size && row.dateTime >= buckets[bucketIndex].second) {
                bucketIndex++
            }
            if (bucketIndex >= buckets.size) break
            if (row.dateTime >= buckets[bucketIndex].first) {
                sums[bucketIndex] += row.amountInCents
            }
        }

        return buckets.mapIndexed { index, bucket ->
            TrendPoint(
                bucketStartMillis = bucket.first,
                label = DateTimeUtils.trendLabel(bucket.first, granularity),
                amount = sums[index],
            )
        }
    }

    override suspend fun exportRows(filter: TransactionFilter): List<TransactionWithTag> =
        transactionDao.queryForExport(
            type = filter.type,
            tagId = filter.tagId,
            keyword = filter.keyword?.trim()?.takeIf { it.isNotEmpty() },
            startMillis = filter.range.startMillis,
            endMillis = filter.range.endMillisExclusive,
        ).map { row ->
            TransactionWithTag(
                transaction = row.transaction.toDomain(),
                tagName = row.tagName,
                tagColorHex = row.tagColorHex,
            )
        }

    /* ============================================================ 内部工具 */

    /**
     * 生成 [start, end) 覆盖到的所有桶（含没有数据的桶）。
     * 桶数上限 [MAX_BUCKETS]：防止用户选了一个 20 年的自定义区间时，
     * 生成上万根柱子把内存和帧率拖死。
     */
    private fun buildBuckets(
        startMillis: Long,
        endMillisExclusive: Long,
        granularity: TrendGranularity,
    ): List<Pair<Long, Long>> {
        val buckets = ArrayList<Pair<Long, Long>>()
        var cursor = DateTimeUtils.bucketStart(startMillis, granularity)
        while (cursor < endMillisExclusive && buckets.size < MAX_BUCKETS) {
            val next = DateTimeUtils.nextBucketStart(cursor, granularity)
            buckets += cursor to next
            cursor = next
        }
        return buckets
    }

    private companion object {
        const val MAX_BUCKETS = 400
    }
}
