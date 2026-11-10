package com.patrickzedler.grocy.core.forms

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmptyListStateTest {

    @Test
    fun aListTheStartScreenDownloadedIsKnownToBeEmpty() {
        assertTrue(isEmptyListKnown(false, storedByStart = true, isEverythingDownloaded = true))
    }

    @Test
    fun beforeTheFirstDownloadItIsNot() {
        assertFalse(isEmptyListKnown(false, storedByStart = true, isEverythingDownloaded = false))
    }

    @Test
    fun aListWithADownloadOfItsOwnWaitsForIt() {
        assertFalse(isEmptyListKnown(false, storedByStart = false, isEverythingDownloaded = true))
    }

    @Test
    fun aFilteredListHasItsDataAlready() {
        assertTrue(isEmptyListKnown(true, storedByStart = false, isEverythingDownloaded = false))
    }

    @Test
    fun anEmptyListIsKnownOnceItsOwnDownloadWorked() {
        assertTrue(
            isEmptyListKnown(
                false,
                storedByStart = false,
                isEverythingDownloaded = false,
                loadedOnce = true,
            ),
        )
    }
}
