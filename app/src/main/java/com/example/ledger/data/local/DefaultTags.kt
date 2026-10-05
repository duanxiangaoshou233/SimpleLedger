package com.example.ledger.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ledger.domain.model.TagType

/**
 * 首次创建数据库时写入的默认标签。
 *
 * 为什么要预置？
 *  - 用户第一笔记账时就有现成标签可点，而不是面对一个空列表被迫先建标签；
 *  - 颜色直接从设计系统的图表色板里挑，保证"标签色 = 图表色"，统计页天然好看。
 *
 * 这些标签是**普通标签**：用户可以随意改名、改色、删除，
 * 因此不做"内置标签不可删"这类限制（少一点特例，少一点 bug）。
 */
internal object DefaultTags {

    private data class Seed(val name: String, val colorHex: String, val type: TagType)

    private val seeds = listOf(
        // ---------------- 支出 ----------------
        Seed("餐饮", "#F08A4B", TagType.EXPENSE),
        Seed("交通", "#4F86E0", TagType.EXPENSE),
        Seed("购物", "#E86BA0", TagType.EXPENSE),
        Seed("居住", "#9C7BF0", TagType.EXPENSE),
        Seed("娱乐", "#F0A93B", TagType.EXPENSE),
        Seed("医疗", "#5BB4D4", TagType.EXPENSE),
        Seed("学习", "#2E9E8F", TagType.EXPENSE),
        Seed("其他", "#3FAE6A", TagType.EXPENSE),
        // ---------------- 收入 ----------------
        Seed("工资", "#2E9E8F", TagType.INCOME),
        Seed("兼职", "#4F86E0", TagType.INCOME),
        Seed("理财", "#F0A93B", TagType.INCOME),
        Seed("红包", "#E86BA0", TagType.INCOME),
        Seed("其他收入", "#F08A4B", TagType.INCOME),
        // ---------------- 通用 ----------------
        Seed("报销", "#3FAE6A", TagType.COMMON),
    )

    /** 在 Room 的 onCreate 回调里执行（此时还没有 DAO 实例，只能用原始 SQL） */
    fun seed(db: SupportSQLiteDatabase) {
        seeds.forEach { seed ->
            db.execSQL(
                "INSERT INTO tags (name, colorHex, type) VALUES (?, ?, ?)",
                arrayOf(seed.name, seed.colorHex, seed.type.name),
            )
        }
    }
}
