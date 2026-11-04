package com.patrickzedler.grocy.core.forms

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnknownProductFlowTest {

    private val milk = Product(id = 1, name = "Milk")
    private val fresh = Product(id = 7, name = "Fresh")

    @Test
    fun askingKeepsTheTrimmedTextAndBlankAsksNothing() {
        val flow = UnknownProductFlow()

        flow.ask("  Fresh ")
        assertEquals("Fresh", flow.input)

        flow.dismiss()
        flow.ask("   ")
        assertNull(flow.input)
    }

    @Test
    fun addAsBarcodeKeepsTheCodeAndClearsTheField() {
        val flow = UnknownProductFlow()
        flow.ask("123")
        val key = flow.fieldResetKey

        flow.addAsBarcode()

        assertEquals("123", flow.pendingBarcode)
        assertNull(flow.input)
        assertTrue(flow.fieldResetKey != key)
    }

    @Test
    fun aNewProductWithNameIsTheOneThatAppearsNext() {
        val flow = UnknownProductFlow()
        flow.ask("Fresh")

        val request = flow.newProductWithName(listOf(milk))

        assertEquals(ProductFormRequest(name = "Fresh"), request)
        assertNull(flow.pendingBarcode)
        assertNull(flow.takeCreatedProduct(listOf(milk), emptyList()))
        assertEquals(fresh, flow.takeCreatedProduct(listOf(milk, fresh), emptyList()))
        // Only once, a later refresh must not choose it again
        assertNull(flow.takeCreatedProduct(listOf(milk, fresh), emptyList()))
    }

    @Test
    fun aNewProductWithBarcodeWaitsForThatBarcode() {
        val flow = UnknownProductFlow()
        flow.ask("123")

        val request = flow.newProductWithBarcode()

        assertEquals(ProductFormRequest(barcode = "123"), request)
        assertEquals("123", flow.pendingBarcode)
        // Another new product is not the one asked for
        assertNull(flow.takeCreatedProduct(listOf(milk, fresh), emptyList()))
        val barcode = ProductBarcode(1, productId = 7, barcode = "123")
        assertEquals(fresh, flow.takeCreatedProduct(listOf(milk, fresh), listOf(barcode)))
    }

    @Test
    fun stoppingToWaitIgnoresTheProductArrivingLater() {
        val flow = UnknownProductFlow()
        flow.ask("Fresh")
        flow.newProductWithName(listOf(milk))

        flow.stopWaiting()

        assertNull(flow.takeCreatedProduct(listOf(milk, fresh), emptyList()))
    }

    @Test
    fun scanAgainForgetsTheTextWithoutKeepingACode() {
        val flow = UnknownProductFlow()
        flow.ask("123")
        val key = flow.fieldResetKey

        flow.scanAgain()

        assertNull(flow.input)
        assertNull(flow.pendingBarcode)
        assertTrue(flow.fieldResetKey != key)
    }

    @Test
    fun theNameOfAProductNotOfferedIsGrocysErrorInsteadOfTheQuestion() {
        val flow = UnknownProductFlow()
        val key = flow.fieldResetKey

        assertTrue(flow.onTextWithoutProduct(" fresh ", false, listOf(milk, fresh), emptyList()))

        assertTrue(flow.isNotOffered)
        assertNull(flow.input)
        assertTrue(flow.fieldResetKey != key)
    }

    @Test
    fun theBarcodeOfAProductNotOfferedIsGrocysError() {
        val flow = UnknownProductFlow()
        val barcode = ProductBarcode(1, productId = 7, barcode = "123")

        assertTrue(flow.onTextWithoutProduct("123", false, listOf(milk, fresh), listOf(barcode)))

        assertTrue(flow.isNotOffered)
    }

    @Test
    fun textNobodyKnowsAsksTheQuestion() {
        val flow = UnknownProductFlow()

        assertTrue(flow.onTextWithoutProduct("Other", false, listOf(milk), emptyList()))

        assertEquals("Other", flow.input)
        assertFalse(flow.isNotOffered)
    }

    @Test
    fun aSuggestionContainingTheTextTakesIt() {
        val flow = UnknownProductFlow()

        assertFalse(flow.onTextWithoutProduct("Fresh", true, listOf(milk, fresh), emptyList()))

        assertNull(flow.input)
        assertFalse(flow.isNotOffered)
    }

    @Test
    fun choosingAProductEndsTheError() {
        val flow = UnknownProductFlow()
        flow.onTextWithoutProduct("Fresh", false, listOf(fresh), emptyList())

        flow.stopWaiting()

        assertFalse(flow.isNotOffered)
    }
}
