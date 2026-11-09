package com.patrickzedler.grocy.feature.settings.impl.general

import com.patrickzedler.grocy.core.model.start.StartLayout
import com.patrickzedler.grocy.core.model.start.StartLayoutEntry
import com.patrickzedler.grocy.core.model.start.StartSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartLayoutDraftTest {

    private val sections = listOf(
        StartSection.STOCK_OVERVIEW,
        StartSection.SHOPPING_LIST,
        StartSection.RECIPES,
    )
    private val stock = StartSection.STOCK_OVERVIEW.name
    private val shopping = StartSection.SHOPPING_LIST.name
    private val recipes = StartSection.RECIPES.name

    private fun draft(vararg entries: StartLayoutEntry) =
        StartLayoutDraft.of(StartLayout(entries.toList()), sections)

    private fun section(section: StartSection) = StartLayoutEntry.Section(section)

    private val divider = StartLayoutEntry.Divider

    private fun StartLayoutDraft.shownKeys() = shown.map { it.key }

    @Test
    fun whatIsNotShownIsAvailableBelowTheDivider() {
        val draft = draft(section(StartSection.STOCK_OVERVIEW))

        assertEquals(
            listOf(draft.divider.key, shopping, recipes),
            draft.available.map { it.key },
        )
    }

    @Test
    fun theAvailableDividerStaysWhenOneIsTaken() {
        val before = draft(section(StartSection.STOCK_OVERVIEW))
        val taken = before.divider.key

        val after = before.placed(taken, 0)

        assertEquals(listOf(taken, stock), after.shownKeys())
        assertEquals(listOf(after.divider.key, shopping, recipes), after.available.map { it.key })
        assertTrue(after.divider.key != taken)
    }

    @Test
    fun aRemovedDividerDoesNotPileUpInTheAvailableRows() {
        val before = draft(section(StartSection.STOCK_OVERVIEW), divider)

        val after = before.removed(before.shown.last().key)

        assertEquals(1, after.available.count { it is StartLayoutRow.Divider })
    }

    @Test
    fun aRemovedSectionBecomesAvailable() {
        val after = draft(section(StartSection.STOCK_OVERVIEW)).removed(stock)

        assertTrue(after.shown.isEmpty())
        assertTrue(stock in after.available.map { it.key })
    }

    @Test
    fun addingPutsItAtTheEndOfTheStartScreen() {
        val after = draft(section(StartSection.STOCK_OVERVIEW)).added(recipes)

        assertEquals(listOf(stock, recipes), after.shownKeys())
    }

    @Test
    fun movingSwapsWithTheNeighbourAndStopsAtTheEnds() {
        val draft = draft(section(StartSection.STOCK_OVERVIEW), section(StartSection.RECIPES))

        assertEquals(listOf(recipes, stock), draft.moved(stock, 1).shownKeys())
        assertEquals(draft, draft.moved(stock, -1))
        assertEquals(draft, draft.moved(recipes, 1))
    }

    @Test
    fun theDraftBecomesTheLayoutWithItsDividers() {
        val draft = draft(section(StartSection.STOCK_OVERVIEW), divider)

        assertEquals(StartLayout(listOf(section(StartSection.STOCK_OVERVIEW), divider)), draft.toLayout())
    }

    @Test
    fun sectionsTheServerHidesAreNotOffered() {
        val draft = StartLayoutDraft.of(
            StartLayout(listOf(section(StartSection.CALENDAR))),
            sections,
        )

        assertTrue(draft.shown.isEmpty())
        assertFalse(StartSection.CALENDAR.name in draft.available.map { it.key })
    }

    @Test
    fun aRowBelongsInFrontOfTheFirstRowWhoseMiddleIsBelowIt() {
        val others = listOf("a", "b", "c")
        val middles = mapOf("a" to 100f, "b" to 200f, "c" to 300f)

        assertEquals(0, dropIndex(others, middles, 50f))
        assertEquals(1, dropIndex(others, middles, 150f))
        assertEquals(3, dropIndex(others, middles, 350f))
    }

    @Test
    fun rowsScrolledAwayKeepTheirPlaceInTheOrder() {
        val others = listOf("a", "b", "c")

        assertEquals(1, dropIndex(others, mapOf("b" to 100f, "c" to 200f), 50f))
        assertEquals(2, dropIndex(others, mapOf("a" to 100f, "b" to 200f), 300f))
    }

    @Test
    fun withNoOtherRowOnTheScreenThereIsNoPlace() {
        assertNull(dropIndex(listOf("a"), emptyMap(), 50f))
    }
}
