package com.patrickzedler.grocy.core.data.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparisonTest {

    @Test
    fun higherNumberIsNewer() {
        assertTrue(isNewerVersion("1.0.1", than = "1.0.0"))
        assertTrue(isNewerVersion("1.1.0", than = "1.0.9"))
        assertTrue(isNewerVersion("2.0.0", than = "1.12.3"))
    }

    @Test
    fun numbersCompareNumericallyNotAlphabetically() {
        assertTrue(isNewerVersion("1.10.0", than = "1.9.0"))
    }

    @Test
    fun sameOrLowerIsNotNewer() {
        assertFalse(isNewerVersion("1.0.0", than = "1.0.0"))
        assertFalse(isNewerVersion("0.9.0", than = "1.0.0"))
    }

    @Test
    fun missingNumbersCountAsZero() {
        assertFalse(isNewerVersion("1.0", than = "1.0.0"))
        assertTrue(isNewerVersion("1.0.0.1", than = "1.0"))
    }

    @Test
    fun suffixesAreIgnored() {
        assertFalse(isNewerVersion("1.0.0", than = "1.0.0-nightly+abc1234"))
    }

    @Test
    fun unreadableCandidateIsNeverNewer() {
        assertFalse(isNewerVersion("latest", than = "1.0.0"))
    }
}
