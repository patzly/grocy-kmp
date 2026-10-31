package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class ShoppingListOptionsRepositoryTest {

    private val stored = ShoppingListOptions(grouping = ShoppingListGrouping.NONE)
    private val settingsStore = FakeSettingsStore(shoppingListOptions = stored)

    private fun TestScope.repository() = ShoppingListOptionsRepository(
        settingsStore,
        // Unconfined, so the options are read right away like at app start
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun storedOptionsAreReadBeforeTheScreenAsksForThem() = runTest {
        assertEquals(stored, repository().options.value)
    }

    @Test
    fun updatesKeepTheOtherOptions() = runTest {
        val repository = repository()

        repository.update { it.copy(fields = setOf(ShoppingListField.BARCODES)) }

        val expected = stored.copy(fields = setOf(ShoppingListField.BARCODES))
        assertEquals(expected, settingsStore.shoppingListOptions.value)
        assertEquals(expected, repository.options.value)
    }

    @Test
    fun statusFilterIsSharedButNeverStored() = runTest {
        val repository = repository()

        repository.setStatus(ShoppingListStatus.UNDONE)

        assertEquals(ShoppingListStatus.UNDONE, repository.status.value)
        assertEquals(stored, settingsStore.shoppingListOptions.value)
        assertNull(repository().status.value)
    }
}
