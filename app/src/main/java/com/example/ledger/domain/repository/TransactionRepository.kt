package com.example.ledger.domain.repository

import com.example.ledger.domain.model.TagAmount
import com.example.ledger.domain.model.TimeRange
import com.example.ledger.domain.model.Transaction
import com.example.ledger.domain.model.TransactionFilter
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.model.TransactionWithTag
import com.example.ledger.domain.model.TrendGranularity
import com.example.ledger.domain.model.TrendPoint
import com.example.ledger.domain.model.TypeTotals
import kotlinx.coroutines.flow.Flow

/**
 * 账单仓库接口（领域层）。
 *
 * 为什么 UI 只依赖接口？
 *  - ViewModel 只认识"能观察账单流、能算统计"，不关心底层是 Room 还是别的东西；
 *  - 阶段 5 的统计页所有数据都从这里出，图表组件拿到的就是**已经算好的领域对象**，
 *    UI 层不再做任何聚合运算（UI 只负责画）。
 *
 * 线程约定：所有 suspend 方法内部切到 IO（Room 自带），调用方（ViewModel）只需在
 * viewModelScope 里调用即可，不必再 withContext。
 */
interface TransactionRepository {

    /* ============================================================ 读取 */

    /** 按筛选条件观察账单（时间倒序）。默认全部。 */
    fun observeTransactions(filter: TransactionFilter = TransactionFilter.Empty): Flow<List<Transaction>>

    /** 首页"最近记录"：最新 N 条 */
    fun observeRecent(limit: Int = 8): Flow<List<Transaction>>

    suspend fun getById(id: Long): Transaction?

    /* ============================================================ 写入 */

    /**
     * 新增或更新一笔账单，返回其 id。
     * `id == 0L` 视为新增，否则为更新（createdAt 会被保留，不会被覆盖）。
     */
    suspend fun save(transaction: Transaction): Long

    suspend fun delete(transaction: Transaction)

    suspend fun deleteById(id: Long)

    /* ============================================================ 统计 */

    /** 某区间的收入/支出总额（可观察，首页头部用） */
    fun observeTotals(range: TimeRange): Flow<TypeTotals>

    /** 某区间的记账笔数（可观察） */
    fun observeCount(range: TimeRange): Flow<Int>

    /** 某区间的收入/支出总额（一次性读取，导出与统计快照用） */
    suspend fun totals(range: TimeRange = TimeRange.All): TypeTotals

    /** 某类型在某区间的总额（分）；无数据返回 0 */
    suspend fun sumAmount(type: TransactionType, range: TimeRange = TimeRange.All): Long

    /**
     * 按标签汇总（金额降序）。
     * 返回值已算好 `ratio`（占比）与"无标签"虚拟分组，UI 直接用于环形图/排行榜。
     */
    suspend fun sumByTag(
        type: TransactionType,
        range: TimeRange = TimeRange.All,
    ): List<TagAmount>

    /**
     * 趋势数据（柱状图）。
     * 空区间返回空列表；有数据时**补齐零值桶**，这样柱状图的 X 轴间距才均匀。
     */
    suspend fun trend(
        type: TransactionType,
        range: TimeRange,
        granularity: TrendGranularity,
    ): List<TrendPoint>

    /** 导出 CSV 用：一次性拿到账单 + 标签名（按时间升序） */
    suspend fun exportRows(filter: TransactionFilter = TransactionFilter.Empty): List<TransactionWithTag>
}
