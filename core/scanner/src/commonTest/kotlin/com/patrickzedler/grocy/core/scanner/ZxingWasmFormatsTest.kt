package com.patrickzedler.grocy.core.scanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZxingWasmFormatsTest {

    @Test
    fun everyFormatRoundTrips() {
        BarcodeFormat.entries.forEach {
            assertEquals(it, ZxingWasmFormats.formatOf(ZxingWasmFormats.nameOf(it)))
        }
    }

    @Test
    fun variantsMapToTheirSymbology() {
        assertEquals(BarcodeFormat.QR_CODE, ZxingWasmFormats.formatOf("QRCodeModel2"))
        assertEquals(BarcodeFormat.EAN_13, ZxingWasmFormats.formatOf("ISBN"))
        assertEquals(BarcodeFormat.ITF, ZxingWasmFormats.formatOf("ITF14"))
        assertEquals(BarcodeFormat.CODE_39, ZxingWasmFormats.formatOf("Code39Ext"))
    }

    @Test
    fun unsupportedFormatsAreIgnored() {
        assertNull(ZxingWasmFormats.formatOf("MaxiCode"))
        assertNull(ZxingWasmFormats.formatOf("None"))
    }
}
