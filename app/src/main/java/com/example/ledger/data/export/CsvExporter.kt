package com.example.ledger.data.export

import android.content.Context
import android.net.Uri
import com.example.ledger.core.util.DateTimeUtils
import com.example.ledger.domain.model.TransactionFilter
import com.example.ledger.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 导出结果：成功带条数，失败带可读原因（会直接显示在 Snackbar 里） */
sealed interface ExportResult {
    data class Success(val rowCount: Int) : ExportResult
    data class Failure(val message: String) : ExportResult
}

/**
 * CSV 导出服务。
 *
 * 只做三件事：查数据 → 拼 CSV → 写进 SAF 给的 Uri。
 * 不碰权限、不碰路径：Uri 由系统的文件选择器返回，我们只有"写入这一个文件"的能力，
 * 这是 Android 上最省事也最安全的落盘方式。
 *
 * `openOutputStream(uri, "wt")` 的 "wt" 表示 truncate 写入：
 * 覆盖用户选中的已存在文件时，不会残留上一次导出的尾部内容。
 */
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
) {

    suspend fun exportTo(uri: Uri, filter: TransactionFilter): ExportResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val rows = transactionRepository.exportRows(filter)
                val csv = CsvWriter.buildCsv(rows)

                val stream = context.contentResolver.openOutputStream(uri, "wt")
                    ?: return@runCatching ExportResult.Failure("无法打开目标文件，请换个位置重试")

                stream.use { output ->
                    output.write(csv.toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                ExportResult.Success(rows.size)
            }.getOrElse { error ->
                ExportResult.Failure(error.message ?: "写入文件失败")
            }
        }

    /** 默认文件名：简易记账_20240601.csv（日期用导出当天，便于区分多次导出） */
    fun suggestedFileName(): String {
        val today = DateTimeUtils.formatCsvDate(DateTimeUtils.now()).replace("-", "")
        return "简易记账_$today.csv"
    }
}
