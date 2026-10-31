package com.patrickzedler.grocy.core.productcard

import kotlin.test.Test
import kotlin.test.assertEquals

class PriceHistoryChartTest {

    @Test
    fun priceAxisStartsAtZeroInRoundSteps() {
        assertEquals(PriceAxis(8.0, listOf(0.0, 2.0, 4.0, 6.0, 8.0)), priceAxis(7.2))
        assertEquals(PriceAxis(1.0, listOf(0.0, 0.25, 0.5, 0.75, 1.0)), priceAxis(0.99))
        assertEquals(PriceAxis(600.0, listOf(0.0, 200.0, 400.0, 600.0)), priceAxis(420.0))
    }

    @Test
    fun exactMaximumNeedsNoExtraStep() {
        assertEquals(4.0, priceAxis(4.0).max)
    }

    @Test
    fun zeroPricesStillGetAnAxis() {
        assertEquals(1.0, priceAxis(0.0).max)
    }

    @Test
    fun pointsOfAllStoresAreOrderedByDay() {
        val history = PriceHistory(
            series = listOf(
                PriceSeries(SeriesStore.Unknown, 0, listOf(PricePoint(1, 1.0), PricePoint(5, 1.0))),
                PriceSeries(SeriesStore.Others, 1, listOf(PricePoint(3, 2.0))),
            ),
            trend = null,
        )

        assertEquals(
            listOf(1L to 0, 3L to 1, 5L to 0),
            chartPoints(history).map { it.point.epochDay to it.seriesIndex },
        )
    }

    @Test
    fun resourceTemplatesAreFilledInOrder() {
        assertEquals("2 per Pack", "%1\$s per %2\$s".fill("2", "Pack"))
        assertEquals("Pack: 2", "%2\$s: %1\$s".fill("2", "Pack"))
    }
}
