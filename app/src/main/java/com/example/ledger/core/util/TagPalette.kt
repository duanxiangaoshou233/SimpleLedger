package com.example.ledger.core.util

/**
 * 标签调色板（十六进制字符串，供数据层新建标签时挑默认色）。
 *
 * ⚠️ 与 `ui/theme/Color.kt` 中的 `chartColors` **刻意保持一致**：
 * 标签色 = 图表分类色，统计页的环形图/柱状图才能"一看颜色就知道是哪一类"。
 * 之所以两边各写一份：数据层不应该依赖 UI 层（domain/data 不许 import compose 的东西），
 * 8 个色值的少量重复，换来干净的依赖方向，是划算的。
 */
object TagPalette {

    /** 与设计系统图表色板同源的 8 色（浅色主题口径） */
    val hexColors: List<String> = listOf(
        "#2E9E8F", // 青绿
        "#4F86E0", // 湖蓝
        "#F0A93B", // 琥珀
        "#9C7BF0", // 藤紫
        "#E86BA0", // 樱粉
        "#3FAE6A", // 苔绿
        "#F08A4B", // 陶橙
        "#5BB4D4", // 天青
    )

    /** 兜底色（颜色全部被占用时使用） */
    const val FALLBACK = "#7A8B86"

    /** 标签颜色可选集合（新建/编辑标签时给用户挑） */
    val selectable: List<String> = hexColors + listOf(
        "#E2574C", // 珊瑚红
        "#B08968", // 咖褐
        "#8D99AE", // 雾灰蓝
        "#C77DFF", // 薰衣草
        "#06A77D", // 森林绿
        "#EF476F", // 玫红
    )
}
