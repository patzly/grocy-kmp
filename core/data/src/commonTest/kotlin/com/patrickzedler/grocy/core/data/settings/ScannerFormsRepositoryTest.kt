package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerFormsRepositoryTest {

    @Test
    fun formsStartWithoutScanner() = runTest {
        val repository = ScannerFormsRepository(
            FakeSettingsStore(),
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )

        assertFalse(repository.isOpen(ScannerForm.CONSUME))
    }

    @Test
    fun eachFormRemembersItsOwnScanner() = runTest {
        val store = FakeSettingsStore()
        val repository = ScannerFormsRepository(
            store,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )

        repository.setOpen(ScannerForm.CONSUME, true)
        repository.setOpen(ScannerForm.PURCHASE, true)
        repository.setOpen(ScannerForm.PURCHASE, false)

        assertTrue(repository.isOpen(ScannerForm.CONSUME))
        assertFalse(repository.isOpen(ScannerForm.PURCHASE))
        assertEquals(setOf(ScannerForm.CONSUME), store.scannerForms.value)
    }

    @Test
    fun storedFormsAreShownFromTheStart() = runTest {
        val repository = ScannerFormsRepository(
            FakeSettingsStore(scannerForms = setOf(ScannerForm.INVENTORY)),
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )

        assertTrue(repository.isOpen(ScannerForm.INVENTORY))
    }

    @Test
    fun storedTextStaysReadable() {
        assertEquals(
            setOf(ScannerForm.PURCHASE, ScannerForm.BARCODE),
            ScannerForm.of("PURCHASE,BARCODE,FROM_A_NEWER_VERSION"),
        )
        assertEquals(emptySet(), ScannerForm.of(null))
        assertEquals(
            "CONSUME,SHOPPING_LIST_ITEM",
            ScannerForm.stored(setOf(ScannerForm.SHOPPING_LIST_ITEM, ScannerForm.CONSUME)),
        )
    }
}
