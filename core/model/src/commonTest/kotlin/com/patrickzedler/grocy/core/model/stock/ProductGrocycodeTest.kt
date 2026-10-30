package com.patrickzedler.grocy.core.model.stock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductGrocycodeTest {

    @Test
    fun productCodesNameTheProduct() {
        assertEquals(ProductGrocycode(12, null), ProductGrocycode.parse("grcy:p:12"))
    }

    @Test
    fun stockEntryCodesAlsoNameTheEntry() {
        assertEquals(
            ProductGrocycode(12, "63f1a2b3c4d5e"),
            ProductGrocycode.parse(" grcy:p:12:63f1a2b3c4d5e "),
        )
    }

    @Test
    fun barcodesAndOtherGrocycodesAreNoProductCodes() {
        assertNull(ProductGrocycode.parse("4006381333931"))
        assertNull(ProductGrocycode.parse("grcy:c:3"))
        assertNull(ProductGrocycode.parse("grcy:p:"))
    }
}
