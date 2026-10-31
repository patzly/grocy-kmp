package com.patrickzedler.grocy.core.forms

import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ObjectActionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(entity: String): ObjectActionsViewModel {
        environment.repository.refresh()
        advanceUntilIdle()
        return ObjectActionsViewModel(entity, environment.repository)
    }

    @Test
    fun deleteAsksWithTheObjectsName() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.LOCATIONS)

        viewModel.request(ObjectAction.DELETE, 2)
        advanceUntilIdle()

        assertEquals(ObjectActionDialog.Delete(2, "Pantry"), viewModel.dialog)
    }

    @Test
    fun confirmedDeleteRemovesTheObject() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.LOCATIONS)

        viewModel.events.test {
            viewModel.delete(2)
            advanceUntilIdle()

            assertEquals(ObjectActionEvent.Deleted(2), awaitItem())
        }
        assertNull(viewModel.dialog)
        val names = environment.repository.locations.first().map { it.name }
        assertEquals(listOf("Fridge"), names)
    }

    @Test
    fun deleteOfflineKeepsTheObject() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.LOCATIONS)
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.delete(2)
            advanceUntilIdle()

            assertEquals(ObjectActionEvent.Offline, awaitItem())
        }
        assertEquals(2, environment.repository.locations.first().size)
    }

    @Test
    fun rejectedDeleteCarriesGrocysMessage() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.QUANTITY_UNITS)
        environment.server.respond(
            "/api/objects/quantity_units/1",
            HttpStatusCode.BadRequest,
            """{"error_message":"FOREIGN KEY constraint failed"}""",
            HttpMethod.Delete,
        )

        viewModel.events.test {
            viewModel.delete(1)
            advanceUntilIdle()

            assertEquals(
                ObjectActionEvent.Failed(ObjectAction.DELETE, "FOREIGN KEY constraint failed"),
                awaitItem(),
            )
        }
    }

    @Test
    fun copyReportsTheCopysId() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.PRODUCTS)
        environment.server.respond(
            "/api/stock/products/1/copy",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        environment.server.respond(
            "/api/objects/products/${MasterDataJson.CREATED_ID}",
            body = MasterDataJson.CREATED_PRODUCT,
        )

        viewModel.events.test {
            viewModel.request(ObjectAction.COPY, 1)
            advanceUntilIdle()

            assertEquals(ObjectActionEvent.Copied(MasterDataJson.CREATED_ID), awaitItem())
        }
    }

    @Test
    fun mergeStartsWithTheObjectToKeep() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.CHORES)

        viewModel.request(ObjectAction.MERGE, 1)

        assertEquals(ObjectActionDialog.Merge(keepId = 1), viewModel.dialog)
        val names = viewModel.mergeCandidates.first().map { it.name }
        assertEquals(listOf("Old chore", "Vacuum"), names)
    }

    @Test
    fun mergeOfProductsPostsBothIds() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.PRODUCTS)
        environment.server.respond(
            "/api/stock/products/1/merge/4",
            HttpStatusCode.NoContent,
            method = HttpMethod.Post,
        )

        viewModel.events.test {
            viewModel.merge(keepId = 1, removeId = 4)
            advanceUntilIdle()

            assertEquals(ObjectActionEvent.Merged(keepId = 1, removeId = 4), awaitItem())
        }
        val merges = environment.server.requestsTo("/api/stock/products/1/merge/4", HttpMethod.Post)
        assertEquals(1, merges.size)
    }

    @Test
    fun mergeIntoItselfSendsNothing() = runTest(dispatcher) {
        val viewModel = viewModel(MasterDataRepository.PRODUCTS)

        viewModel.merge(keepId = 1, removeId = 1)
        advanceUntilIdle()

        val path = "/api/stock/products/1/merge/1"
        assertEquals(0, environment.server.requestsTo(path, HttpMethod.Post).size)
    }
}
