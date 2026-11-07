package com.patrickzedler.grocy.feature.stock.impl.reports

import com.patrickzedler.grocy.core.model.stock.SpendingsRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpendingsChartTest {

    private fun rows(vararg totals: Double) =
        totals.mapIndexed { index, total -> SpendingsRow(index, "Row $index", total) }

    @Test
    fun largestRowsGetTheColorsInOrder() {
        val chart = spendingsChartOf(rows(1.0, 5.0, 3.0))

        assertEquals(listOf(5.0, 3.0, 1.0), chart.slices.map { it.total })
        // Colors follow the rows' size, the table keeps Grocy's order by name
        assertEquals(listOf(2, 0, 1), chart.colorOf)
        assertEquals(9.0, chart.total)
    }

    @Test
    fun rowsBeyondThePaletteShareOneSliceOther() {
        val chart = spendingsChartOf(rows(9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0))

        assertEquals(SPENDINGS_COLORS, chart.slices.size)
        assertEquals(SpendingsSlice(null, null, 3.0), chart.slices.last())
        assertNull(chart.colorOf[7])
        assertNull(chart.colorOf[8])
    }

    @Test
    fun asManyRowsAsColorsNeedNoOther() {
        val chart = spendingsChartOf(rows(8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0))

        assertEquals((0 until SPENDINGS_COLORS).toList(), chart.slices.map { it.slot })
    }

    @Test
    fun tapsFindTheirSliceClockwiseFromTheTop() {
        val chart = spendingsChartOf(rows(3.0, 1.0))

        assertEquals(0, sliceAt(chart, 10f))
        assertEquals(0, sliceAt(chart, 269f))
        assertEquals(1, sliceAt(chart, 271f))
        assertNull(sliceAt(spendingsChartOf(emptyList()), 10f))
    }

    @Test
    fun smallSharesKeepADecimal() {
        assertEquals("4.5", percentText(0.045, '.'))
        assertEquals("33", percentText(1.0 / 3, '.'))
    }
}
