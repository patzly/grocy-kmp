package com.patrickzedler.grocy.core.data.util

import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource

class RefreshControllerTest {

    @Test
    fun refreshingShowsUntilTheDownloadIsDone() = runTest {
        var downloads = 0
        val controller = RefreshController(this, testTimeSource) { downloads++ }

        controller.refresh()
        assertTrue(controller.state.value.isRefreshing)
        advanceUntilIdle()

        assertEquals(RefreshState(loadedOnce = true), controller.state.value)
        assertEquals(1, downloads)
    }

    @Test
    fun onlyOneDownloadRunsAtATime() = runTest {
        var downloads = 0
        val controller = RefreshController(this, testTimeSource) { downloads++ }

        controller.refresh()
        controller.refresh()
        advanceUntilIdle()

        assertEquals(1, downloads)
    }

    @Test
    fun unreachableServerIsOffline() = runTest {
        val controller = RefreshController(this, testTimeSource) {
            throw GrocyApiException.Unreachable(RuntimeException())
        }

        controller.refresh()
        advanceUntilIdle()

        assertEquals(RefreshError.OFFLINE, controller.state.value.error)
        assertFalse(controller.state.value.isRefreshing)
    }

    @Test
    fun otherErrorsAreFailures() = runTest {
        val controller = RefreshController(this, testTimeSource) {
            throw GrocyApiException.HttpError(500)
        }

        controller.refresh()
        advanceUntilIdle()

        assertEquals(RefreshError.FAILED, controller.state.value.error)
    }

    @Test
    fun failedDownloadAfterAWorkingOneKeepsTheDataLoaded() = runTest {
        var fail = false
        val controller = RefreshController(this, testTimeSource) {
            if (fail) throw GrocyApiException.Unreachable(RuntimeException())
        }
        controller.refresh()
        advanceUntilIdle()
        fail = true

        controller.refresh()
        advanceUntilIdle()

        assertTrue(controller.state.value.loadedOnce)
        assertEquals(RefreshError.OFFLINE, controller.state.value.error)
    }

    @Test
    fun failedFirstDownloadLoadedNothing() = runTest {
        val controller = RefreshController(this, testTimeSource) {
            throw GrocyApiException.Unreachable(RuntimeException())
        }

        controller.refresh()
        advanceUntilIdle()

        assertFalse(controller.state.value.loadedOnce)
    }

    @Test
    fun shownErrorIsCleared() = runTest {
        val controller = RefreshController(this, testTimeSource) {
            throw GrocyApiException.HttpError(500)
        }
        controller.refresh()
        advanceUntilIdle()

        controller.onErrorShown()

        assertNull(controller.state.value.error)
    }
}
