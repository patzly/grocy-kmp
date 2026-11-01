package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecondColumnTest {

    @Test
    fun aTabletInLandscapeShowsBothColumns() {
        assertTrue(showsSecondColumn(width = 1280.dp))
    }

    @Test
    fun theExpandedWidthClassShowsBothColumnsFromItsFirstDp() {
        assertTrue(showsSecondColumn(width = 840.dp))
    }

    @Test
    fun aPhoneShowsOneList() {
        assertFalse(showsSecondColumn(width = 411.dp))
    }
}
