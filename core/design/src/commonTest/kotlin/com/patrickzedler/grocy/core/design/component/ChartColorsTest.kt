package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartColorsTest {

    @Test
    fun bothModesHaveEveryColorOnce() {
        for (colors in listOf(LightChartColors, DarkChartColors)) {
            assertEquals(CHART_COLOR_COUNT, colors.size)
            assertEquals(CHART_COLOR_COUNT, colors.distinct().size)
        }
    }
}
