package com.patrickzedler.grocy.core.scanner.mlkit

import com.patrickzedler.grocy.core.scanner.BarcodeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MlKitFormatsTest {

    @Test
    fun everyFormatMapsToMlKitAndBack() {
        BarcodeFormat.entries.forEach { assertEquals(it, barcodeFormatOf(it.toMlKit())) }
    }

    @Test
    fun everyFormatHasItsOwnMlKitFormat() {
        val mlKitFormats = BarcodeFormat.entries.map { it.toMlKit() }.toSet()

        assertEquals(BarcodeFormat.entries.size, mlKitFormats.size)
    }

    @Test
    fun unknownMlKitFormatIsIgnored() {
        assertNull(barcodeFormatOf(-1))
    }
}
