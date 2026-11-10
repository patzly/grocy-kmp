package com.patrickzedler.grocy.feature.settings.impl.general

import com.patrickzedler.grocy.core.model.start.StartLayout
import com.patrickzedler.grocy.core.model.start.StartSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartLayoutDragTest {

    private val draft = StartLayoutDraft.of(StartLayout.Default, StartSection.entries)

    private fun speedAt(top: Float) = edgeScrollSpeed(
        top = top,
        rowSize = 100,
        viewportStart = 0,
        viewportEnd = 1000,
        edge = 100f,
        maxSpeed = 800f,
    )

    private fun List<DialogRow>.keys() = map { it.key }

    @Test
    fun aRowHeldInTheMiddleDoesNotScroll() {
        assertEquals(0f, speedAt(400f))
    }

    @Test
    fun scrollingRisesGentlyTowardsTheEdges() {
        assertEquals(-800f, speedAt(0f))
        assertEquals(-200f, speedAt(50f))
        assertEquals(200f, speedAt(850f))
        assertEquals(800f, speedAt(900f))
    }

    @Test
    fun aRowHeldBeyondTheEdgeScrollsNoFasterThanTheMaximum() {
        assertEquals(-800f, speedAt(-300f))
        assertEquals(800f, speedAt(1400f))
    }

    @Test
    fun withoutAHeldRowEveryRowIsInItsGroup() {
        val rows = dialogRowsOf(draft).filterIsInstance<DialogRow.Movable>()

        assertEquals(draft.shown, rows.filter { it.isShown }.map { it.row })
        assertEquals(draft.available, rows.filterNot { it.isShown }.map { it.row })
    }

    @Test
    fun aRowHeldOverTheAvailableRowsStandsWhereTheFingerIs() {
        val held = draft.shown.first().key

        val keys = dialogRowsOf(draft, HeldInAvailable(held, 0)).keys()

        assertEquals(held, keys[keys.indexOf("available") + 1])
        assertEquals(draft.divider.key, keys[keys.indexOf("available") + 2])
    }

    @Test
    fun aRowFromTheAvailableRowsMovesAmongThem() {
        val layout = StartLayout.of("STOCK_OVERVIEW")
        val short = StartLayoutDraft.of(layout, StartSection.entries)
        val held = short.available[2].key

        val keys = dialogRowsOf(short, HeldInAvailable(held, 0)).keys()

        assertEquals(held, keys[keys.indexOf("available") + 1])
    }

    @Test
    fun aHeldRowDoesNotChangeTheDraftOrHowManyRowsThereAre() {
        val held = draft.shown.first().key

        assertEquals(dialogRowsOf(draft).size, dialogRowsOf(draft, HeldInAvailable(held, 1)).size)
        assertTrue(draft.isShown(held))
    }

    @Test
    fun theGroupShapesCountTheHeldRowInItsNewGroup() {
        val held = draft.shown.first().key

        val rows = dialogRowsOf(draft, HeldInAvailable(held, 1))
            .filterIsInstance<DialogRow.Movable>()

        assertEquals(draft.shown.size - 1, rows.first { it.isShown }.count)
        assertEquals(draft.available.size + 1, rows.first { !it.isShown }.count)
    }
}
