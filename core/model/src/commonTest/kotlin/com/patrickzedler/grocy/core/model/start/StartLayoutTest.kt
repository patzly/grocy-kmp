package com.patrickzedler.grocy.core.model.start

import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StartLayoutTest {

    private fun section(section: StartSection) = StartLayoutEntry.Section(section)

    private val stock = section(StartSection.STOCK_OVERVIEW)
    private val shopping = section(StartSection.SHOPPING_LIST)
    private val recipes = section(StartSection.RECIPES)
    private val divider = StartLayoutEntry.Divider

    @Test
    fun theDefaultIsTheFormerArrangementOfTheStartScreen() {
        assertEquals(
            listOf(
                listOf(StartSection.STOCK_OVERVIEW, StartSection.SHOPPING_LIST),
                listOf(StartSection.RECIPES, StartSection.MEAL_PLAN),
                listOf(
                    StartSection.CHORES,
                    StartSection.TASKS,
                    StartSection.BATTERIES,
                    StartSection.EQUIPMENT,
                    StartSection.CALENDAR,
                ),
                listOf(StartSection.MASTER_DATA),
            ),
            StartLayout.Default.groups(null),
        )
    }

    @Test
    fun aDividerStartsANewGroupAndEmptyGroupsVanish() {
        val layout = StartLayout(listOf(divider, stock, divider, divider, recipes, divider))

        assertEquals(
            listOf(listOf(StartSection.STOCK_OVERVIEW), listOf(StartSection.RECIPES)),
            layout.groups(null),
        )
    }

    @Test
    fun whatTheServerTurnedOffIsLeftOut() {
        val layout = StartLayout(listOf(stock, divider, recipes))

        assertEquals(
            listOf(listOf(StartSection.RECIPES)),
            layout.groups(ServerConfig(stock = false)),
        )
    }

    @Test
    fun aHiddenSectionComesBackBehindTheOneItFollowed() {
        val previous = StartLayout(listOf(stock, shopping, recipes))
        val edited = StartLayout(listOf(recipes, stock))

        val merged = edited.withHiddenFrom(previous) { it == StartSection.SHOPPING_LIST }

        assertEquals(listOf(recipes, stock, shopping), merged.entries)
    }

    @Test
    fun aRemovedSectionStaysRemoved() {
        val merged = StartLayout(listOf(stock))
            .withHiddenFrom(StartLayout(listOf(stock, shopping))) { false }

        assertFalse(shopping in merged.entries)
    }

    @Test
    fun theStoredFormatStaysReadable() {
        assertEquals(
            StartLayout(listOf(stock, divider, recipes)),
            StartLayout.of("STOCK_OVERVIEW,-,RECIPES"),
        )
        assertEquals("STOCK_OVERVIEW,-,RECIPES", StartLayout(listOf(stock, divider, recipes)).stored())
    }

    @Test
    fun nothingStoredIsTheDefaultAndAnEmptyLayoutStaysEmpty() {
        assertEquals(StartLayout.Default, StartLayout.of(null))
        assertEquals(emptyList(), StartLayout.of("").entries)
    }

    @Test
    fun unknownAndRepeatedSectionsAreLeftOut() {
        assertEquals(
            StartLayout(listOf(stock)),
            StartLayout.of("FUTURE,STOCK_OVERVIEW,STOCK_OVERVIEW"),
        )
    }
}
