package com.patrickzedler.grocy.feature.settings.impl.log

import com.patrickzedler.grocy.core.data.log.LogReader
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class LogViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var reads = 0
    private var log = "E/Grocy: first"
    private val reader = LogReader {
        reads++
        log
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun logLoadsRightAway() = runTest {
        val viewModel = LogViewModel(reader)
        assertNull(viewModel.log)

        advanceUntilIdle()

        assertEquals("E/Grocy: first", viewModel.log)
    }

    @Test
    fun reloadReadsTheNewestErrors() = runTest {
        val viewModel = LogViewModel(reader)
        advanceUntilIdle()
        log = "E/Grocy: second"

        viewModel.reload()
        assertNull(viewModel.log)
        advanceUntilIdle()

        assertEquals("E/Grocy: second", viewModel.log)
        assertEquals(2, reads)
    }
}
