package com.example.ledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ledger.data.local.dao.TagDao
import com.example.ledger.data.local.dao.TransactionDao
import com.example.ledger.data.local.entity.TagEntity
import com.example.ledger.data.local.entity.TransactionEntity

/**
 * 应用唯一的 Room 数据库。
 *
 * 版本策略：
 *  - `version = 1` 起步，`exportSchema = true` + ksp 的 `room.schemaLocation`
 *    会把建表 DDL 导出到 `app/schemas/`，以后加字段时照着写 Migration 即可；
 *  - **刻意不加 `fallbackToDestructiveMigration()`**：那会在版本升级时静默清空用户账本，
 *    对记账 App 是不可接受的。以后改表结构必须显式写 Migration。
 *
 * 首次创建时通过 Callback 写入默认标签。
 */
@Database(
    entities = [
        TransactionEntity::class,
        TagEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "ledger.db"

        /** 建表后写入默认标签 */
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DefaultTags.seed(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // 外键约束默认是关闭的：显式打开，保证 TagEntity 删除时
                // transactions.tagId 会按 SET_NULL 规则被置空（数据安全兜底）
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        fun build(context: Context): LedgerDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LedgerDatabase::class.java,
                DATABASE_NAME,
            )
                .addCallback(seedCallback)
                .build()
    }
}
