package com.patrickzedler.grocy.feature.stock.impl.reports

import com.patrickzedler.grocy.core.model.stock.SpendingsRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun slicesCoverTheirShareOfTheCircle() {
        assertEquals(listOf(270f, 90f), sliceSweeps(listOf(3.0, 1.0)))
    }

    @Test
    fun aTinySliceKeepsARoundedShapeAndTheCircleStaysFull() {
        val sweeps = sliceSweeps(listOf(1000.0, 500.0, 0.01))

        assertEquals(6f, sweeps[2])
        assertEquals(360f, sweeps.sum(), 0.01f)
        assertEquals(2f, sweeps[0] / sweeps[1], 0.01f)
    }

    @Test
    fun tapsFollowTheEnlargedSlices() {
        val chart = spendingsChartOf(rows(1000.0, 0.01))

        assertEquals(1, sliceAt(chart, 357f))
    }

    @Test
    fun largeSlicesKeepTheFullCorner() {
        assertEquals(16f, sliceCornerRadius(16f, 150f, 100f, 4f, 1.5f))
    }

    @Test
    fun aCornerIsAtMostHalfTheRing() {
        assertEquals(10f, sliceCornerRadius(16f, 120f, 100f, 4f, 1.5f))
    }

    @Test
    fun narrowSlicesGetSmallerCornersThatStillFit() {
        val sweep = 0.1f

        val corner = sliceCornerRadius(16f, 150f, 100f, 4f, sweep)

        assertTrue(corner in 0f..16f)
        assertTrue(corner < 16f)
    }
}
