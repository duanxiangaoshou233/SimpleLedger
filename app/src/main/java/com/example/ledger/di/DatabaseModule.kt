package com.example.ledger.di

import android.content.Context
import com.example.ledger.data.local.LedgerDatabase
import com.example.ledger.data.local.dao.TagDao
import com.example.ledger.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库相关依赖。
 *
 * @Provides 而不是 @Binds：因为 Room 的实例是"造出来"的（Builder 模式），
 * 不是靠构造函数注入的。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLedgerDatabase(
        @ApplicationContext context: Context,
    ): LedgerDatabase = LedgerDatabase.build(context)

    @Provides
    fun provideTransactionDao(database: LedgerDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTagDao(database: LedgerDatabase): TagDao = database.tagDao()
}
