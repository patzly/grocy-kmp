package com.patrickzedler.grocy.core.scanner

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ScannerHeightTest {

    @Test
    fun portraitPhoneShowsSixteenToTen() {
        assertEquals(225.dp, scannerHeight(width = 360.dp, windowHeight = 800.dp))
    }

    @Test
    fun landscapePhoneGetsAWideStrip() {
        // 640.dp would be 400.dp high, the whole window
        assertEquals(180.dp, scannerHeight(width = 640.dp, windowHeight = 400.dp))
    }

    @Test
    fun wideWindowGetsAStrip() {
        // 576.dp would be 360.dp high, most of a laptop's browser window
        assertEquals(240.dp, scannerHeight(width = 576.dp, windowHeight = 1000.dp))
    }

    @Test
    fun tinyWindowKeepsAUsableHeight() {
        assertEquals(140.dp, scannerHeight(width = 640.dp, windowHeight = 200.dp))
    }

    @Test
    fun narrowScannerIsNeverTallerThanSixteenToTen() {
        assertEquals(100.dp, scannerHeight(width = 160.dp, windowHeight = 200.dp))
    }
}
