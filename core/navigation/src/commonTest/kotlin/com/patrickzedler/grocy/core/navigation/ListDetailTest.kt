@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.patrickzedler.grocy.core.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ListDetailTest {

    @Test
    fun panesStartHalfAndHalf() {
        assertEquals(0.5f, PaneAnchors.first().proportion)
    }

    @Test
    fun panesKeepTheFocusWhereTheUserActedAndEverythingElseOfTheDirective() {
        val directive = PaneScaffoldDirective(
            maxHorizontalPartitions = 2,
            horizontalPartitionSpacerSize = 24.dp,
            maxVerticalPartitions = 1,
            verticalPartitionSpacerSize = 0.dp,
            defaultPanePreferredWidth = 360.dp,
            defaultPanePreferredHeight = 420.dp,
            excludedBounds = listOf(Rect(100f, 0f, 120f, 800f)),
            shouldAutoFocusCurrentDestination = true,
        )

        val withoutAutoFocus = directive.withoutAutoFocus()

        assertFalse(withoutAutoFocus.shouldAutoFocusCurrentDestination)
        assertEquals(directive.maxHorizontalPartitions, withoutAutoFocus.maxHorizontalPartitions)
        assertEquals(
            directive.horizontalPartitionSpacerSize,
            withoutAutoFocus.horizontalPartitionSpacerSize,
        )
        assertEquals(directive.maxVerticalPartitions, withoutAutoFocus.maxVerticalPartitions)
        assertEquals(
            directive.verticalPartitionSpacerSize,
            withoutAutoFocus.verticalPartitionSpacerSize,
        )
        assertEquals(
            directive.defaultPanePreferredWidth,
            withoutAutoFocus.defaultPanePreferredWidth,
        )
        assertEquals(
            directive.defaultPanePreferredHeight,
            withoutAutoFocus.defaultPanePreferredHeight,
        )
        assertEquals(directive.excludedBounds, withoutAutoFocus.excludedBounds)
    }

    @Test
    fun arrowKeysMoveToTheNeighbouringAnchor() {
        val half = PaneAnchors.first()

        assertEquals(2f / 3, neighbourAnchor(half, towardsEnd = true)?.proportion)
        assertEquals(1f / 3, neighbourAnchor(half, towardsEnd = false)?.proportion)
    }

    @Test
    fun arrowKeysStopAtTheOuterAnchors() {
        assertNull(neighbourAnchor(PaneAnchorsInOrder.last(), towardsEnd = true))
        assertNull(neighbourAnchor(PaneAnchorsInOrder.first(), towardsEnd = false))
    }

    @Test
    fun handleWithoutAnchorGoesToHalfAndHalf() {
        assertEquals(0.5f, neighbourAnchor(null, towardsEnd = true)?.proportion)
    }

    @Test
    fun listDetailScenesOfTwoPanesGetThePaneBackground() {
        val scene = TestScene(listOf(entry(listPaneMetadata("list") {})))

        val decorated = decorate(scene, isTwoPane = true)

        assertIs<PaneBackgroundScene>(decorated)
        assertSame(scene, decorated.scene)
        assertEquals(scene.key, decorated.key)
    }

    @Test
    fun onePaneKeepsTheScreensBackground() {
        val scene = TestScene(listOf(entry(listPaneMetadata("list") {})))

        assertSame(scene, decorate(scene, isTwoPane = false))
    }

    @Test
    fun otherScenesKeepTheScreensBackground() {
        val scene = TestScene(listOf(entry(emptyMap())))

        assertSame(scene, decorate(scene, isTwoPane = true))
    }

    @Test
    fun decoratedScenesAreEqualForTheSameScene() {
        val scene = TestScene(listOf(entry(listPaneMetadata("list") {})))

        assertEquals(decorate(scene, isTwoPane = true), decorate(scene, isTwoPane = true))
    }

    @Test
    fun backFromADetailOpenedFromAnotherReturnsToIt() {
        val strategy = NestedDetailSceneStrategy(RecordingStrategy)
        val list = entry(listPaneMetadata("settings") {})
        val detail = entry(detailPaneMetadata("settings"))
        val nested = entry(detailPaneMetadata("settings"))

        val scene = with(strategy) {
            SceneStrategyScope<NavKey>().calculateScene(listOf(list, detail, nested))
        }

        assertIs<BackToScene>(scene)
        assertEquals(listOf(list, detail), scene.previousEntries)
    }

    @Test
    fun backFromADetailOpenedWithItsListLeavesTheScene() {
        // The stock settings opened from the products, with the categories and user settings
        val strategy = NestedDetailSceneStrategy(LastEntriesStrategy(count = 3))
        val products = entry(listPaneMetadata("products") {})
        val categories = entry(listPaneMetadata("settings") {})
        val userSettings = entry(detailPaneMetadata("settings"))
        val stockSettings = entry(detailPaneMetadata("settings", backLeavesScene = true))

        val scene = with(strategy) {
            SceneStrategyScope<NavKey>().calculateScene(
                listOf(products, categories, userSettings, stockSettings),
            )
        }

        assertIs<BackToScene>(scene)
        assertEquals(listOf(products), scene.previousEntries)
    }

    @Test
    fun aSingleDetailKeepsMaterialsBack() {
        val strategy = NestedDetailSceneStrategy(RecordingStrategy)
        val entries = listOf(
            entry(listPaneMetadata("list") {}),
            entry(detailPaneMetadata("list")),
        )

        val scene = with(strategy) { SceneStrategyScope<NavKey>().calculateScene(entries) }

        assertIs<TestScene>(scene)
    }

    @Test
    fun aListOpenedFromADetailKeepsMaterialsBack() {
        // The products opened from the master data beside the start screen
        val strategy = NestedDetailSceneStrategy(RecordingStrategy)
        val entries = listOf(
            entry(listPaneMetadata("start") {}),
            entry(detailPaneMetadata("start")),
            entry(listPaneMetadata("products") {}),
        )

        val scene = with(strategy) { SceneStrategyScope<NavKey>().calculateScene(entries) }

        assertIs<TestScene>(scene)
    }

    @Test
    fun scenesOfOnePaneAreLeftToTheNextStrategy() {
        val strategy = NestedDetailSceneStrategy(NoSceneStrategy)
        val entries = listOf(
            entry(detailPaneMetadata("settings")),
            entry(detailPaneMetadata("settings")),
        )

        assertNull(with(strategy) { SceneStrategyScope<NavKey>().calculateScene(entries) })
    }

    @Test
    fun scenesBackToADetailAreEqualForTheSameScene() {
        val scene = TestScene(listOf(entry(detailPaneMetadata("settings"))))

        assertEquals(BackToScene(scene, emptyList()), BackToScene(scene, emptyList()))
    }

    private fun decorate(scene: Scene<NavKey>, isTwoPane: Boolean): Scene<NavKey> =
        with(PaneBackgroundDecorator(isTwoPane)) {
            SceneDecoratorStrategyScope<NavKey>().decorateScene(scene)
        }

    private fun entry(metadata: Map<String, Any>) =
        NavEntry<NavKey>(TestKey, metadata = metadata) {}

    private data object TestKey : NavKey

    // Lays out no scene, like Material's strategy for a single pane
    private object NoSceneStrategy : SceneStrategy<NavKey> {
        override fun SceneStrategyScope<NavKey>.calculateScene(
            entries: List<NavEntry<NavKey>>,
        ): Scene<NavKey>? = null
    }

    // Lays out every entry it gets as one scene
    private object RecordingStrategy : SceneStrategy<NavKey> {
        override fun SceneStrategyScope<NavKey>.calculateScene(
            entries: List<NavEntry<NavKey>>,
        ): Scene<NavKey> = TestScene(entries)
    }

    // Lays out the last entries as one scene, like Material's scene of a list and its details
    private class LastEntriesStrategy(private val count: Int) : SceneStrategy<NavKey> {
        override fun SceneStrategyScope<NavKey>.calculateScene(
            entries: List<NavEntry<NavKey>>,
        ): Scene<NavKey> = TestScene(entries.takeLast(count))
    }

    private class TestScene(override val entries: List<NavEntry<NavKey>>) : Scene<NavKey> {
        override val key: Any = "scene"
        override val previousEntries: List<NavEntry<NavKey>> = emptyList()
        override val content: @Composable () -> Unit = {}
    }
}
