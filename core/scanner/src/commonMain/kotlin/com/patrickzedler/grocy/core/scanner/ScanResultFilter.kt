package com.patrickzedler.grocy.core.scanner

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Turns decoder output of consecutive frames into scans, identical for every engine:
 * - linear codes have weak checksums, so they count only after two identical reads in a row
 * - a code is reported once while it stays in view and again only after [repeatDelay] without it
 */
internal class ScanResultFilter(
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val repeatDelay: Duration = 2.seconds,
) {
    private var previousRead: Barcode? = null
    private var reported: Barcode? = null
    private var reportedLastSeen: TimeMark? = null

    /** Returns the barcode to report for this frame, or null. */
    fun accept(barcodes: List<Barcode>): Barcode? {
        val barcode = barcodes.firstOrNull()
        val isConfirmed = barcode != null &&
            (barcode.format.isTwoDimensional || barcode == previousRead)
        previousRead = barcode
        if (barcode == null || !isConfirmed) return null

        val isStillInView = barcode == reported &&
            reportedLastSeen?.let { it.elapsedNow() < repeatDelay } == true
        reported = barcode
        reportedLastSeen = timeSource.markNow()
        return barcode.takeUnless { isStillInView }
    }
}
