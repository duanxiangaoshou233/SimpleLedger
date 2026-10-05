package com.example.ledger.di

import com.example.ledger.data.repository.TagRepositoryImpl
import com.example.ledger.data.repository.TransactionRepositoryImpl
import com.example.ledger.data.settings.SettingsRepositoryImpl
import com.example.ledger.domain.repository.TagRepository
import com.example.ledger.domain.repository.TransactionRepository
import com.example.ledger.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 仓库绑定。
 *
 * 用 @Binds（接口 -> 实现）而不是 @Provides：
 *  - 生成代码更少、编译更快（@Binds 是纯声明，无方法体）；
 *  - 作用域标在绑定处，实现类保持"纯 Kotlin 类"，方便单元测试里直接 new。
 *
 * 为什么用 abstract class 而不是 object？
 *  - @Binds 方法必须是抽象方法，因此所在 Module 也必须是抽象类。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        impl: TagRepositoryImpl,
    ): TagRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository
}
