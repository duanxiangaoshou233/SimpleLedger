package com.example.ledger.ui.common

import androidx.compose.runtime.Immutable
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.core.util.MoneyFormatter
import com.example.ledger.domain.model.Tag
import com.example.ledger.domain.model.TagAmount
import com.example.ledger.domain.model.Transaction
import com.example.ledger.domain.model.TransactionType
import com.example.ledger.domain.model.TrendGranularity

/**
 * 列表里的一行账单（UI 模型）。
 *
 * 为什么要额外做一层 UI 模型（而不是把 Transaction 直接丢给 Composable）？
 *  - 金额格式化、标签名/色回填、日/时间文案都属于"展示决策"，统一在这里做完，
 *    Composable 里就只剩 `Text(item.signedAmountText)`，不会到处散落 format 调用；
 *  - 金额字符串是**预先算好的**，滚动时不会有 DecimalFormat 的开销。
 */
@Immutable
data class TransactionUiModel(
    val id: Long,
    val type: TransactionType,
    val amountInCents: Long,
    /** "-38.50" / "+8,200.00"，已带符号与千分位 */
    val signedAmountText: String,
    /** "38.50"，不带符号与千分位（编辑页大数字用） */
    val plainAmountText: String,
    val tagId: Long?,
    val tagName: String,
    val tagColorHex: String,
    val note: String,
    val dateTime: Long,
    /** "今天" / "昨天" / "6月1日" */
    val dayLabel: String,
    /** "14:30" */
    val timeText: String,
    /** "2024年6月1日 14:30"（详情/编辑用） */
    val fullDateTimeText: String,
) {
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
}

/** 按天分组后的列表区块 */
@Immutable
data class TransactionDayGroup(
    /** 当天 00:00 的 epoch millis，可作为 LazyColumn 的稳定 key */
    val dayStartMillis: Long,
    val dayLabel: String,
    val items: List<TransactionUiModel>,
    val expenseInCents: Long,
    val incomeInCents: Long,
) {
    /** 当天小计文案："-120.00" / "+8,200.00" / "-120.00  +20.00" */
    val summaryText: String
        get() = buildString {
            if (expenseInCents > 0L) append("-").append(MoneyFormatter.format(expenseInCents))
            if (expenseInCents > 0L && incomeInCents > 0L) append("  ")
            if (incomeInCents > 0L) append("+").append(MoneyFormatter.format(incomeInCents))
        }
}

/**
 * Transaction -> TransactionUiModel。
 *
 * @param tagsById 标签字典。由调用方用 `tags.associateBy { it.id }` 预先构建，
 *                 避免在 map 里对每条账单线性查标签（列表长时会变成 O(n²)）。
 */
fun Transaction.toUiModel(tagsById: Map<Long, Tag>): TransactionUiModel {
    val tag = tagId?.let { tagsById[it] }
    return TransactionUiModel(
        id = id,
        type = type,
        amountInCents = amountInCents,
        signedAmountText = type.sign + MoneyFormatter.format(amountInCents),
        plainAmountText = MoneyFormatter.formatPlain(amountInCents),
        tagId = tag?.id,
        tagName = tag?.name ?: TagAmount.NO_TAG_NAME,
        tagColorHex = tag?.colorHex ?: TagAmount.NO_TAG_COLOR_HEX,
        note = note,
        dateTime = dateTime,
        dayLabel = DateTimeUtils.friendlyDayLabel(dateTime),
        timeText = DateTimeUtils.formatTime(dateTime),
        fullDateTimeText = DateTimeUtils.formatDateTime(dateTime),
    )
}

/** 批量转换（列表页/首页最近记录用） */
fun List<Transaction>.toUiModels(tagsById: Map<Long, Tag>): List<TransactionUiModel> =
    map { it.toUiModel(tagsById) }

/**
 * 按天分组。输入必须**已按时间倒序**（DAO 保证了这一点），
 * `groupBy` 使用 LinkedHashMap 保序，因此分组结果天然是"最近的日期在上"。
 */
fun List<TransactionUiModel>.groupByDay(): List<TransactionDayGroup> =
    groupBy { DateTimeUtils.bucketStart(it.dateTime, TrendGranularity.DAY) }
        .map { (dayStart, items) ->
            TransactionDayGroup(
                dayStartMillis = dayStart,
                dayLabel = items.first().dayLabel,
                items = items,
                expenseInCents = items.filter { it.isExpense }.sumOf { it.amountInCents },
                incomeInCents = items.filterNot { it.isExpense }.sumOf { it.amountInCents },
            )
        }
        .sortedByDescending { it.dayStartMillis }
