package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ShowNewestSnackbarTest {

    private val host = SnackbarHostState()

    private val shownMessage get() = host.currentSnackbarData?.visuals?.message

    @Test
    fun aNewMessageReplacesTheOneOnScreenAtOnce() = runTest {
        val first = async { host.showNewestSnackbar("first", actionLabel = "Undo") }
        advanceUntilIdle()
        assertEquals("first", shownMessage)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { host.showNewestSnackbar("second") }
        advanceUntilIdle()

        assertEquals("second", shownMessage)
        assertEquals(SnackbarResult.Dismissed, first.await())
    }

    @Test
    fun quickEventsShowOnlyTheLastWithoutWaitingForTheOthers() = runTest {
        val events = Channel<String>(Channel.BUFFERED)
        val undone = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            events.receiveAsFlow().collectLatest { event ->
                val result = host.showNewestSnackbar(event, actionLabel = "Undo")
                if (result == SnackbarResult.ActionPerformed) undone += event
            }
        }

        listOf("first", "second", "third").forEach {
            events.send(it)
            advanceUntilIdle()
            assertEquals(it, shownMessage)
        }

        host.currentSnackbarData?.performAction()
        advanceUntilIdle()

        assertEquals(listOf("third"), undone)
        assertNull(shownMessage)
    }

    @Test
    fun anUndoableMessageWaitsWhileNothingNewComes() = runTest {
        val shown = async { host.showNewestSnackbar("first", actionLabel = "Undo") }
        advanceUntilIdle()

        host.currentSnackbarData?.performAction()

        assertEquals(SnackbarResult.ActionPerformed, shown.await())
    }
}
