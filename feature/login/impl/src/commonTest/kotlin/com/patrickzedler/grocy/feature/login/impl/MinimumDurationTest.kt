package com.patrickzedler.grocy.feature.login.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource

class MinimumDurationTest {

    @Test
    fun fastBlockIsExtendedToMinimum() = runTest {
        val result = withMinimumDuration(500.milliseconds, testTimeSource) {
            delay(100)
            "done"
        }

        assertEquals("done", result)
        assertEquals(500, currentTime)
    }

    @Test
    fun slowBlockIsNotExtended() = runTest {
        withMinimumDuration(500.milliseconds, testTimeSource) { delay(800) }

        assertEquals(800, currentTime)
    }

    @Test
    fun failureIsRethrownAfterMinimum() = runTest {
        assertFailsWith<IllegalStateException> {
            withMinimumDuration(500.milliseconds, testTimeSource) { error("failed") }
        }

        assertEquals(500, currentTime)
    }
}
