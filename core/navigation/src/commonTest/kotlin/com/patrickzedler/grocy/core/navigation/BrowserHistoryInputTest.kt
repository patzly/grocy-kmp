package com.patrickzedler.grocy.core.navigation

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class BrowserHistoryInputTest {

    private val dispatcher = NavigationEventDispatcher()
    private var app = FakeBackStack()
    private var userAgentTransitions = 0

    // Not backgroundScope: advanceUntilIdle leaves its work undone
    private var inputScope: CoroutineScope? = null
    private lateinit var input: BrowserHistoryInput

    @AfterTest
    fun tearDown() {
        inputScope?.cancel()
        dispatcher.dispose()
    }

    private fun TestScope.connect(browser: FakeBrowser) {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        inputScope = scope
        dispatcher.addHandler(app)
        input = BrowserHistoryInput(
            history = browser,
            initialIndex = app.size - 1,
            scope = scope,
            awaitFrame = { delay(FRAME) },
            isScreen = { it is ScreenInfo },
            onUserAgentTransition = { userAgentTransitions++ },
        )
        dispatcher.addInput(input)
        advanceUntilIdle()
    }

    @Test
    fun everyScreenGetsOneBrowserEntry() = runTest {
        val browser = FakeBrowser()
        connect(browser)

        app.navigate()
        app.navigate()
        advanceUntilIdle()

        assertEquals(listOf<Int?>(0, 1, 2), browser.entries)
        assertEquals(2, browser.position)
    }

    @Test
    fun browserBackGoesBackInTheApp() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        app.navigate()
        advanceUntilIdle()

        browser.userBack()
        advanceUntilIdle()

        assertEquals(2, app.size)
        assertEquals(1, browser.position)
    }

    @Test
    fun browserBackOnTheFirstScreenLeavesTheApp() = runTest {
        val browser = FakeBrowser()
        connect(browser)

        // No entry of the app is left to go back to, so the browser leaves the page
        assertEquals(listOf<Int?>(0), browser.entries)
        assertEquals(0, browser.position)
    }

    @Test
    fun appBackMovesTheBrowserBackWithoutAnotherAppBack() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        app.navigate()
        advanceUntilIdle()

        app.back()
        advanceUntilIdle()

        assertEquals(2, app.size)
        assertEquals(1, browser.position)
    }

    @Test
    fun replacingTheBackStackReturnsTheBrowserToTheFirstEntry() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        advanceUntilIdle()

        app.replaceAll()
        advanceUntilIdle()

        assertEquals(1, app.size)
        assertEquals(0, browser.position)
    }

    @Test
    fun browserForwardIsReverted() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        advanceUntilIdle()
        browser.userBack()
        advanceUntilIdle()

        browser.userForward()
        advanceUntilIdle()

        assertEquals(1, app.size)
        assertEquals(0, browser.position)
    }

    @Test
    fun browserBackClosesAnOverlayBeforeTheScreen() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        val dialog = FakeDialog()
        dispatcher.addHandler(dialog)
        advanceUntilIdle()
        assertEquals(2, browser.position)

        browser.userBack()
        advanceUntilIdle()

        assertTrue(dialog.isDismissed)
        assertEquals(2, app.size)
        assertEquals(1, browser.position)
    }

    @Test
    fun overlayDismissedInTheAppRemovesItsEntry() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        val dialog = FakeDialog()
        dispatcher.addHandler(dialog)
        advanceUntilIdle()

        dialog.remove()
        advanceUntilIdle()

        assertEquals(0, browser.position)
    }

    @Test
    fun reloadWithoutRestoredScreensReturnsTheBrowserToTheFirstScreen() = runTest {
        val browser = FakeBrowser(entries = listOf(0, 1, 2), position = 2)

        connect(browser)

        assertEquals(0, browser.position)
        assertEquals(1, app.size)
    }

    @Test
    fun reloadKeepsTheBrowserOnTheRestoredScreen() = runTest {
        val browser = FakeBrowser(entries = listOf(0, 1, 2), position = 2)
        app = FakeBackStack(initialSize = 3)

        connect(browser)

        assertEquals(2, browser.position)
        assertEquals(listOf<Int?>(0, 1, 2), browser.entries)
    }

    @Test
    fun reloadOnADialogEntryReturnsToItsScreen() = runTest {
        // Dialogs are gone after a reload, their entry sits above the screen's
        val browser = FakeBrowser(entries = listOf(0, 1, 2), position = 2)
        app = FakeBackStack(initialSize = 2)

        connect(browser)

        assertEquals(1, browser.position)
    }

    @Test
    fun restoredScreensGoBackThroughTheirEntries() = runTest {
        val browser = FakeBrowser(entries = listOf(0, 1, 2), position = 2)
        app = FakeBackStack(initialSize = 3)
        connect(browser)

        browser.userBack()
        advanceUntilIdle()

        assertEquals(2, app.size)
    }

    @Test
    fun anotherScreenAtTheSameIndexRewritesTheEntry() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        advanceUntilIdle()

        // E.g. a login replaces the back stack: the first entry must show the new screen
        app.replaceAll()
        advanceUntilIdle()

        assertEquals(0, browser.position)
        assertEquals(0, browser.replacedAt.last())
    }

    @Test
    fun newFirstScreenRewritesTheFirstEntry() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        val writes = browser.replacedAt.size

        // E.g. the login replaced by the start screen, which the dispatcher does not report
        input.onBackStackChanged(lastIndex = 0)

        assertEquals(listOf(0), browser.replacedAt.drop(writes))
    }

    @Test
    fun halfDoneNavigationDoesNotRewriteThePreviousEntry() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        val writes = browser.replacedAt.size

        // The back stack grew, the dispatcher has not reported the new screen yet
        input.onBackStackChanged(lastIndex = 1)

        assertEquals(emptyList(), browser.replacedAt.drop(writes))
    }

    @Test
    fun entryOfAnOpenOverlayIsNotRewritten() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        dispatcher.addHandler(FakeDialog())
        advanceUntilIdle()
        val writes = browser.replacedAt.size

        input.onBackStackChanged(lastIndex = 0)

        assertEquals(emptyList(), browser.replacedAt.drop(writes))
    }

    @Test
    fun animatedBrowserBackIsReported() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        advanceUntilIdle()

        browser.userBack(hasUserAgentTransition = true)
        advanceUntilIdle()

        assertEquals(1, userAgentTransitions)
    }

    @Test
    fun plainBrowserBackIsNotReportedAsAnimated() = runTest {
        val browser = FakeBrowser()
        connect(browser)
        app.navigate()
        advanceUntilIdle()

        browser.userBack()
        advanceUntilIdle()

        assertEquals(0, userAgentTransitions)
    }

    private data class ScreenInfo(val index: Int) : NavigationEventInfo()

    private object DialogInfo : NavigationEventInfo()

    /** The app's back stack as NavDisplay reports it, updated at once instead of recomposing. */
    private class FakeBackStack(initialSize: Int = 1) : NavigationEventHandler<ScreenInfo>(
        initialInfo = ScreenInfo(0),
        isBackEnabled = false,
    ) {
        var size = 1
            private set

        init {
            update(initialSize)
        }

        fun navigate() = update(size + 1)

        fun back() = update(size - 1)

        fun replaceAll() = update(1)

        override fun onBackCompleted() = back()

        private fun update(newSize: Int) {
            size = newSize
            setInfo(
                currentInfo = ScreenInfo(size - 1),
                backInfo = (size - 2 downTo 0).map(::ScreenInfo),
            )
            isBackEnabled = size > 1
        }
    }

    /** A dialog with its own back handling, like Compose dialogs and popups on the web. */
    private class FakeDialog : NavigationEventHandler<NavigationEventInfo>(
        initialInfo = DialogInfo,
        isBackEnabled = true,
    ) {
        var isDismissed = false
            private set

        override fun onBackCompleted() {
            isDismissed = true
            remove()
        }
    }

    /** Browser history that reports moves asynchronously, like real browsers. */
    private class FakeBrowser(
        entries: List<Int?> = listOf(null),
        position: Int = entries.lastIndex,
    ) : BrowserHistory {
        val entries = entries.toMutableList()
        var position = position
            private set

        /** Positions of the entries [replaceState] rewrote, oldest first. */
        val replacedAt = mutableListOf<Int>()

        override val popStates = MutableSharedFlow<PopState>(extraBufferCapacity = 16)

        override val currentIndex: Int?
            get() = entries[position]

        override fun pushState(index: Int) {
            while (entries.lastIndex > position) entries.removeAt(entries.lastIndex)
            entries += index
            position++
        }

        override fun replaceState(index: Int) {
            entries[position] = index
            replacedAt += position
        }

        override fun go(delta: Int) {
            val target = position + delta
            if (delta == 0 || target !in entries.indices) return
            position = target
            popStates.tryEmit(PopState(entries[target], hasUserAgentTransition = false))
        }

        fun userBack(hasUserAgentTransition: Boolean = false) {
            position--
            popStates.tryEmit(PopState(entries[position], hasUserAgentTransition))
        }

        fun userForward() {
            position++
            popStates.tryEmit(PopState(entries[position], hasUserAgentTransition = false))
        }
    }

    private companion object {
        const val FRAME = 16L
    }
}
