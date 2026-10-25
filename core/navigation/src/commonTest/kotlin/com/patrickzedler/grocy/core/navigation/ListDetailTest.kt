@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.patrickzedler.grocy.core.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ListDetailTest {

    @Test
    fun panesStartHalfAndHalf() {
        assertEquals(0.5f, PaneAnchors.first().proportion)
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

    private fun decorate(scene: Scene<NavKey>, isTwoPane: Boolean): Scene<NavKey> =
        with(PaneBackgroundDecorator(isTwoPane)) {
            SceneDecoratorStrategyScope<NavKey>().decorateScene(scene)
        }

    private fun entry(metadata: Map<String, Any>) =
        NavEntry<NavKey>(TestKey, metadata = metadata) {}

    private data object TestKey : NavKey

    private class TestScene(override val entries: List<NavEntry<NavKey>>) : Scene<NavKey> {
        override val key: Any = "scene"
        override val previousEntries: List<NavEntry<NavKey>> = emptyList()
        override val content: @Composable () -> Unit = {}
    }
}
