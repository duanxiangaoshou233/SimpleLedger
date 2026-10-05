package com.example.ledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。
 *
 * 职责：
 *  1) 初始化 Hilt 依赖注入容器（阶段 2 起，DatabaseModule / RepositoryModule 会被装载）；
 *  2) 预留全局初始化位置（阶段 2 若需要"首次启动写入默认标签"，在此触发种子数据）。
 *
 * 注意：这里开启 strictMode 只在 debug 下生效，便于早期发现主线程 IO。
 */
@HiltAndroidApp
class LedgerApplication : Application()
