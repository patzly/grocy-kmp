package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

class ObjectListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)

    // Resource keys instead of texts, the JVM tests have no app language
    private val texts = object : ListTexts {
        override suspend fun string(resource: StringResource, vararg args: Any) =
            (listOf(resource.key) + args.map { it.toString() }).joinToString(" ")

        override suspend fun plural(resource: PluralStringResource, quantity: Int) =
            "${resource.key} $quantity"
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(kind: ObjectListKind, entity: String? = null) =
        ObjectListViewModel(kind, environment.repository, entity, texts, dispatcher).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }

    private val ObjectListViewModel.rows
        get() = requireNotNull(state.value).rows

    @Test
    fun listDownloadsAndShowsTheActiveObjects() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.CHORES)

        assertEquals(listOf(1), viewModel.rows.map { it.id })
        assertEquals("chore_period_weekly", viewModel.rows.single().texts[ObjectColumn.PERIOD_TYPE])
        assertTrue(requireNotNull(viewModel.state.value).hasObjects)
    }

    @Test
    fun showDisabledAddsTheDisabledObjects() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.CHORES)

        viewModel.setFilter { it.copy(showDisabled = true) }
        advanceUntilIdle()

        assertEquals(listOf(2, 1), viewModel.rows.map { it.id })
    }

    @Test
    fun productGroupsCountEveryProductOfTheGroup() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.PRODUCT_GROUPS)
        viewModel.setFilter { it.copy(showDisabled = true) }
        advanceUntilIdle()

        val counts = viewModel.rows.associate { it.id to it.numbers[ObjectColumn.PRODUCT_COUNT] }

        // Milk and the disabled Old cheese
        assertEquals(mapOf(1 to 2.0, 2 to 0.0), counts)
    }

    @Test
    fun userfieldsStartWithTheEntityOfTheirLink() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.USERFIELDS, entity = "chores")

        assertEquals(listOf(2), viewModel.rows.map { it.id })
        assertTrue("userentity-books" in requireNotNull(viewModel.state.value).entities)
    }

    @Test
    fun userfieldsAreSortedByEntityFirstLikeGrocysTable() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.USERFIELDS)

        assertEquals(ObjectSort(ObjectColumn.ENTITY), viewModel.defaultSort)
        assertEquals(listOf("chores", "products"), viewModel.rows.map { it.entity })
    }

    @Test
    fun searchFindsTheTextsOfEveryColumn() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.BATTERIES)
        viewModel.showSearch()

        viewModel.searchState.setTextAndPlaceCursorAtEnd("living")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.rows.map { it.id })
    }

    @Test
    fun clearFilterResetsFiltersAndSearch() = runTest(dispatcher) {
        val viewModel = viewModel(ObjectListKind.USERFIELDS, entity = "chores")
        viewModel.showSearch()
        viewModel.searchState.setTextAndPlaceCursorAtEnd("nothing")

        viewModel.clearFilter()
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(ObjectFilter(), requireNotNull(viewModel.state.value).filter)
        assertEquals(false, viewModel.isSearchVisible)
        assertEquals(2, viewModel.rows.size)
    }
}
