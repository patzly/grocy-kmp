package com.patrickzedler.grocy.core.scanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class ScanResultFilterTest {

    private val timeSource = TestTimeSource()
    private val filter = ScanResultFilter(timeSource = timeSource, repeatDelay = 2.seconds)

    private val qrCode = Barcode("https://grocy.example.com/api|key", BarcodeFormat.QR_CODE)
    private val ean = Barcode("4006381333931", BarcodeFormat.EAN_13)

    private fun frame(vararg barcodes: Barcode): Barcode? {
        timeSource += 100.milliseconds
        return filter.accept(barcodes.toList())
    }

    @Test
    fun twoDimensionalCodeIsReportedAtOnce() {
        assertEquals(qrCode, frame(qrCode))
    }

    @Test
    fun linearCodeNeedsTwoIdenticalReadsInARow() {
        assertNull(frame(ean))
        assertEquals(ean, frame(ean))
    }

    @Test
    fun differentReadInBetweenRestartsConfirmation() {
        val misread = ean.copy(value = "4006381333924")

        assertNull(frame(ean))
        assertNull(frame(misread))
        assertNull(frame(ean))
        assertEquals(ean, frame(ean))
    }

    @Test
    fun emptyFrameInBetweenRestartsConfirmation() {
        assertNull(frame(ean))
        assertNull(frame())
        assertNull(frame(ean))
    }

    @Test
    fun codeIsReportedOnceWhileInView() {
        assertEquals(qrCode, frame(qrCode))

        repeat(50) { assertNull(frame(qrCode)) }
    }

    @Test
    fun codeIsReportedAgainAfterBeingOutOfView() {
        frame(qrCode)

        timeSource += 2.seconds

        assertEquals(qrCode, frame(qrCode))
    }

    @Test
    fun newCodeIsReportedRightAway() {
        frame(qrCode)
        val other = Barcode("grcy:p:12", BarcodeFormat.QR_CODE)

        assertEquals(other, frame(other))
    }
}
